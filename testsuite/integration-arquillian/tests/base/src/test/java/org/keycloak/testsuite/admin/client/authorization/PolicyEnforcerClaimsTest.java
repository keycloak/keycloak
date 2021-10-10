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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.common.Profile.Feature.AUTHORIZATION;
import static org.keycloak.common.Profile.Feature.UPLOAD_SCRIPTS;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.security.cert.X509Certificate;

import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.AuthorizationContext;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade.Cookie;
import org.keycloak.adapters.spi.HttpFacade.Request;
import org.keycloak.adapters.spi.HttpFacade.Response;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
@EnableFeature(value = UPLOAD_SCRIPTS, skipRestart = true)
public class PolicyEnforcerClaimsTest extends AbstractKeycloakTest {

    protected static final String REALM_NAME = "authz-test";

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name(REALM_NAME)
                .roles(RolesBuilder.create()
                        .realmRole(RoleBuilder.create().name("uma_authorization").build())
                        .realmRole(RoleBuilder.create().name("uma_protection").build())
                )
                .user(UserBuilder.create().username("marta").password("password")
                        .addRoles("uma_authorization", "uma_protection")
                        .role("resource-server-test", "uma_protection"))
                .user(UserBuilder.create().username("kolo").password("password"))
                .client(ClientBuilder.create().clientId("resource-server-uma-test")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-uma-test")
                        .defaultRoles("uma_protection")
                        .directAccessGrants())
                .client(ClientBuilder.create().clientId("resource-server-test")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-test")
                        .defaultRoles("uma_protection")
                        .directAccessGrants())
                .client(ClientBuilder.create().clientId("public-client-test")
                        .publicClient()
                        .redirectUris("http://localhost:8180/auth/realms/master/app/auth/*", "https://localhost:8543/auth/realms/master/app/auth/*")
                        .directAccessGrants())
                .build());
    }

    @Test
    public void testEnforceUMAAccessWithClaimsUsingBearerToken() {
        initAuthorizationSettings(getClientResource("resource-server-uma-test"));

        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-uma-claims-test.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        HashMap<String, List<String>> headers = new HashMap<>();
        HashMap<String, List<String>> parameters = new HashMap<>();

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        AuthzClient authzClient = getAuthzClient("enforcer-uma-claims-test.json");
        String token = authzClient.obtainAccessToken("marta", "password").getToken();

        headers.put("Authorization", Arrays.asList("Bearer " + token));

        AuthorizationContext context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", "POST", token, headers, parameters));
        assertFalse(context.isGranted());

        AuthorizationRequest request = new AuthorizationRequest();

        request.setTicket(extractTicket(headers));

        AuthorizationResponse response = authzClient.authorization("marta", "password").authorize(request);
        token = response.getToken();

        assertNotNull(token);

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", "POST", token, headers, parameters));
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("200"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", "POST", token, headers, parameters));
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", "POST", token, headers, parameters));
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("10"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", "POST", token, headers, parameters));

        request = new AuthorizationRequest();

        request.setTicket(extractTicket(headers));

        response = authzClient.authorization("marta", "password").authorize(request);
        token = response.getToken();

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", "POST", token, headers, parameters));
        assertTrue(context.isGranted());

        request = new AuthorizationRequest();

        request.setTicket(extractTicket(headers));

        response = authzClient.authorization("marta", "password").authorize(request);
        token = response.getToken();

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", "GET", token, headers, parameters));
        assertTrue(context.isGranted());

        assertEquals(1, context.getPermissions().size());
        Permission permission = context.getPermissions().get(0);

        assertEquals(parameters.get("withdrawal.amount").get(0), permission.getClaims().get("withdrawal.amount").iterator().next());
    }

    @Test
    public void testEnforceEntitlementAccessWithClaimsWithoutBearerToken() {
        initAuthorizationSettings(getClientResource("resource-server-test"));

        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-entitlement-claims-test.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        HashMap<String, List<String>> headers = new HashMap<>();
        HashMap<String, List<String>> parameters = new HashMap<>();

        AuthzClient authzClient = getAuthzClient("enforcer-entitlement-claims-test.json");
        String token = authzClient.obtainAccessToken("marta", "password").getToken();

        AuthorizationContext context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));
        assertTrue(context.isGranted());
        assertEquals(1, context.getPermissions().size());
        Permission permission = context.getPermissions().get(0);
        assertEquals(parameters.get("withdrawal.amount").get(0), permission.getClaims().get("withdrawal.amount").iterator().next());

        parameters.put("withdrawal.amount", Arrays.asList("200"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("10"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));

        assertTrue(context.isGranted());

        assertEquals(1, context.getPermissions().size());
        permission = context.getPermissions().get(0);
        assertEquals(parameters.get("withdrawal.amount").get(0), permission.getClaims().get("withdrawal.amount").iterator().next());
    }

    @Test
    public void testEnforceEntitlementAccessWithClaimsWithBearerToken() {
        initAuthorizationSettings(getClientResource("resource-server-test"));

        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-entitlement-claims-test.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        HashMap<String, List<String>> headers = new HashMap<>();
        HashMap<String, List<String>> parameters = new HashMap<>();

        AuthzClient authzClient = getAuthzClient("enforcer-entitlement-claims-test.json");
        String token = authzClient.obtainAccessToken("marta", "password").getToken();

        headers.put("Authorization", Arrays.asList("Bearer " + token));

        AuthorizationContext context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("200"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("10"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));

        assertTrue(context.isGranted());
    }

    @Test
    public void testEnforceEntitlementAccessWithClaimsWithBearerTokenFromPublicClient() {
        initAuthorizationSettings(getClientResource("resource-server-test"));

        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(getAdapterConfiguration("enforcer-entitlement-claims-test.json"));
        PolicyEnforcer policyEnforcer = deployment.getPolicyEnforcer();
        HashMap<String, List<String>> headers = new HashMap<>();
        HashMap<String, List<String>> parameters = new HashMap<>();

        oauth.realm(REALM_NAME);
        oauth.clientId("public-client-test");
        oauth.doLogin("marta", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        String token = response.getAccessToken();

        headers.put("Authorization", Arrays.asList("Bearer " + token));

        AuthorizationContext context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("200"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));
        assertFalse(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("50"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));
        assertTrue(context.isGranted());

        parameters.put("withdrawal.amount", Arrays.asList("10"));

        context = policyEnforcer.enforce(createHttpFacade("/api/bank/account/1/withdrawal", token, headers, parameters));

        assertTrue(context.isGranted());
    }

    private String extractTicket(HashMap<String, List<String>> headers) {
        List<String> wwwAuthenticateHeader = headers.get("WWW-Authenticate");

        assertNotNull(wwwAuthenticateHeader);
        assertFalse(wwwAuthenticateHeader.isEmpty());

        String wwwAuthenticate = wwwAuthenticateHeader.get(0);
        return wwwAuthenticate.substring(wwwAuthenticate.indexOf("ticket=") + "ticket=\"".length(), wwwAuthenticate.lastIndexOf('"'));
    }

    private void initAuthorizationSettings(ClientResource clientResource) {
        if (clientResource.authorization().resources().findByName("Bank Account").isEmpty()) {
            JSPolicyRepresentation policy = new JSPolicyRepresentation();

            policy.setName("Withdrawal Limit Policy");

            StringBuilder code = new StringBuilder();

            code.append("var context = $evaluation.getContext();");
            code.append("var attributes = context.getAttributes();");
            code.append("var withdrawalAmount = attributes.getValue('withdrawal.amount');");
            code.append("if (withdrawalAmount && withdrawalAmount.asDouble(0) <= 100) {");
            code.append("   $evaluation.grant();");
            code.append("}");

            policy.setCode(code.toString());

            clientResource.authorization().policies().js().create(policy).close();

            createResource(clientResource, "Bank Account", "/api/bank/account/{id}/withdrawal", "withdrawal");

            ScopePermissionRepresentation permission = new ScopePermissionRepresentation();

            permission.setName("Withdrawal Permission");
            permission.addScope("withdrawal");
            permission.addPolicy(policy.getName());

            clientResource.authorization().permissions().scope().create(permission).close();
        }
    }

    private InputStream getAdapterConfiguration(String fileName) {
        try {
            return httpsAwareConfigurationStream(getClass().getResourceAsStream("/authorization-test/" + fileName));
        } catch (IOException e) {
            throw new AssertionError("Could not load keycloak configuration", e);
        }
    }

    private ResourceRepresentation createResource(ClientResource clientResource, String name, String uri, String... scopes) {
        ResourceRepresentation representation = new ResourceRepresentation();

        representation.setName(name);
        representation.setUri(uri);
        representation.setScopes(Arrays.asList(scopes).stream().map(ScopeRepresentation::new).collect(Collectors.toSet()));

        try (javax.ws.rs.core.Response response = clientResource.authorization().resources().create(representation)) {

            representation.setId(response.readEntity(ResourceRepresentation.class).getId());

            return representation;
        }
    }

    private ClientResource getClientResource(String name) {
        ClientsResource clients = realmsResouce().realm(REALM_NAME).clients();
        ClientRepresentation representation = clients.findByClientId(name).get(0);
        return clients.get(representation.getId());
    }

    private OIDCHttpFacade createHttpFacade(String path, String method, String token, Map<String, List<String>> headers, Map<String, List<String>> parameters, InputStream requestBody) {
        return new OIDCHttpFacade() {
            Request request;
            Response response;

            @Override
            public KeycloakSecurityContext getSecurityContext() {
                AccessToken accessToken;
                try {
                    accessToken = new JWSInput(token).readJsonContent(AccessToken.class);
                } catch (JWSInputException cause) {
                    throw new RuntimeException(cause);
                }
                return new KeycloakSecurityContext(token, accessToken, null, null);
            }

            @Override
            public Request getRequest() {
                if (request == null) {
                    request = createHttpRequest(path, method, headers, parameters, requestBody);
                }
                return request;
            }

            @Override
            public Response getResponse() {
                if (response == null) {
                    response = createHttpResponse(headers);
                }
                return response;
            }

            @Override
            public X509Certificate[] getCertificateChain() {
                return new X509Certificate[0];
            }
        };
    }

    private OIDCHttpFacade createHttpFacade(String path, String token, Map<String, List<String>> headers, Map<String, List<String>> parameters) {
        return createHttpFacade(path, null, token, headers, parameters, null);
    }

    private OIDCHttpFacade createHttpFacade(String path, String method, String token, Map<String, List<String>> headers, Map<String, List<String>> parameters) {
        return createHttpFacade(path, method, token, headers, parameters, null);
    }

    private Response createHttpResponse(Map<String, List<String>> headers) {
        return new Response() {

            private int status;

            @Override
            public void setStatus(int status) {
                this.status = status;
            }

            @Override
            public void addHeader(String name, String value) {
                setHeader(name, value);
            }

            @Override
            public void setHeader(String name, String value) {
                headers.put(name, Arrays.asList(value));
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

    private Request createHttpRequest(String path, String method, Map<String, List<String>> headers, Map<String, List<String>> parameters, InputStream requestBody) {
        return new Request() {

            private InputStream inputStream;

            @Override
            public String getMethod() {
                return method == null ? "GET" : method;
            }

            @Override
            public String getURI() {
                return path;
            }

            @Override
            public String getRelativePath() {
                return path;
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public String getFirstParam(String param) {
                List<String> values = parameters.getOrDefault(param, Collections.emptyList());

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
                return null;
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

    protected AuthzClient getAuthzClient(String fileName) {
        return AuthzClient.create(getAdapterConfiguration(fileName));
    }
}
