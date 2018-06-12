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
package org.keycloak.testsuite.openshift;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Errors;
import org.keycloak.models.*;
import org.keycloak.protocol.openshift.OpenshiftProtocolEndpoint;
import org.keycloak.protocol.openshift.TokenReviewRequestRepresentation;
import org.keycloak.protocol.openshift.TokenReviewResponseRepresentation;
import org.keycloak.protocol.openshift.connections.rest.OpenshiftClient;
import org.keycloak.protocol.openshift.connections.rest.api.v1.Secrets;
import org.keycloak.protocol.openshift.connections.rest.api.v1.ServiceAccounts;
import org.keycloak.protocol.openshift.connections.rest.apis.oauth.OAuthClients;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
//@Ignore
public class TokenReviewEndpointTest extends AbstractOpenshiftBaseTest {

    public static final String OPENSHIFT_OAUTH_CLIENT = "literal-oauth-client";
    public static final String SERVICE_ACCOUNT = "system:serviceaccount:myproject:sa-oauth";
    @Rule
    public AssertEvents events = new AssertEvents(this);


    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Deployment
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(UserResource.class)
                .addPackages(true, "org.keycloak.testsuite");
    }


    @Before
    public void setupTest() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            if (session.users().getUserByUsername("reviewer", realm) != null) return;

            ClientModel client = session.realms().getClientByClientId("test-app", realm);
            client.setDirectAccessGrantsEnabled(true);

            GroupModel group = realm.createGroup("openshift");
            GroupModel child = realm.createGroup("child");
            realm.moveGroup(child, group);

            UserModel user = session.users().addUser(realm, "reviewer");
            user.setEnabled(true);
            session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password("password"));
            user.joinGroup(child);


        });
    }

    protected String sa_token;

    @Before
    public void createClients() throws Exception {
        if (sa_token != null) return;
        OpenshiftClient client = createOpenshiftClient();

        OAuthClients.OAuthClientRepresentation rep = OAuthClients.OAuthClientRepresentation.create();
        // with literal scope restriction
        client.apis().oauth().clients().delete(OPENSHIFT_OAUTH_CLIENT).close();
        rep.setName(OPENSHIFT_OAUTH_CLIENT);
        rep.setGrantMethod("auto");
        rep.setSecret("geheim");
        rep.setRespondWithChallenges(false);
        rep.addRedirectURI("http://host1");
        rep.addRedirectURI("http://host2");
        rep.addLiteralScopeRestriction("foo");
        rep.addLiteralScopeRestriction("foo:bar");
        OAuthClients.OAuthClientRepresentation.ClusterRoleRestriction restriction = new OAuthClients.OAuthClientRepresentation.ClusterRoleRestriction();
        restriction.setAllowEscalation(true);
        restriction.getNamespaces().add("myproject");
        restriction.getRoleNames().add("myrole");
        rep.addClusterRoleScopeRestriction(restriction);
        restriction = new OAuthClients.OAuthClientRepresentation.ClusterRoleRestriction();
        restriction.setAllowEscalation(false);
        restriction.getNamespaces().add("myproject");
        restriction.getRoleNames().add("another");
        rep.addClusterRoleScopeRestriction(restriction);

        client.apis().oauth().clients().create(rep).close();

        client.api().namespace("myproject").serviceAccounts().delete("sa-oauth").close();
        ServiceAccounts.ServiceAccountRepresentation sa = new ServiceAccounts.ServiceAccountRepresentation();
        sa.setName("sa-oauth");
        sa.setNamespace("myproject");
        sa.setOauthWantChallenges(true);
        sa.addRedirectUri("http:/host1");
        sa.addRedirectUri("http:/host2");
        sa = client.api().namespace("myproject").serviceAccounts().create(sa);
        for (int i = 0; i < 5; i++) {
            // sleep as generating secrets takes awhile.
            Thread.sleep(1000);
            sa = client.api().namespace("myproject").serviceAccounts().get("sa-oauth");
            if (sa.getSecrets().isEmpty()) continue;
            break;
        }

        for (String secret : sa.getSecrets()) {
            Secrets.SecretRepresentation secretRep = client.api().namespace("myproject").secrets().get(secret);
            if (secretRep.isServiceAccountToken()) {
                sa_token = secretRep.getToken();
                //System.out.println(client.api().namespace("myproject").secrets().getPretty(secret, true));
                break;
            }
        }
        Assert.assertNotNull(sa_token);

        client.close();



    }

    @Test
    public void testOAuthClient() throws Exception {
        Client httpClient = javax.ws.rs.client.ClientBuilder.newClient();
        String grantUri = getResourceOwnerPasswordCredentialGrantUrl();
        WebTarget grantTarget = httpClient.target(grantUri);
        String tokenReviewUrl = getTokenReviewUrl();
        WebTarget tokenReviewTarget =  httpClient.target(tokenReviewUrl);


        // test good scope/token
        {
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("client_id", OPENSHIFT_OAUTH_CLIENT);
            form.param("client_secret", "geheim");
            form.param("username", "reviewer");
            form.param("password", "password");
            form.param("scope", "foo foo:bar role:myrole:myproject");
            Response response = grantTarget.request()
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
            String accessToken = tokenResponse.getToken();
            Assert.assertNotNull(accessToken);
            response.close();

            response = tokenReviewTarget.request()
                    .post(Entity.json(TokenReviewRequestRepresentation.build(accessToken)));
            assertEquals(200, response.getStatus());
            String reviewString = response.readEntity(String.class);
            //System.out.println(reviewString);
            TokenReviewResponseRepresentation review = JsonSerialization.readValue(reviewString, TokenReviewResponseRepresentation.class);
            Assert.assertTrue(review.getStatus().isAuthenticated());
            TokenReviewResponseRepresentation.Status.User user = review.getStatus().getUser();
            Assert.assertEquals("reviewer", user.getUsername());
            Assert.assertTrue(user.getGroups().contains("openshift"));
            Assert.assertTrue(user.getGroups().contains("openshift:child"));
            List<String> scopes = (List<String>)user.getExtra().getData().get("scopes.authorization.openshift.io");
            Assert.assertTrue(scopes.contains("foo"));
            Assert.assertTrue(scopes.contains("foo:bar"));
            Assert.assertTrue(scopes.contains("role:myrole:myproject"));

        }


        // test escalation
        {
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("client_id", OPENSHIFT_OAUTH_CLIENT);
            form.param("client_secret", "geheim");
            form.param("username", "reviewer");
            form.param("password", "password");
            form.param("scope", "role:myrole:myproject:! role:another:myproject");
            Response response = grantTarget.request()
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
            String accessToken = tokenResponse.getToken();
            Assert.assertNotNull(accessToken);
            response.close();

            response = tokenReviewTarget.request()
                    .post(Entity.json(TokenReviewRequestRepresentation.build(accessToken)));
            assertEquals(200, response.getStatus());
            String reviewString = response.readEntity(String.class);
            //System.out.println(reviewString);
            TokenReviewResponseRepresentation review = JsonSerialization.readValue(reviewString, TokenReviewResponseRepresentation.class);
            Assert.assertTrue(review.getStatus().isAuthenticated());
            TokenReviewResponseRepresentation.Status.User user = review.getStatus().getUser();
            Assert.assertEquals("reviewer", user.getUsername());
            List<String> scopes = (List<String>)user.getExtra().getData().get("scopes.authorization.openshift.io");
            Assert.assertTrue(scopes.contains("role:myrole:myproject:!"));
            Assert.assertTrue(scopes.contains("role:another:myproject"));

        }
        // test bad scope
        {
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("client_id", OPENSHIFT_OAUTH_CLIENT);
            form.param("client_secret", "geheim");
            form.param("username", "reviewer");
            form.param("password", "password");
            form.param("scope", "bad_scope");
            Response response = grantTarget.request()
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
            String accessToken = tokenResponse.getToken();
            Assert.assertNotNull(accessToken);
            response.close();

            response = tokenReviewTarget.request()
                    .post(Entity.json(TokenReviewRequestRepresentation.build(accessToken)));
            assertEquals(401, response.getStatus());
            String reviewString = response.readEntity(String.class);
            //System.out.println(reviewString);
            TokenReviewResponseRepresentation review = JsonSerialization.readValue(reviewString, TokenReviewResponseRepresentation.class);
            Assert.assertFalse(review.getStatus().isAuthenticated());
            Assert.assertEquals(Errors.INVALID_SCOPE, review.getStatus().getError());
        }

        // test bad escalation
        {
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("client_id", OPENSHIFT_OAUTH_CLIENT);
            form.param("client_secret", "geheim");
            form.param("username", "reviewer");
            form.param("password", "password");
            form.param("scope", "role:another:myproject:!");
            Response response = grantTarget.request()
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
            String accessToken = tokenResponse.getToken();
            Assert.assertNotNull(accessToken);
            response.close();

            response = tokenReviewTarget.request()
                    .post(Entity.json(TokenReviewRequestRepresentation.build(accessToken)));
            assertEquals(401, response.getStatus());
            String reviewString = response.readEntity(String.class);
            //System.out.println(reviewString);
            TokenReviewResponseRepresentation review = JsonSerialization.readValue(reviewString, TokenReviewResponseRepresentation.class);
            Assert.assertFalse(review.getStatus().isAuthenticated());
            Assert.assertEquals(Errors.INVALID_SCOPE, review.getStatus().getError());
        }


        httpClient.close();
        events.clear();
    }

    @Test
    public void testServiceAccount() throws Exception {
        Client httpClient = javax.ws.rs.client.ClientBuilder.newClient();
        String grantUri = getResourceOwnerPasswordCredentialGrantUrl();
        WebTarget grantTarget = httpClient.target(grantUri);
        String tokenReviewUrl = getTokenReviewUrl();
        WebTarget tokenReviewTarget =  httpClient.target(tokenReviewUrl);


        // test good scope/token
        {
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("client_id", SERVICE_ACCOUNT);
            form.param("client_secret", sa_token);
            form.param("username", "reviewer");
            form.param("password", "password");
            form.param("scope", "user:info user:check-access role:myrole:myproject role:another:myproject:!");
            Response response = grantTarget.request()
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
            String accessToken = tokenResponse.getToken();
            Assert.assertNotNull(accessToken);
            response.close();

            response = tokenReviewTarget.request()
                    .post(Entity.json(TokenReviewRequestRepresentation.build(accessToken)));
            assertEquals(200, response.getStatus());
            String reviewString = response.readEntity(String.class);
            //System.out.println(reviewString);
            TokenReviewResponseRepresentation review = JsonSerialization.readValue(reviewString, TokenReviewResponseRepresentation.class);
            Assert.assertTrue(review.getStatus().isAuthenticated());
            TokenReviewResponseRepresentation.Status.User user = review.getStatus().getUser();
            Assert.assertEquals("reviewer", user.getUsername());
            Assert.assertTrue(user.getGroups().contains("openshift"));
            Assert.assertTrue(user.getGroups().contains("openshift:child"));
            List<String> scopes = (List<String>)user.getExtra().getData().get("scopes.authorization.openshift.io");
            Assert.assertTrue(scopes.contains("user:info"));
            Assert.assertTrue(scopes.contains("user:check-access"));
            Assert.assertTrue(scopes.contains("role:myrole:myproject"));
            Assert.assertTrue(scopes.contains("role:another:myproject:!"));

        }

        // test bad scope
        {
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("client_id", SERVICE_ACCOUNT);
            form.param("client_secret", sa_token);
            form.param("username", "reviewer");
            form.param("password", "password");
            form.param("scope", "bad_scope");
            Response response = grantTarget.request()
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
            String accessToken = tokenResponse.getToken();
            Assert.assertNotNull(accessToken);
            response.close();

            response = tokenReviewTarget.request()
                    .post(Entity.json(TokenReviewRequestRepresentation.build(accessToken)));
            assertEquals(401, response.getStatus());
            String reviewString = response.readEntity(String.class);
            //System.out.println(reviewString);
            TokenReviewResponseRepresentation review = JsonSerialization.readValue(reviewString, TokenReviewResponseRepresentation.class);
            Assert.assertFalse(review.getStatus().isAuthenticated());
            Assert.assertEquals(Errors.INVALID_SCOPE, review.getStatus().getError());
        }

        // test bad role namespace
        {
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("client_id", SERVICE_ACCOUNT);
            form.param("client_secret", sa_token);
            form.param("username", "reviewer");
            form.param("password", "password");
            form.param("scope", "role:another:badproject");
            Response response = grantTarget.request()
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
            String accessToken = tokenResponse.getToken();
            Assert.assertNotNull(accessToken);
            response.close();

            response = tokenReviewTarget.request()
                    .post(Entity.json(TokenReviewRequestRepresentation.build(accessToken)));
            assertEquals(401, response.getStatus());
            String reviewString = response.readEntity(String.class);
            //System.out.println(reviewString);
            TokenReviewResponseRepresentation review = JsonSerialization.readValue(reviewString, TokenReviewResponseRepresentation.class);
            Assert.assertFalse(review.getStatus().isAuthenticated());
            Assert.assertEquals(Errors.INVALID_SCOPE, review.getStatus().getError());
        }

        // test bad role namespace
        {
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("client_id", SERVICE_ACCOUNT);
            form.param("client_secret", sa_token);
            form.param("username", "reviewer");
            form.param("password", "password");
            form.param("scope", "role:another:badproject:!");
            Response response = grantTarget.request()
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
            String accessToken = tokenResponse.getToken();
            Assert.assertNotNull(accessToken);
            response.close();

            response = tokenReviewTarget.request()
                    .post(Entity.json(TokenReviewRequestRepresentation.build(accessToken)));
            assertEquals(401, response.getStatus());
            String reviewString = response.readEntity(String.class);
            //System.out.println(reviewString);
            TokenReviewResponseRepresentation review = JsonSerialization.readValue(reviewString, TokenReviewResponseRepresentation.class);
            Assert.assertFalse(review.getStatus().isAuthenticated());
            Assert.assertEquals(Errors.INVALID_SCOPE, review.getStatus().getError());
        }


        httpClient.close();
        events.clear();
    }

    public String getResourceOwnerPasswordCredentialGrantUrl() {
        UriBuilder b = OpenshiftProtocolEndpoint.tokenUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT));
        return b.build("test").toString();
    }

    public String getTokenReviewUrl() {
        UriBuilder b = OpenshiftProtocolEndpoint.tokenReviewnUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT));
        return b.build("test").toString();
    }


}
