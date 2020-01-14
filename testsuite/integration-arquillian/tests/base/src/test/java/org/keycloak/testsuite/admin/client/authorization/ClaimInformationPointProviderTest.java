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
package org.keycloak.testsuite.admin.client.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.cert.X509Certificate;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.authorization.ClaimInformationPointProvider;
import org.keycloak.adapters.authorization.ClaimInformationPointProviderFactory;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.HttpFacade.Cookie;
import org.keycloak.adapters.spi.HttpFacade.Request;
import org.keycloak.adapters.spi.HttpFacade.Response;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class ClaimInformationPointProviderTest extends AbstractKeycloakTest {

    private static Undertow httpService;

    @BeforeClass
    public static void onBeforeClass() {
        httpService = Undertow.builder().addHttpListener(8989, "localhost").setHandler(exchange -> {
            if (exchange.isInIoThread()) {
                try {
                    if (exchange.getRelativePath().equals("/post-claim-information-provider")) {
                        FormParserFactory parserFactory = FormParserFactory.builder().build();
                        FormDataParser parser = parserFactory.createParser(exchange);
                        FormData formData = parser.parseBlocking();

                        if (!"Bearer tokenString".equals(exchange.getRequestHeaders().getFirst("Authorization"))
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
                        if (!"Bearer idTokenString".equals(exchange.getRequestHeaders().getFirst("Authorization"))
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
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getClass().getResourceAsStream("/authorization-test/enforcer-config-claims-provider.json"));
        deployment.setClient(HttpClients.createDefault());
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
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
        HttpFacade httpFacade = createHttpFacade();
        Map<String, List<String>> claims = getClaimInformationProviderForPath("/claims-provider", "claims").resolve(httpFacade);

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
        HttpFacade httpFacade = createHttpFacade(headers, new ByteArrayInputStream(treeNode.toString().getBytes()));

        Map<String, List<String>> claims = getClaimInformationProviderForPath("/claims-provider", "claims").resolve(httpFacade);

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
        HttpFacade httpFacade = createHttpFacade(headers, new ByteArrayInputStream(treeNode.toString().getBytes()));

        Map<String, List<String>> claims = getClaimInformationProviderForPath("/claims-from-body-json-object", "claims").resolve(httpFacade);

        assertEquals(1, claims.size());
        assertEquals(2, claims.get("individualRoles").size());
        assertEquals("{\"roleSpec\":2342,\"roleId\":4234}", claims.get("individualRoles").get(0));
        assertEquals("{\"roleSpec\":4223,\"roleId\":523}", claims.get("individualRoles").get(1));

        headers.put("Content-Type", Arrays.asList("application/json; charset=utf-8"));

        httpFacade = createHttpFacade(headers, new ByteArrayInputStream(treeNode.toString().getBytes()));
        claims = getClaimInformationProviderForPath("/claims-from-body-json-object", "claims").resolve(httpFacade);

        assertEquals(1, claims.size());
        assertEquals(2, claims.get("individualRoles").size());
        assertEquals("{\"roleSpec\":2342,\"roleId\":4234}", claims.get("individualRoles").get(0));
        assertEquals("{\"roleSpec\":4223,\"roleId\":523}", claims.get("individualRoles").get(1));
    }

    @Test
    public void testBodyClaimsInformationPoint() {
        HttpFacade httpFacade = createHttpFacade(new HashMap<>(), new ByteArrayInputStream("raw-body-text".getBytes()));

        Map<String, List<String>> claims = getClaimInformationProviderForPath("/claims-provider", "claims").resolve(httpFacade);

        assertEquals("raw-body-text", claims.get("claim-from-body").get(0));
    }

    @Test
    public void testHttpClaimInformationPointProviderWithoutClaims() {
        HttpFacade httpFacade = createHttpFacade();

        Map<String, List<String>> claims = getClaimInformationProviderForPath("/http-get-claim-provider", "http").resolve(httpFacade);

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
        HttpFacade httpFacade = createHttpFacade();

        Map<String, List<String>> claims = getClaimInformationProviderForPath("/http-post-claim-provider", "http").resolve(httpFacade);

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

    private HttpFacade createHttpFacade(Map<String, List<String>> headers, InputStream requestBody) {
        return new OIDCHttpFacade() {
            private Request request;

            @Override
            public KeycloakSecurityContext getSecurityContext() {
                AccessToken token = new AccessToken();

                token.subject("sub");
                token.setPreferredUsername("username");
                token.getOtherClaims().put("custom_claim", Arrays.asList("param-other-claims-value1", "param-other-claims-value2"));

                IDToken idToken = new IDToken();

                idToken.subject("sub");
                idToken.setPreferredUsername("username");
                idToken.getOtherClaims().put("custom_claim", Arrays.asList("param-other-claims-value1", "param-other-claims-value2"));

                return new KeycloakSecurityContext("tokenString", token, "idTokenString", idToken);
            }

            @Override
            public Request getRequest() {
                if (request == null) {
                    request = createHttpRequest(headers, requestBody);
                }
                return request;
            }

            @Override
            public Response getResponse() {
                return createHttpResponse();
            }

            @Override
            public X509Certificate[] getCertificateChain() {
                return new X509Certificate[0];
            }
        };
    }

    private HttpFacade createHttpFacade() {
        return createHttpFacade(new HashMap<>(), null);
    }

    private Response createHttpResponse() {
        return new Response() {
            @Override
            public void setStatus(int status) {

            }

            @Override
            public void addHeader(String name, String value) {

            }

            @Override
            public void setHeader(String name, String value) {

            }

            @Override
            public void resetCookie(String name, String path) {

            }

            @Override
            public void setCookie(String name, String value, String path, String domain, int maxAge, boolean secure, boolean httpOnly) {

            }

            @Override
            public OutputStream getOutputStream() {
                return null;
            }

            @Override
            public void sendError(int code) {

            }

            @Override
            public void sendError(int code, String message) {

            }

            @Override
            public void end() {

            }
        };
    }

    private Request createHttpRequest(Map<String, List<String>> headers, InputStream requestBody) {
        Map<String, List<String>> queryParameter = new HashMap<>();

        queryParameter.put("a", Arrays.asList("parameter-a"));

        headers.put("b", Arrays.asList("header-b"));

        Map<String, Cookie> cookies = new HashMap<>();

        cookies.put("c", new Cookie("c", "cookie-c", 1, "localhost", "/"));

        return new Request() {

            private InputStream inputStream;

            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public String getURI() {
                return "/app/request-uri";
            }

            @Override
            public String getRelativePath() {
                return "/request-relative-path";
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public String getFirstParam(String param) {
                List<String> values = queryParameter.getOrDefault(param, Collections.emptyList());

                if (!values.isEmpty()) {
                    return values.get(0);
                }

                return null;
            }

            @Override
            public String getQueryParamValue(String param) {
                return getFirstParam(param);
            }

            @Override
            public Cookie getCookie(String cookieName) {
                return cookies.get(cookieName);
            }

            @Override
            public String getHeader(String name) {
                List<String> headers = getHeaders(name);

                if (!headers.isEmpty()) {
                    return headers.get(0);
                }

                return null;
            }

            @Override
            public List<String> getHeaders(String name) {
                return headers.getOrDefault(name, Collections.emptyList());
            }

            @Override
            public InputStream getInputStream() {
                return getInputStream(false);
            }

            @Override
            public InputStream getInputStream(boolean buffer) {
                if (requestBody == null) {
                    return new ByteArrayInputStream(new byte[] {});
                }

                if (inputStream != null) {
                    return inputStream;
                }

                if (buffer) {
                    return inputStream = new BufferedInputStream(requestBody);
                }

                return requestBody;
            }

            @Override
            public String getRemoteAddr() {
                return "user-remote-addr";
            }

            @Override
            public void setError(AuthenticationError error) {

            }

            @Override
            public void setError(LogoutError error) {

            }
        };
    }
}
