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

import com.changefirst.model.UserDto;

import com.changefirst.api.user.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.Config.Scope;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Remote user federation provider factory tests.
 */
public class RestUserFederationProviderTest {

    RestUserFederationProviderFactory factory;

    @Mock
    private KeycloakSessionFactory keycloakSessionFactory;

    @Mock
    private KeycloakSession keycloakSession;

    @Mock
    private Scope config;

    @Mock
    private ComponentModel userFederationProviderModel;

    @Mock
    private RealmModel realm;

    @Mock
    private UserModel user;

    @Mock
    private UserCredentialModel input;

    @Mock
    private UserRepository repository;

    @Mock
    private UserProvider userProvider;

    // @Mock
    // private UserFederationManager userFederationManager;

    private UserDto testRemoteUser = new UserDto();

    @Mock
    private UserModel newUser;

    @Mock
    private UserModel existingUser;

    private String lastCreatedUserName = null;
    private String lastChangedEmail = null;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        factory = new RestUserFederationProviderFactory();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<String, String>();
        config.putSingle(RestUserFederationProviderFactory.PROPERTY_URL, "http://localhost.com");
        when(userFederationProviderModel.getConfig()).thenReturn(config);
        when(userFederationProviderModel.getId()).thenReturn("testRms");

        when(user.getUsername()).thenReturn("user@changefirst.com");

        when(userProvider.addUser(any(), any()))

                .thenAnswer(i -> {
                    lastCreatedUserName = i.getArguments()[1].toString();
                    when(newUser.getUsername()).thenReturn(lastCreatedUserName);

                    doAnswer((emailString) -> {
                        lastChangedEmail = emailString.getArguments()[0].toString();
                        return null;
                    }).when(newUser).setEmail(anyString());
                    return newUser;
                });

        when(keycloakSession.userLocalStorage()).thenReturn(userProvider);

        when(input.getValue()).thenReturn("password");
        when(input.getType()).thenReturn(CredentialModel.PASSWORD);
        when(repository.findUserByUsername(anyString())).thenReturn(testRemoteUser);

    }

    @Test
    public void testGetInstance() throws Exception {

        Object provider = factory.create(keycloakSession, userFederationProviderModel);

        assertNotNull(provider);
        assertTrue(provider instanceof RestUserFederationProvider);
    }

    @Test
    public void loginNewUserNoEmail() throws Exception {

        testRemoteUser.setUsername("testUsername");

        RestUserFederationProvider restProvider = new RestUserFederationProvider(keycloakSession,
                userFederationProviderModel, repository);

        UserModel loadedUser = restProvider.getUserByUsername("user@changefirst.com", realm);
        assertNotNull(loadedUser);
        assertEquals("testUsername", loadedUser.getUsername());
        assertEquals("testUsername", lastCreatedUserName);
        assertNull(loadedUser.getEmail());
        assertNull(lastChangedEmail);
    }

    @Test
    public void loginNewUserWithEmail() throws Exception {

        lastChangedEmail = null;
        lastCreatedUserName = null;

        testRemoteUser.setUsername("testUsername");
        testRemoteUser.setEmail("bob@123.com");

        RestUserFederationProvider restProvider = new RestUserFederationProvider(keycloakSession,
                userFederationProviderModel, repository);

        UserModel loadedUser = restProvider.getUserByUsername("bob@123.com", realm);
        assertNotNull(loadedUser);
        assertEquals("testUsername", loadedUser.getUsername());
        assertEquals("testUsername", lastCreatedUserName);

        assertEquals("bob@123.com", lastChangedEmail);
    }

    @Test
    public void loginNewUserWithEmailSameAsUsername() throws Exception {

        lastChangedEmail = null;
        lastCreatedUserName = null;

        testRemoteUser.setUsername("bob@123.com");
        testRemoteUser.setEmail("bob@123.com");

        RestUserFederationProvider restProvider = new RestUserFederationProvider(keycloakSession,
                userFederationProviderModel, repository);

        UserModel loadedUser = restProvider.getUserByUsername("bob@123.com", realm);
        assertNotNull(loadedUser);

        assertEquals("bob@123.com", lastCreatedUserName);

        assertEquals("bob@123.com", lastChangedEmail);
    }

    @Test
    public void loginNewUserWithInvalidEmail() throws Exception {

        lastChangedEmail = null;
        lastCreatedUserName = null;

        testRemoteUser.setUsername("testUsername");
        testRemoteUser.setEmail("MonkeyBallz");
        lastChangedEmail = null;

        RestUserFederationProvider restProvider = new RestUserFederationProvider(keycloakSession,
                userFederationProviderModel, repository);

        UserModel loadedUser = restProvider.getUserByUsername("MonkeyBallz", realm);
        assertNotNull(loadedUser);
        assertEquals("testUsername", loadedUser.getUsername());
        assertEquals("testUsername", lastCreatedUserName);

        assertNull(lastChangedEmail);
    }

    @Test
    public void loginExitingUserWithEmail() throws Exception {

        lastChangedEmail = null;
        lastCreatedUserName = null;

        testRemoteUser.setUsername("testUsername");
        testRemoteUser.setEmail("bob@123.com");

        when(existingUser.getUsername()).thenReturn("testUsername");

        when(userProvider.getUserByUsername(eq("testUsername"), eq(realm))).thenReturn(existingUser);

        RestUserFederationProvider restProvider = new RestUserFederationProvider(keycloakSession,
                userFederationProviderModel, repository);

        UserModel loadedUser = restProvider.getUserByUsername("user@changefirst.com", realm);
        assertNotNull(loadedUser);
        assertEquals("testUsername", loadedUser.getUsername());
        assertNull(lastCreatedUserName);

        assertNull(lastChangedEmail);
    }

    @Test
    public void loginExitingUserWithEmailTheSameAsUsename() throws Exception {

        lastChangedEmail = null;
        lastCreatedUserName = null;

        // remote call should not be reuired so set as monkeyballz
        testRemoteUser.setUsername("monkyBallz");
        testRemoteUser.setEmail("monkyBallz@123.com");

        when(existingUser.getUsername()).thenReturn("bob@123.com");

        when(userProvider.getUserByUsername(eq("bob@123.com"), eq(realm))).thenReturn(existingUser);

        RestUserFederationProvider restProvider = new RestUserFederationProvider(keycloakSession,
                userFederationProviderModel, repository);

        UserModel loadedUser = restProvider.getUserByUsername("bob@123.com", realm);
        assertNotNull(loadedUser);
        assertNull(lastCreatedUserName);

        assertNull(lastChangedEmail);
    }

    @Test
    public void testclose() throws Exception {
        RestUserFederationProvider provider = factory.create(keycloakSession, userFederationProviderModel);
        provider.close();
        verifyZeroInteractions(config);
    }
}