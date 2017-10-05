/*
 * Copyright 2015 Smartling, Inc.
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
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

/**
 * Remote user federation provider factory.
 *
 * @author Scott Rossillo
 */
public class RestUserFederationProviderFactory implements UserStorageProviderFactory<RestUserFederationProvider> {


    public static final String PROVIDER_NAME = "Rest User Migration Federation SPI";

    public static final String PROPERTY_URL = "url";
    public static final String PROPERTY_ATTRIBS = "attributes";
    public static final String ROLE_PREFIX = "role-prefix";
    public static final String PROPERTY_CLIENTID = "clientId";
    public static final String ROLE_CLIENTID = "roleClientId";
    public static final String AUTO_ENABLE_ACCOUNT = "auto-enable";
    public static final String AUTO_CONVERT_LOCALE = "auto-locale";
    public static final String UPPERCASE_ROLE = "uppercase-role";
    public static final String LOWERCASE_ROLE = "lowercase-role";

    private static final Logger LOG = Logger.getLogger(RestUserFederationProviderFactory.class);

    protected static final List<ProviderConfigProperty> configMetadata;

    static {
        configMetadata = ProviderConfigurationBuilder.create()
                .property().name(PROPERTY_URL)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Remote User Information Url")
                .defaultValue("https://")
                .helpText("Remote repository url")
                .add()
                .property().name(PROPERTY_CLIENTID)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Remote Client identifier")
                .defaultValue("")
                .helpText("This value will be sent to the remote service along with the username")
                .add()
                .property().name(ROLE_CLIENTID)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Role Client ID")
                .defaultValue("")
                .helpText("This will add the roles to the provided client")
                .add()
                .property().name(PROPERTY_ATTRIBS)
                .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
                .label("Names of attributes to process in addition to the default properties")
                .defaultValue("")
                .helpText("Using this property, one can pass in attributes onto the user")
                .add()
                .property().name(ROLE_PREFIX)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Prefix that needs to be removed from roles received from remote API")
                .defaultValue("")
                .helpText("it is possible to remove a string prefix from roles")
                .add()
                .property().name(AUTO_ENABLE_ACCOUNT)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Auto enable migrated user accounts")
                .defaultValue(true)
                .helpText("Using this property, one can mark all migrated accounts enabled")
                .add()
                .property().name(AUTO_CONVERT_LOCALE)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Auto convert user's locale")
                .defaultValue(true)
                .helpText("Using this property, one can auto convert a locale. e.g. if user locale is en-GB and only en is enabled it will try to find and match that one")
                .add()
                .property().name(UPPERCASE_ROLE)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Uppercase role name")
                .defaultValue(true)
                .helpText("Convert remote roles to all uppercase")
                .add()
                .property().name(LOWERCASE_ROLE)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Lowercase role name")
                .defaultValue(true)
                .helpText("Convert remote roles to all lowercase")
                .add()
                .build();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
       String url = config.getConfig().getFirst(PROPERTY_URL);
       String clientId = config.getConfig().getFirst(PROPERTY_CLIENTID);
       String roleClientId = config.getConfig().getFirst(ROLE_CLIENTID);

       boolean valid = false;

       if ( url != null && url.length() > 10) {
           valid =  true;
       }


       if ( valid && clientId != null && clientId.length() > 0 ) {
           valid = true;
       } else {
           valid = false;
       }

       if(valid && roleClientId != null && roleClientId.length() > 0 ){
           valid = true;
       } else {
           valid = false;
       }

       LOG.debugf("validating module config %s", valid);

       if (!valid) {
           throw new ComponentValidationException("Invalid configuration. Please check the url");
       }
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public String getHelpText() {
        return "This is a read-only user federation provider that can be used to sync user data one way from a remote REST API";
    }

    @Override
    public RestUserFederationProvider create(KeycloakSession session, ComponentModel model) {
        String url = model.getConfig().getFirst(PROPERTY_URL);
        String clientId = model.getConfig().getFirst(PROPERTY_CLIENTID);
        String roleClientId = model.getConfig().getFirst(ROLE_CLIENTID);
        Boolean autoEnable = Boolean.valueOf(model.getConfig().getFirst(AUTO_ENABLE_ACCOUNT));
        Boolean autoConvertLocale = Boolean.valueOf(model.getConfig().getFirst(AUTO_CONVERT_LOCALE));
        Boolean upperCase = Boolean.valueOf(model.getConfig().getFirst(UPPERCASE_ROLE));
        Boolean lowerCase = Boolean.valueOf(model.getConfig().getFirst(LOWERCASE_ROLE));
        List<String> attribList = model.getConfig().getList(PROPERTY_ATTRIBS);
        String rolePrefix = model.getConfig().getFirst(ROLE_PREFIX);
        UserRepository repository = new UserRepository(url, clientId);
        return new RestUserFederationProvider(session, model, repository, attribList, rolePrefix, autoEnable,autoConvertLocale, upperCase, lowerCase, roleClientId);
    }
}
