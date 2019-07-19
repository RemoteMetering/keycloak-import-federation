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

    private String lastCreatedUserName = "user@changefirst.com";
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
                        lastChangedEmail =  emailString.getArguments()[0].toString();
                        return null;
                    }).when(newUser).setEmail(anyString());
                    return newUser;
                });

        when(keycloakSession.userLocalStorage()).thenReturn(userProvider);

        when(input.getValue()).thenReturn("password");
        when(input.getType()).thenReturn(CredentialModel.PASSWORD);
        when(repository.findUserByUsername("user@changefirst.com")).thenReturn(testRemoteUser);

    }

    @Test
    public void testGetInstance() throws Exception {

        Object provider = factory.create(keycloakSession, userFederationProviderModel);

        assertNotNull(provider);
        assertTrue(provider instanceof RestUserFederationProvider);
    }

    @Test
    public void loginUserNoEmail() throws Exception {
     
        testRemoteUser.setUsername("testUsername");

        RestUserFederationProvider restProvider = new RestUserFederationProvider(keycloakSession,
                userFederationProviderModel, repository);

        UserModel user = restProvider.getUserByUsername("user@changefirst.com", realm);
        assertNotNull(user);
        assertEquals("testUsername", user.getUsername());
        assertEquals("testUsername", lastCreatedUserName);
        assertNull(user.getEmail());
    }

    @Test
    public void loginUserWithEmail() throws Exception {
      
        testRemoteUser.setUsername("testUsername");
        testRemoteUser.setEmail("bob@123.com");

        RestUserFederationProvider restProvider = new RestUserFederationProvider(keycloakSession,
                userFederationProviderModel, repository);

        UserModel user = restProvider.getUserByUsername("user@changefirst.com", realm);
        assertNotNull(user);
        assertEquals("testUsername", user.getUsername());
        assertEquals("testUsername", lastCreatedUserName);

        assertEquals("bob@123.com", lastChangedEmail);
    }

    @Test
    public void loginUserWithInvalidEmail() throws Exception {

        testRemoteUser.setUsername("testUsername");
        testRemoteUser.setEmail("MonkeyBallz");
        lastChangedEmail = null;

        RestUserFederationProvider restProvider = new RestUserFederationProvider(keycloakSession,
                userFederationProviderModel, repository);

        UserModel user = restProvider.getUserByUsername("user@changefirst.com", realm);
        assertNotNull(user);
        assertEquals("testUsername", user.getUsername());
        assertEquals("testUsername", lastCreatedUserName);

        assertNull(lastChangedEmail);
    }

    @Test
    public void testclose() throws Exception {
        RestUserFederationProvider provider = factory.create(keycloakSession, userFederationProviderModel);
        provider.close();
        verifyZeroInteractions(config);
    }
}