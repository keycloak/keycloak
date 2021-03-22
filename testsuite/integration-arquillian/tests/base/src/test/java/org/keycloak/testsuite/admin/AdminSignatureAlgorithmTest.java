package org.keycloak.testsuite.admin;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.crypto.Algorithm;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.TokenSignatureUtil;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AdminSignatureAlgorithmTest extends AbstractKeycloakTest {

    private CloseableHttpClient client;

    @Before
    public void before() {
        client = HttpClientBuilder.create().build();
    }

    @After
    public void after() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void changeRealmTokenAlgorithm() throws Exception {
        String defaultSignatureAlgorithm = adminClient.realm("master").toRepresentation().getDefaultSignatureAlgorithm();

        TokenSignatureUtil.changeRealmTokenSignatureProvider("master", adminClient, Algorithm.ES256);

        try (Keycloak adminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), suiteContext.getAuthServerInfo().getContextRoot().toString())) {
            AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(accessToken.getToken(), AccessToken.class);
            assertEquals(Algorithm.ES256, verifier.getHeader().getAlgorithm().name());

            assertNotNull(adminClient.realms().findAll());

            String whoAmiUrl = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/admin/master/console/whoami";

            JsonNode jsonNode = SimpleHttp.doGet(whoAmiUrl, client).auth(accessToken.getToken()).asJson();
            assertNotNull(jsonNode.get("realm"));
            assertNotNull(jsonNode.get("userId"));
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider("master", adminClient, defaultSignatureAlgorithm);
        }
    }

}
