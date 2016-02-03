/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.examples.broker.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * <p>A simple servlet to proxy Twitter API using the Twitter4j library.</p>
 *
 * <p>It provides some additional code to properly handle token retrieval from the Twitter identity provider in Keycloak
 * and use that token to invoke Twitter's API.</p>
 *
 * @author pedroigor
 */
@WebServlet(urlPatterns = "/twitter/showUser")
public class TwitterShowUserServlet extends HttpServlet {

    private Keycloak keycloak;
    private String authServer;
    private String realmName;
    private IdentityProviderRepresentation identityProvider;

    @Override
    public void init(ServletConfig config) throws ServletException {
        initKeycloakClient(config);
    }

    @Override
    public void destroy() {
        this.keycloak.close();
    }

    @Override
    protected void doGet(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        TwitterOAuthResponse twitterOAuthResponse = getTwitterOAuthResponse(request);
        ConfigurationBuilder cb = new ConfigurationBuilder();

        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(this.identityProvider.getConfig().get("clientId"))
                .setOAuthConsumerSecret(this.identityProvider.getConfig().get("clientSecret"))
                .setOAuthAccessToken(twitterOAuthResponse.getToken())
                .setOAuthAccessTokenSecret(twitterOAuthResponse.getTokenSecret());

        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance();

        try {
            User user = twitter.users().showUser(twitterOAuthResponse.getScreenName());

            response.setContentType(MediaType.APPLICATION_JSON);

            PrintWriter writer = response.getWriter();

            writer.println(new ObjectMapper().writeValueAsString(user));

            writer.flush();
        } catch (TwitterException e) {
            throw new RuntimeException("Could not load social profile.", e);
        }
    }

    private TwitterOAuthResponse getTwitterOAuthResponse(final HttpServletRequest req) {
        ClientRequestFilter authFilter = new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                KeycloakSecurityContext securityContext = (KeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());
                String accessToken = securityContext.getTokenString();

                requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            }
        };

        Client client = ClientBuilder.newBuilder().register(authFilter).build();
        WebTarget target = client.target(getIdentityProviderTokenUrl());

        return target.request().get().readEntity(TwitterOAuthResponse.class);
    }

    private String getIdentityProviderTokenUrl() {
        return this.authServer + "/realms/" + this.realmName + "/broker/" + this.identityProvider.getAlias() + "/token";
    }

    private void initKeycloakClient(ServletConfig config) {
        ServletContext servletContext = config.getServletContext();
        JsonNode keycloakConfig;

        try {
            keycloakConfig = new ObjectMapper().readTree(servletContext.getResourceAsStream("WEB-INF/keycloak.json"));
        } catch (IOException e) {
            throw new RuntimeException("Could not parse keycloak config.", e);
        }

        this.authServer = keycloakConfig.get("auth-server-url").asText();
        this.realmName = keycloakConfig.get("realm").asText();
        this.keycloak = Keycloak.getInstance(authServer, realmName, "admin", "password", "admin-client", "password");
        IdentityProvidersResource providersResource = keycloak.realm(realmName).identityProviders();
        this.identityProvider = providersResource.get("twitter").toRepresentation();
    }
}
