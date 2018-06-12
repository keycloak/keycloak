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
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.openshift.OpenshiftProtocolEndpoint;
import org.keycloak.protocol.openshift.TokenReviewRequestRepresentation;
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

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
//@Ignore
public class OpenshiftClientStorageTest extends AbstractOpenshiftBaseTest {

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

    protected String sa_token;

    @Before
    public void createClients() throws Exception {
        if (sa_token != null) return;
        OpenshiftClient client = AbstractOpenshiftBaseTest.createOpenshiftClient();

        OAuthClients.OAuthClientRepresentation rep = OAuthClients.OAuthClientRepresentation.create();
        // with literal scope restriction
        client.apis().oauth().clients().delete("literal-oauth-client").close();
        rep.setName("literal-oauth-client");
        rep.setGrantMethod("auto");
        rep.setSecret("geheim");
        rep.setRespondWithChallenges(false);
        rep.addRedirectURI("http://host1");
        rep.addRedirectURI("http://host2");
        rep.addLiteralScopeRestriction("foo");
        rep.addLiteralScopeRestriction("foo:bar");
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

        TokenReviewRequestRepresentation request = TokenReviewRequestRepresentation.create(sa_token);
        String response = client.apis().kubernetesAuthentication().tokenReview().reviewPretty(request, true);
        //System.out.println(response);


        client.close();



    }

    //@Test
    public void testServer() throws Exception {
        testingClient.server().run(session -> {
                    RealmModel realm = session.realms().getRealmByName("test");

                    ClientModel client = session.realms().getClientByClientId("kcinit", realm);
                    if (client != null) {
                        return;
                    }

                    ClientModel kcinit = realm.addClient("kcinit");
                    kcinit.setEnabled(true);
                    kcinit.addRedirectUri("http://localhost:*");
                    kcinit.setPublicClient(true);
                });

        Thread.sleep(10000000000000l);
    }

    @Test
    public void testOAuthClient() throws Exception {
        Client httpClient = javax.ws.rs.client.ClientBuilder.newClient();
        String grantUri = getResourceOwnerPasswordCredentialGrantUrl();
        WebTarget grantTarget = httpClient.target(grantUri);

        String accessToken = null;
        {   // test valid password
            String header = BasicAuthHelper.createHeader("literal-oauth-client", "geheim");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("username", "test-user@localhost");
            form.param("password", "password");
            form.param("scope", "oauth openid");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
            accessToken = tokenResponse.getToken();
            Assert.assertNotNull(accessToken);
            response.close();
        }
        httpClient.close();
        events.clear();
    }

    @Test
    public void testServiceAccount() throws Exception {
        Client httpClient = javax.ws.rs.client.ClientBuilder.newClient();
        String grantUri = getResourceOwnerPasswordCredentialGrantUrl();
        WebTarget grantTarget = httpClient.target(grantUri);

        String accessToken = null;
        {   // test valid password
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("client_id", "system:serviceaccount:myproject:sa-oauth");
            form.param("client_secret", sa_token);
            form.param("username", "test-user@localhost");
            form.param("password", "password");
            form.param("scope", "oauth openid");
            Response response = grantTarget.request()
                    .post(Entity.form(form));
            assertEquals(200, response.getStatus());
            AccessTokenResponse tokenResponse = response.readEntity(AccessTokenResponse.class);
            accessToken = tokenResponse.getToken();
            Assert.assertNotNull(accessToken);
            response.close();
        }
        httpClient.close();
        events.clear();
    }

    public String getResourceOwnerPasswordCredentialGrantUrl() {
        UriBuilder b = OpenshiftProtocolEndpoint.tokenUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT));
        return b.build("test").toString();
    }

}
