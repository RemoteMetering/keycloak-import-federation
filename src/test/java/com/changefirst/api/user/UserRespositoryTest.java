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
package com.changefirst.api.user;

import com.changefirst.model.UserDto;
import com.xebialabs.restito.server.StubServer;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.resourceContent;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Condition.*;
import static java.lang.String.format;

/**
 * Remote user federation provider factory tests.
 */
public class UserRespositoryTest {

    private static final String USER_NAME = "user1@changefirst.com";
    private static final String CONTEXT = "/api/migration";
    private static final String CLIENT = "134";

    private StubServer server;

    @Before
    public void setUp() throws Exception {
        server = new StubServer().run();
    }

    @After
    public void stop() {
        server.stop();
    }

    /*
    @Test
    public void testRemoteValidateCredentials() {
        UserRepository userRepository = new UserRepository("http://keycloak.getsandbox.com");
        boolean exists = userRepository.validateCredentials(USER_NAME, "password");
    }
    */

    @Test
    public void testFindUserByUsername() {
        String url = getClientUrl(CONTEXT, CLIENT, USER_NAME);

        whenHttp(server).
                match(get(url)).
                then(status(HttpStatus.OK_200), contentType("application/json"), resourceContent("com/changefirst/api/user.json"));

        UserRepository userRepository = new UserRepository(getRestUrl(), CLIENT);
        UserDto user = userRepository.findUserByUsername(USER_NAME);

        verifyHttp(server).once(
                method(Method.GET),
                uri(url)
        );

        Assert.assertEquals(USER_NAME, user.getEmail());
        Assert.assertEquals("First Name", user.getFirstName());
        Assert.assertEquals("Last Name", user.getLastName());
        Assert.assertEquals("Test Username", user.getUsername());
        Assert.assertTrue("is enabled", user.isEnabled());
        Assert.assertTrue("has admin role", user.getRoles().contains("admin"));
        Assert.assertTrue("has user role", user.getRoles().contains("user"));

        Map<String, List<String>> attribs = user.getAttributes();

        Assert.assertTrue("Contains locale", attribs.containsKey("locale"));
        Assert.assertTrue("Contains country", attribs.containsKey("country"));
        Assert.assertTrue("Contains client id", attribs.containsKey("clientId"));
        Assert.assertTrue("Contains phone Nr", attribs.containsKey("phoneNumber"));
        Assert.assertTrue("Contains mobile Number ", attribs.containsKey("mobileNumber"));

        String clientId = attribs.get("clientId").get(0);
        Assert.assertEquals(CLIENT, clientId);

    }

    @Test
    public void testFindUserByUsernameNoEmail() {
        String url = getClientUrl(CONTEXT, CLIENT, USER_NAME);

        whenHttp(server).match(get(url)).then(status(HttpStatus.OK_200), contentType("application/json"),
                resourceContent("com/changefirst/api/userNoEmail.json"));

        UserRepository userRepository = new UserRepository(getRestUrl(), CLIENT);
        UserDto user = userRepository.findUserByUsername(USER_NAME);

        verifyHttp(server).once(method(Method.GET), uri(url));

        Assert.assertEquals(null, user.getEmail());
        Assert.assertEquals("First Name", user.getFirstName());
        Assert.assertEquals("Last Name", user.getLastName());
        Assert.assertEquals("Test Username", user.getUsername());
        Assert.assertTrue("is enabled", user.isEnabled());
        Assert.assertTrue("has admin role", user.getRoles().contains("admin"));
        Assert.assertTrue("has user role", user.getRoles().contains("user"));

        Map<String, List<String>> attribs = user.getAttributes();

        Assert.assertTrue("Contains locale", attribs.containsKey("locale"));
        Assert.assertTrue("Contains country", attribs.containsKey("country"));
        Assert.assertTrue("Contains client id", attribs.containsKey("clientId"));
        Assert.assertTrue("Contains phone Nr", attribs.containsKey("phoneNumber"));
        Assert.assertTrue("Contains mobile Number ", attribs.containsKey("mobileNumber"));

        String clientId = attribs.get("clientId").get(0);
        Assert.assertEquals(CLIENT, clientId);

    }

    @Test
    public void testValidateCredentials() throws Exception {

        String url = getClientUrl(CONTEXT, CLIENT, USER_NAME);
        String credential = "password";

        whenHttp(server).
                match(post(url)).
                then(status(HttpStatus.OK_200), contentType("application/json"));

        UserRepository userRepository = new UserRepository(getRestUrl(), CLIENT);
        boolean exists = userRepository.validateCredentials(USER_NAME, credential);

        Assert.assertTrue(exists);

        verifyHttp(server).once(
                method(Method.POST),
                uri(url),
                withPostBodyContainingJsonPath("password", credential)
        );

    }

    @Test
    public void testValidateCredentialsInvalid() throws Exception {

        String url = getClientUrl(CONTEXT, CLIENT, USER_NAME);

        whenHttp(server).
                match(post(url)).
                then(status(HttpStatus.NOT_FOUND_404), contentType("application/json"));

        UserRepository userRepository = new UserRepository(getRestUrl(), CLIENT);
        boolean exists = userRepository.validateCredentials(USER_NAME, "password");

        Assert.assertFalse(exists);

        verifyHttp(server).once(
                method(Method.POST),
                uri(url)
        );

    }

    private String getClientUrl(String context, String client, String userName) {
        return format("%s/clients/%s/users/%s", context, client, userName);
    }

    private String getRestUrl() {
        return format("http://localhost:%d%s", server.getPort(), CONTEXT);
    }
}