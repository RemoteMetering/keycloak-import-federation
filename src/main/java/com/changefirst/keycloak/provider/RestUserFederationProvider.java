/*
 * Copyright 2015 Changefirst Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.changefirst.keycloak.provider;

import com.changefirst.api.user.UserRepository;
import com.changefirst.model.UserDto;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rest User federation to import users from remote user store
 *
 * @author Istvan Orban
 */
public class RestUserFederationProvider
        implements UserLookupProvider, ImportedUserValidation, CredentialInputValidator, UserStorageProvider {

    private static final Logger LOG = Logger.getLogger(RestUserFederationProvider.class);

    protected KeycloakSession session;
    protected ComponentModel model;
    protected UserRepository repository;
    protected List<String> attributes;
    protected Boolean autoEnable;
    protected Boolean autoConvertLocale;
    protected Boolean upperCaseRoleName;
    protected Boolean lowerCaseRoleName;
    protected String rolePrefix;
    protected String roleClientId;

    public RestUserFederationProvider(KeycloakSession session, ComponentModel model, UserRepository repository) {
        this(session, model, repository, new ArrayList<String>(), null, false, false, false, false, "");
    }

    public RestUserFederationProvider(KeycloakSession session, ComponentModel model, UserRepository repository,
            List<String> attributes, String rolePrefix, Boolean autoEnable, Boolean autoConvertLocale,
            Boolean upperCaseRoleName, Boolean lowerCaseRoleName, String roleClientId) {
        this.session = session;
        this.model = model;
        this.repository = repository;
        this.attributes = attributes;
        this.rolePrefix = rolePrefix;
        this.autoEnable = autoEnable;
        this.autoConvertLocale = autoConvertLocale;
        this.upperCaseRoleName = upperCaseRoleName;
        this.lowerCaseRoleName = lowerCaseRoleName;
        this.roleClientId = roleClientId;
    }

    protected UserModel createAdapter(RealmModel realm, String username) {
        UserModel local = session.userLocalStorage().getUserByUsername(username, realm);
        // if not found try with the extrnal user nam

        if (local == null) {
            // fetch user remotely
            LOG.debugf("User %s does not exists locally, fetching it from remote.", username);
            UserDto remote = this.repository.findUserByUsername(username);
            if (remote != null) {

                // if (!username.equals(remote.getEmail())) {
                // throw new IllegalStateException(String.format("Local and remote users are not
                // the same : [%s != %s]", username, remote.getEmail()));
                // }

                if (remote.getUsername() == null) {
                    throw new IllegalStateException(
                            String.format("Remote user has no username for login : [%s]", username));
                }

                // check if we can find the user locally with the remote use name
                local = session.userLocalStorage().getUserByUsername(remote.getUsername(), realm);

                //if still no local add the user and roles
                if (local == null) {
                    // create user locally and set up relationship to this SPI
                    local = session.userLocalStorage().addUser(realm, remote.getUsername());
                    local.setFederationLink(model.getId());

                    // merge data from remote to local
                    local.setFirstName(remote.getFirstName());
                    local.setLastName(remote.getLastName());

                    if (remote.getEmail() != null && remote.getEmail().contains("@")) {
                        local.setEmail(remote.getEmail());
                        local.setEmailVerified(remote.isEnabled());
                    }

                    // auto enable account
                    if (this.autoEnable != null && this.autoEnable) {
                        local.setEnabled(true);
                    }

                    // copy user attribs over
                    if (remote.getAttributes() != null) {
                        List<String> attributeValues = null;
                        Map<String, List<String>> attributes = remote.getAttributes();
                        for (String attributeName : attributes.keySet()) {
                            if (this.attributes.isEmpty() || this.attributes.contains(attributeName)) {
                                attributeValues = attributes.get(attributeName);
                                if (attributeValues != null && !attributeValues.isEmpty()) {
                                    if (attributeName.equalsIgnoreCase(UserModel.LOCALE)) {
                                        this.setUserLocale(realm, local, attributeValues.get(0));
                                    } else if (attributeValues.size() == 1) {
                                        local.setSingleAttribute(attributeName, attributeValues.get(0));
                                    } else {
                                        local.setAttribute(attributeName, attributeValues);
                                    }
                                }
                            }
                        }
                    }

                    ClientModel clientModel = realm.getClientByClientId(this.roleClientId);

                    // pass roles along
                    if (remote.getRoles() != null) {
                        for (String role : remote.getRoles()) {
                            RoleModel roleModel = clientModel.getRole(convertRemoteRoleName(role));
                            if (roleModel != null) {
                                local.grantRole(roleModel);
                                LOG.infof("Remote role %s granted to %s", role, username);
                            }
                        }
                    }
                }
            }
        }
        if (local != null) {
            return new UserModelDelegate(local) {
                @Override
                public void setUsername(String username) {
                    // showing we can proxy every request to our repo if needed
                    super.setUsername(username);
                }
            };
        } else {
            return null;
        }
    }

    private String convertRemoteRoleName(String remoteRoleName) {
        String roleName = remoteRoleName;
        if (this.rolePrefix != null && this.rolePrefix.length() > 0) {
            roleName = remoteRoleName.replaceFirst("^" + rolePrefix, "");
        }
        if (this.upperCaseRoleName) {
            roleName = roleName.toUpperCase();
        } else if (this.lowerCaseRoleName) {
            roleName = roleName.toLowerCase();
        }
        return roleName;
    }

    private UserModel setUserLocale(RealmModel realm, UserModel local, String locale) {

        String matchingLocale = null;
        if (this.autoConvertLocale && realm.isInternationalizationEnabled()) {
            Set<String> supportedLocales = realm.getSupportedLocales();
            for (String supLocale : supportedLocales) {
                if (locale.equalsIgnoreCase(supLocale)) {
                    matchingLocale = supLocale;
                    break;
                }
            }
            if (matchingLocale == null) {
                for (String supLocale : supportedLocales) {
                    if (locale.toLowerCase().contains(supLocale.toLowerCase())) {
                        matchingLocale = supLocale;
                        break;
                    }
                }
            }
        }

        matchingLocale = matchingLocale == null ? locale : matchingLocale;
        local.setSingleAttribute(UserModel.LOCALE, matchingLocale);
        return local;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return CredentialModel.PASSWORD.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }
        UserCredentialModel cred = (UserCredentialModel) input;
        return repository.validateCredentials(user.getUsername(), cred.getValue());
    }

    @Override
    public void close() {
        // n/a
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();
        return this.getUserByUsername(username, realm);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return getUserByUsername(email, realm);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        LOG.infof("Creating a new user adapter for: %s", username);
        return this.createAdapter(realm, username);
    }

    @Override
    public void preRemove(RealmModel realm) {
        // n/a
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        // n/a
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        // n/a
    }

    // see
    // https://keycloak.gitbooks.io/server-developer-guide/content/topics/user-storage/import.html
    // this is called every time the user is laoded from the Local user-store
    // this can be used to check if the user still exists in the remote, returning
    // null will remote this user locally
    @Override
    public UserModel validate(RealmModel realm, UserModel user) {
        return user;
    }
}
