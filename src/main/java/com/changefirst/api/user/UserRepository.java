package com.changefirst.api.user;

import com.changefirst.model.UserCredentialsDto;
import com.changefirst.model.UserDto;
import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Remote repository to load remote user data from UserService using REST
 * Created by istvano on 16/02/2017.
 */
public class UserRepository {

    private static final Logger LOG = Logger.getLogger(UserRepository.class);

    private static UserService buildClient(String uri) {
        ResteasyClient client = new ResteasyClientBuilderImpl().disableTrustManager().build();
        ResteasyWebTarget target = client.target(uri);

        return target
                .proxyBuilder(UserService.class)
                .classloader(UserService.class.getClassLoader())
                .build();
    }

    private String url;
    private String client;
    private UserService remoteService;

    public UserRepository(String url, String client) {
        this.url = url;
        this.client = client;
        this.remoteService = buildClient(url);
    }

    /**
     * talks a remote API to check if the user credentials are correct
     * @param userName
     * @param credential
     * @return
     */
    public boolean validateCredentials( String userName, String credential) {
        Response response = remoteService.validateLogin(this.client, userName, new UserCredentialsDto(credential));
        return HttpStatus.SC_OK == response.getStatus();
    }

    /**
     * talks to a remote API to retrieve user info
     * @param userName
     * @return
     */
    public UserDto findUserByUsername(String userName) {
        UserDto remoteUser = null;

        try {
            remoteUser = remoteService.getUserDetails(this.client, userName);
        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            LOG.warnf("Received a non OK answer from upstream migration service for user: " + userName + " - " + response.getStatus());
        }

        return remoteUser;
    }
}
