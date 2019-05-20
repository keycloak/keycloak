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
import org.keycloak.models.*;
import org.keycloak.protocol.openshift.OpenshiftProtocolEndpoint;
import org.keycloak.protocol.openshift.connections.rest.OpenshiftClient;
import org.keycloak.protocol.openshift.connections.rest.api.v1.Secrets;
import org.keycloak.protocol.openshift.connections.rest.api.v1.ServiceAccounts;
import org.keycloak.protocol.openshift.connections.rest.apis.oauth.OAuthClients;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
//@Ignore
public class CodeToTokenTest extends AbstractOpenshiftBaseTest {

    public static final String OPENSHIFT_OAUTH_CLIENT = "literal-oauth-client";
    public static final String SERVICE_ACCOUNT = "system:serviceaccount:myproject:sa-oauth";
    public static final String SERVICE_ACCOUNT_NAME = "sa-oauth";
    public static final String OPENSHIFT_OAUTH_CLIENT_WITH_CHALLENGE = "challenge-oauth-client";
    public static final String SERVICE_ACCOUNT_WITH_CHALLENGE = "system:serviceaccount:myproject:sa-challenge";
    public static final String SERVICE_ACCOUNT_WITH_CHALLENGE_NAME = "sa-challenge";
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
    protected String sa_challenge_token;

    @Before
    public void createClients() throws Exception {
        if (sa_token != null) return;
        OpenshiftClient client = AbstractOpenshiftBaseTest.createOpenshiftClient();

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

        rep = OAuthClients.OAuthClientRepresentation.create();
        // with literal scope restriction
        client.apis().oauth().clients().delete(OPENSHIFT_OAUTH_CLIENT_WITH_CHALLENGE).close();
        rep.setName(OPENSHIFT_OAUTH_CLIENT_WITH_CHALLENGE);
        rep.setGrantMethod("auto");
        rep.setSecret("geheim");
        rep.setRespondWithChallenges(true);
        rep.addRedirectURI("http://localhost");
        client.apis().oauth().clients().create(rep).close();

        client.api().namespace("myproject").serviceAccounts().delete(SERVICE_ACCOUNT_NAME).close();
        ServiceAccounts.ServiceAccountRepresentation sa = new ServiceAccounts.ServiceAccountRepresentation();
        sa.setName(SERVICE_ACCOUNT_NAME);
        sa.setNamespace("myproject");
        sa.setOauthWantChallenges(false);
        sa.addRedirectUri("http:/host1");
        sa = client.api().namespace("myproject").serviceAccounts().create(sa);
        for (int i = 0; i < 5; i++) {
            // sleep as generating secrets takes awhile.
            Thread.sleep(1000);
            sa = client.api().namespace("myproject").serviceAccounts().get(SERVICE_ACCOUNT_NAME);
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

        client.api().namespace("myproject").serviceAccounts().delete(SERVICE_ACCOUNT_WITH_CHALLENGE_NAME).close();
        sa = new ServiceAccounts.ServiceAccountRepresentation();
        sa.setName(SERVICE_ACCOUNT_WITH_CHALLENGE_NAME);
        sa.setNamespace("myproject");
        sa.setOauthWantChallenges(true);
        sa.addRedirectUri("http://localhost");
        sa = client.api().namespace("myproject").serviceAccounts().create(sa);
        for (int i = 0; i < 5; i++) {
            // sleep as generating secrets takes awhile.
            Thread.sleep(1000);
            sa = client.api().namespace("myproject").serviceAccounts().get(SERVICE_ACCOUNT_WITH_CHALLENGE_NAME);
            if (sa.getSecrets().isEmpty()) continue;
            break;
        }

        for (String secret : sa.getSecrets()) {
            Secrets.SecretRepresentation secretRep = client.api().namespace("myproject").secrets().get(secret);
            if (secretRep.isServiceAccountToken()) {
                sa_challenge_token = secretRep.getToken();
                //System.out.println(client.api().namespace("myproject").secrets().getPretty(secret, true));
                break;
            }
        }
        Assert.assertNotNull(sa_challenge_token);

        client.close();



    }

    @Test
    public void testOAuthClient() throws Exception {
        Client httpClient = javax.ws.rs.client.ClientBuilder.newClient();
        String grantUri = getResourceOwnerPasswordCredentialGrantUrl();
        WebTarget grantTarget = httpClient.target(grantUri);
        String tokenReviewUrl = getTokenReviewUrl();
        WebTarget tokenReviewTarget =  httpClient.target(tokenReviewUrl);
        oauth.clientId(OPENSHIFT_OAUTH_CLIENT_WITH_CHALLENGE);
        oauth.redirectUri("http://localhost");
        WebTarget authUrl = httpClient.target(oauth.getLoginFormUrl());

        Response response = authUrl.request().get();
        Assert.assertEquals(302, response.getStatus());
        response.close();
        URI location = response.getLocation();
        response = httpClient.target(location).request().get();
        Assert.assertEquals(401, response.getStatus());
        Assert.assertNotNull(response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
        response.close();
        String header = BasicAuthHelper.createHeader("reviewer", "password");
        response = httpClient.target(location).request().header(HttpHeaders.AUTHORIZATION, header).get();
        Assert.assertEquals(302, response.getStatus());
        location = response.getLocation();
        response.close();
        Pattern codePattern = Pattern.compile("code=([^&]+)");
        String redirect = response.getLocation().toString();
        Matcher m = codePattern.matcher(redirect);
        Assert.assertTrue(m.find());
        String code = m.group(1);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "geheim");
        Assert.assertNotNull(tokenResponse.getAccessToken());
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
        oauth.clientId(SERVICE_ACCOUNT_WITH_CHALLENGE);
        oauth.redirectUri("http://localhost");
        WebTarget authUrl = httpClient.target(oauth.getLoginFormUrl());

        Response response = authUrl.request().get();
        Assert.assertEquals(302, response.getStatus());
        response.close();
        URI location = response.getLocation();
        response = httpClient.target(location).request().get();
        Assert.assertEquals(401, response.getStatus());
        Assert.assertNotNull(response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
        response.close();
        String header = BasicAuthHelper.createHeader("reviewer", "password");
        response = httpClient.target(location).request().header(HttpHeaders.AUTHORIZATION, header).get();
        Assert.assertEquals(302, response.getStatus());
        location = response.getLocation();
        response.close();
        Pattern codePattern = Pattern.compile("code=([^&]+)");
        String redirect = response.getLocation().toString();
        Matcher m = codePattern.matcher(redirect);
        Assert.assertTrue(m.find());
        String code = m.group(1);

        String accessTokenUrl = oauth.getAccessTokenUrl();
        Form form = new Form();
        form.param("client_id", SERVICE_ACCOUNT_WITH_CHALLENGE);
        form.param("client_secret", sa_challenge_token);
        form.param(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri());
        form.param(OAuth2Constants.CODE, code);
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE);
        response = httpClient.target(accessTokenUrl).request().post(Entity.form(form));
        Assert.assertEquals(200, response.getStatus());
        //String data = response.readEntity(String.class);
        AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);

        //OAuthClient.AccessTokenResponse tokenResponse =response.readEntity(OAuthClient.AccessTokenResponse.class);
        Assert.assertNotNull(tokenResponse.getToken());
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
