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

import org.junit.Before;
import org.junit.Test;
import org.keycloak.Config.Scope;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Remote user federation provider factory tests.
 */
public class RestUserFederationProviderFactoryTest {

    private RestUserFederationProviderFactory factory;

    @Mock
    private KeycloakSessionFactory keycloakSessionFactory;

    @Mock
    private KeycloakSession keycloakSession;

    @Mock
    private RealmModel realm;

    @Mock
    private Scope config;

    @Mock
    private ComponentModel userFederationProviderModel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        factory = new RestUserFederationProviderFactory();
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<String, String>();
        config.putSingle(RestUserFederationProviderFactory.PROPERTY_URL, "https://www.example.org");
        config.putSingle(RestUserFederationProviderFactory.PROPERTY_CLIENTID, "134");
        config.putSingle(RestUserFederationProviderFactory.ROLE_CLIENTID, "testClient");
        when(userFederationProviderModel.getConfig())
                .thenReturn(config);
    }

    @Test
    public void testGetInstance() throws Exception {
        Object provider = factory.create(keycloakSession, userFederationProviderModel);
        assertNotNull(provider);
        assertTrue(provider instanceof RestUserFederationProvider);
    }

    @Test
    public void testGetId() throws Exception {
        assertEquals(RestUserFederationProviderFactory.PROVIDER_NAME, factory.getId());
    }

    @Test
    public void testCreate() throws Exception {
        assertNull(factory.create(keycloakSession));
    }

    @Test
    public void testValidateConfiguration() throws Exception {
        factory.init(config);
        factory.validateConfiguration(keycloakSession, realm, userFederationProviderModel);
        verifyZeroInteractions(config);
    }

    @Test
    public void testInit() throws Exception {
        factory.init(config);
        verifyZeroInteractions(config);
    }

    @Test
    public void testPostInit() throws Exception {
        factory.postInit(keycloakSessionFactory);
        verifyZeroInteractions(keycloakSession, keycloakSessionFactory);
    }

    @Test
    public void testClose() throws Exception {
        factory.close();
        verifyZeroInteractions(keycloakSession, keycloakSessionFactory);
    }
}