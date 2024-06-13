/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.authz.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.keycloak.common.Profile.Feature.AUTHORIZATION;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.adapters.authorization.cip.spi.ClaimInformationPointProvider;
import org.keycloak.adapters.authorization.cip.spi.ClaimInformationPointProviderFactory;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.util.AuthzTestUtils;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ClaimInformationPointProviderTest extends AbstractKeycloakTest {

    private static Undertow httpService;

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    @BeforeClass
    public static void onBeforeClass() {
        httpService = Undertow.builder().addHttpListener(8989, "localhost").setHandler(exchange -> {
            if (exchange.isInIoThread()) {
                try {
                    if (exchange.getRelativePath().equals("/post-claim-information-provider")) {
                        FormParserFactory parserFactory = FormParserFactory.builder().build();
                        FormDataParser parser = parserFactory.createParser(exchange);
                        FormData formData = parser.parseBlocking();

                        if (!("Bearer "  + accessTokenString()).equals(exchange.getRequestHeaders().getFirst("Authorization"))
                                || !"post".equalsIgnoreCase(exchange.getRequestMethod().toString())
                                || !"application/x-www-form-urlencoded".equals(exchange.getRequestHeaders().getFirst("Content-Type"))
                                || !exchange.getRequestHeaders().get("header-b").contains("header-b-value1")
                                || !exchange.getRequestHeaders().get("header-b").contains("header-b-value2")
                                || !formData.get("param-a").getFirst().getValue().equals("param-a-value1")
                                || !formData.get("param-a").getLast().getValue().equals("param-a-value2")
                                || !formData.get("param-subject").getFirst().getValue().equals("sub")
                                || !formData.get("param-user-name").getFirst().getValue().equals("username")
                                || !formData.get("param-other-claims").getFirst().getValue().equals("param-other-claims-value1")
                                || !formData.get("param-other-claims").getLast().getValue().equals("param-other-claims-value2")) {
                            exchange.setStatusCode(400);
                            return;
                        }

                        exchange.setStatusCode(200);
                    } else if (exchange.getRelativePath().equals("/get-claim-information-provider")) {
                        if (!("Bearer "  + accessTokenString()).equals(exchange.getRequestHeaders().getFirst("Authorization"))
                                || !"get".equalsIgnoreCase(exchange.getRequestMethod().toString())
                                || !exchange.getRequestHeaders().get("header-b").contains("header-b-value1")
                                || !exchange.getRequestHeaders().get("header-b").contains("header-b-value2")
                                || !exchange.getQueryParameters().get("param-a").contains("param-a-value1")
                                || !exchange.getQueryParameters().get("param-a").contains("param-a-value2")
                                || !exchange.getQueryParameters().get("param-subject").contains("sub")
                                || !exchange.getQueryParameters().get("param-user-name").contains("username")) {
                            exchange.setStatusCode(400);
                            return;
                        }

                        exchange.setStatusCode(200);
                    } else {
                        exchange.setStatusCode(404);
                    }
                } finally {
                    if (exchange.getStatusCode() == 200) {
                        try {
                            ObjectMapper mapper = JsonSerialization.mapper;
                            JsonParser jsonParser = mapper.getFactory().createParser("{\"a\": \"a-value1\", \"b\": \"b-value1\", \"d\": [\"d-value1\", \"d-value2\"]}");
                            TreeNode treeNode = mapper.readTree(jsonParser);
                            exchange.getResponseSender().send(treeNode.toString());
                        } catch (Exception ignore) {
                            ignore.printStackTrace();
                        }
                    }
                    exchange.endExchange();
                }
            }
        }).build();

        httpService.start();
    }

    @AfterClass
    public static void onAfterClass() {
        if (httpService != null) {
            httpService.stop();
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadRealm(getClass().getResourceAsStream("/authorization-test/test-authz-realm.json"));
        testRealms.add(realm);
    }

    private ClaimInformationPointProvider getClaimInformationProviderForPath(String path, String providerName) {
        PolicyEnforcer policyEnforcer = AuthzTestUtils.createPolicyEnforcer("enforcer-config-claims-provider.json", true);
        Map<String, ClaimInformationPointProviderFactory> providers = policyEnforcer.getClaimInformationPointProviderFactories();

        PathConfig pathConfig = policyEnforcer.getPaths().get(path);

        assertNotNull(pathConfig);

        Map<String, Map<String, Object>> cipConfig = pathConfig.getClaimInformationPointConfig();

        assertNotNull(cipConfig);

        ClaimInformationPointProviderFactory factory = providers.get(providerName);

        assertNotNull(factory);

        Map<String, Object> claimsConfig = cipConfig.get(providerName);

        return factory.create(claimsConfig);
    }

    @Test
    public void testBasicClaimsInformationPoint() {
        Map<String, List<String>> claims = getClaimInformationProviderForPath("/claims-provider", "claims")
                .resolve(createHttpRequest());

        assertEquals("parameter-a", claims.get("claim-from-request-parameter").get(0));
        assertEquals("header-b", claims.get("claim-from-header").get(0));
        assertEquals("cookie-c", claims.get("claim-from-cookie").get(0));
        assertEquals("user-remote-addr", claims.get("claim-from-remoteAddr").get(0));
        assertEquals("GET", claims.get("claim-from-method").get(0));
        assertEquals("/app/request-uri", claims.get("claim-from-uri").get(0));
        assertEquals("/request-relative-path", claims.get("claim-from-relativePath").get(0));
        assertEquals("true", claims.get("claim-from-secure").get(0));
        assertEquals("static value", claims.get("claim-from-static-value").get(0));
        assertEquals("static", claims.get("claim-from-multiple-static-value").get(0));
        assertEquals("value", claims.get("claim-from-multiple-static-value").get(1));
        assertEquals("Test param-other-claims-value1 and parameter-a", claims.get("param-replace-multiple-placeholder").get(0));
    }

    @Test
    public void testBodyJsonClaimsInformationPoint() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();

        headers.put("Content-Type", Arrays.asList("application/json"));

        ObjectMapper mapper = JsonSerialization.mapper;
        JsonParser parser = mapper.getFactory().createParser("{\"a\": {\"b\": {\"c\": \"c-value\"}}, \"d\": [\"d-value1\", \"d-value2\"], \"e\": {\"number\": 123}}");
        TreeNode treeNode = mapper.readTree(parser);

        Map<String, List<String>> claims = getClaimInformationProviderForPath("/claims-provider", "claims").resolve(
                createHttpRequest(headers, new ByteArrayInputStream(treeNode.toString().getBytes())));

        assertEquals("c-value", claims.get("claim-from-json-body-object").get(0));
        assertEquals("d-value2", claims.get("claim-from-json-body-array").get(0));
        assertEquals("123", claims.get("claim-from-json-body-number").get(0));
    }

    @Test
    public void testBodyJsonObjectClaim() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();

        headers.put("Content-Type", Arrays.asList("application/json"));

        ObjectMapper mapper = JsonSerialization.mapper;
        JsonParser parser = mapper.getFactory().createParser("{\"Individual\" : {\n"
                + "\n"
                + "                \"Name\":  \"John\",\n"
                + "\n"
                + "                \"Lastname\": \"Doe\",\n"
                + "\n"
                + "                \"individualRoles\" : [ {\n"
                + "\n"
                + "                                \"roleSpec\": 2342,\n"
                + "\n"
                + "                                \"roleId\": 4234},\n"
                + "\n"
                + "{\n"
                + "\n"
                + "                                \"roleSpec\": 4223,\n"
                + "\n"
                + "                                \"roleId\": 523\n"
                + "\n"
                + "                }\n"
                + "\n"
                + "                ]\n"
                + "\n"
                + "}}");
        TreeNode treeNode = mapper.readTree(parser);

        Map<String, List<String>> claims = getClaimInformationProviderForPath("/claims-from-body-json-object", "claims")
                .resolve(createHttpRequest(headers, new ByteArrayInputStream(treeNode.toString().getBytes())));

        assertEquals(1, claims.size());
        assertEquals(2, claims.get("individualRoles").size());
        assertEquals("{\"roleSpec\":2342,\"roleId\":4234}", claims.get("individualRoles").get(0));
        assertEquals("{\"roleSpec\":4223,\"roleId\":523}", claims.get("individualRoles").get(1));

        headers.put("Content-Type", Arrays.asList("application/json; charset=utf-8"));

        claims = getClaimInformationProviderForPath("/claims-from-body-json-object", "claims")
                .resolve(createHttpRequest(headers, new ByteArrayInputStream(treeNode.toString().getBytes())));

        assertEquals(1, claims.size());
        assertEquals(2, claims.get("individualRoles").size());
        assertEquals("{\"roleSpec\":2342,\"roleId\":4234}", claims.get("individualRoles").get(0));
        assertEquals("{\"roleSpec\":4223,\"roleId\":523}", claims.get("individualRoles").get(1));
    }

    @Test
    public void testBodyClaimsInformationPoint() {
        Map<String, List<String>> claims = getClaimInformationProviderForPath("/claims-provider", "claims")
                .resolve(createHttpRequest(new HashMap<>(), new ByteArrayInputStream("raw-body-text".getBytes())));

        assertEquals("raw-body-text", claims.get("claim-from-body").get(0));
    }

    @Test
    public void testHttpClaimInformationPointProviderWithoutClaims() {
        Map<String, List<String>> claims = getClaimInformationProviderForPath("/http-get-claim-provider", "http")
                .resolve(createHttpRequest(new HashMap<>(), null));

        assertEquals("a-value1", claims.get("a").get(0));
        assertEquals("b-value1", claims.get("b").get(0));
        assertEquals("d-value1", claims.get("d").get(0));
        assertEquals("d-value2", claims.get("d").get(1));

        assertNull(claims.get("claim-a"));
        assertNull(claims.get("claim-d"));
        assertNull(claims.get("claim-d0"));
        assertNull(claims.get("claim-d-all"));
    }

    @Test
    public void testHttpClaimInformationPointProviderWithClaims() {
        Map<String, List<String>> claims = getClaimInformationProviderForPath("/http-post-claim-provider", "http")
                .resolve(createHttpRequest(new HashMap<>(), null));

        assertEquals("a-value1", claims.get("claim-a").get(0));
        assertEquals("d-value1", claims.get("claim-d").get(0));
        assertEquals("d-value2", claims.get("claim-d").get(1));
        assertEquals("d-value1", claims.get("claim-d0").get(0));
        assertEquals("d-value1", claims.get("claim-d-all").get(0));
        assertEquals("d-value2", claims.get("claim-d-all").get(1));

        assertNull(claims.get("a"));
        assertNull(claims.get("b"));
        assertNull(claims.get("d"));
    }

    private static HttpRequest createHttpRequest() {
        return createHttpRequest(new HashMap<>(), null);
    }

    private static HttpRequest createHttpRequest(Map<String, List<String>> headers, InputStream requestBody) {
        Map<String, List<String>> queryParameter = new HashMap<>();
        queryParameter.put("a", Arrays.asList("parameter-a"));
        headers.put("b", Arrays.asList("header-b"));
        Map<String, String> cookies = new HashMap<>();
        cookies.put("c", "cookie-c");
        return AuthzTestUtils.createHttpRequest("/app/request-uri", "/request-relative-path", "GET",
                accessTokenString(), headers, queryParameter, cookies, requestBody);
    }

    private static AccessToken accessToken() {
        AccessToken token = new AccessToken();
        token.subject("sub");
        token.setPreferredUsername("username");
        token.getOtherClaims().put("custom_claim", Arrays.asList("param-other-claims-value1", "param-other-claims-value2"));
        return token;
    }

    private static String accessTokenString() {
        return new JWSBuilder().jsonContent(accessToken()).none();
    }
}
