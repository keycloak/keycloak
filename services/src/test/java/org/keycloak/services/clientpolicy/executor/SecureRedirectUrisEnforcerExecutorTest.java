/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.executor;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.keycloak.OAuthErrorException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutor.Configuration;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutor.UriValidation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory.ALLOW_HTTP_SCHEME;
import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory.ALLOW_IPV4_LOOPBACK_ADDRESS;
import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory.ALLOW_IPV6_LOOPBACK_ADDRESS;
import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory.ALLOW_OPEN_REDIRECT;
import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory.ALLOW_PERMITTED_DOMAINS;
import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory.ALLOW_PRIVATE_USE_URI_SCHEME;
import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory.ALLOW_WILDCARD_CONTEXT_PATH;
import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory.OAUTH_2_1_COMPLIANT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SecureRedirectUrisEnforcerExecutorTest {

    private static SecureRedirectUrisEnforcerExecutor executor;
    private ObjectNode configuration;

    @BeforeClass
    public static void setupAll() {
        executor = new SecureRedirectUrisEnforcerExecutor(null);
    }

    @Before
    public void setup() {
        configuration = JsonSerialization.createObjectNode();
    }

    @Test
    public void defaultConfiguration() {
        setupConfiguration(configuration);
        Configuration configuration = executor.getConfiguration();

        assertFalse(configuration.isAllowIPv4LoopbackAddress());
        assertFalse(configuration.isAllowIPv6LoopbackAddress());
        assertFalse(configuration.isAllowPrivateUseUriScheme());
        assertFalse(configuration.isAllowHttpScheme());
        assertFalse(configuration.isAllowWildcardContextPath());
        assertFalse(configuration.isOAuth2_1Compliant());
        assertFalse(configuration.isAllowOpenRedirect());

        assertTrue(configuration.getAllowPermittedDomains().isEmpty());
    }

    @Test
    public void failUriSyntax() {
        checkFail("https://keycloak.org\n" ,false, SecureRedirectUrisEnforcerExecutor.ERR_GENERAL);
        checkFail("Collins'&1=1;--" ,false, SecureRedirectUrisEnforcerExecutor.ERR_GENERAL);
    }

    @Test
    public void failValidatePrivateUseUriScheme() {
        // default config
        checkFail("myapp:/oauth.redirect", false, SecureRedirectUrisEnforcerExecutor.ERR_PRIVATESCHEME);

        // allow private use uri scheme
        enable(ALLOW_PRIVATE_USE_URI_SCHEME);
        checkFail("myapp.example.com:/*", false, SecureRedirectUrisEnforcerExecutor.ERR_PRIVATESCHEME);

        // allow wildcard context path
        enable(ALLOW_WILDCARD_CONTEXT_PATH);
        checkFail("myapp.example.com:/*/abc/*/efg", false, SecureRedirectUrisEnforcerExecutor.ERR_PRIVATESCHEME);

        // OAuth 2.1 compliant
        enable(OAUTH_2_1_COMPLIANT);
        Stream.of(
                "myapp:/oauth.redirect#pinpoint",
                "myapp.example.com:/oauth.redirect/*",
                "myapp:/oauth.redirect"
        ).forEach(i->checkFail(i, false, SecureRedirectUrisEnforcerExecutor.ERR_PRIVATESCHEME));
    }

    @Test
    public void successValidatePrivateUseUriScheme() {
        // allow private use uri scheme
        enable(ALLOW_PRIVATE_USE_URI_SCHEME);
        Stream.of(
                "com.example.app:/oauth2redirect/example-provider",
                "com.example.app:51004/oauth2redirect/example-provider"
        ).forEach(i->checkSuccess(i, false));

        // allow wildcard context path
        enable(ALLOW_WILDCARD_CONTEXT_PATH);
        checkSuccess("myapp.example.com:/*", false);

        // OAuth 2.1 compliant
        enable(OAUTH_2_1_COMPLIANT);
        checkSuccess("com.example.app:/oauth2redirect/example-provider", false);
    }

    @Test
    public void failValidateIPv4LoopbackAddress() {
        // default config
        checkFail("https://127.0.0.1/auth/admin", false, SecureRedirectUrisEnforcerExecutor.ERR_LOOPBACK);

        // allow IPv4 loopback address
        enable(ALLOW_IPV4_LOOPBACK_ADDRESS);
        Stream.of(
                "http://127.0.0.1:8080/auth/admin",
                "https://127.0.0.1:8080/*"
        ).forEach(i->checkFail(i, false, SecureRedirectUrisEnforcerExecutor.ERR_LOOPBACK));

        // allow wildcard context path
        enable(ALLOW_WILDCARD_CONTEXT_PATH);
        checkFail("https://127.0.0.1:8080/*/efg/*/abc", false, SecureRedirectUrisEnforcerExecutor.ERR_LOOPBACK);

        // allow http scheme
        enable(ALLOW_HTTP_SCHEME);

        // OAuth 2.1 compliant
        enable(OAUTH_2_1_COMPLIANT);
        Stream.of(
                "http://127.0.0.1:8080/auth/admin",
                "http://127.0.0.1/*",
                "http://127.0.0.1/auth/admin#fragment"
        ).forEach(i->checkFail(i, false, SecureRedirectUrisEnforcerExecutor.ERR_LOOPBACK));
        // authorization code request redirect_uri parameter
        Stream.of(
                "http://127.0.0.1:123456/oauth2redirect/example-provider",
                "http://127.0.0.1/oauth2redirect/example-provider"
        ).forEach(i->checkFail(i, true, SecureRedirectUrisEnforcerExecutor.ERR_LOOPBACK));
    }

    @Test
    public void successValiIPv4LoopbackAddress() {
        // allow IPv4 loopback address
        enable(ALLOW_IPV4_LOOPBACK_ADDRESS);
        Stream.of(
                "https://127.0.0.1:8443",
                "https://localhost/auth/admin"
        ).forEach(i->checkSuccess(i, false));

        // allow wildcard context path
        enable(ALLOW_WILDCARD_CONTEXT_PATH);
        checkSuccess("https://localhost/*", false);

        // allow http scheme
        enable(ALLOW_HTTP_SCHEME);
        checkSuccess("http://127.0.0.1:8080/oauth2redirect", false);

        // OAuth 2.1 compliant
        enable(OAUTH_2_1_COMPLIANT);
        checkSuccess("http://127.0.0.1/oauth2redirect/example-provider", false);
        checkSuccess("http://127.0.0.1:43321/oauth2redirect/example-provider", true);
    }

    @Test
    public void failValidateIPv6LoopbackAddress() {
        // default config
        checkFail("https://[::1]:9999/oauth2redirect/example-provider", false, SecureRedirectUrisEnforcerExecutor.ERR_LOOPBACK);

        // allow IPv6 loopback address
        enable(ALLOW_IPV6_LOOPBACK_ADDRESS);
        checkFail("http://[::1]:9999/oauth2redirect/example-provider", false, SecureRedirectUrisEnforcerExecutor.ERR_LOOPBACK);
        checkFail("https://[::1]:9999/*", false, SecureRedirectUrisEnforcerExecutor.ERR_LOOPBACK);

        // allow wildcard context path
        enable(ALLOW_WILDCARD_CONTEXT_PATH);
        checkFail("https://[::1]/*/efg/*/abc", false, SecureRedirectUrisEnforcerExecutor.ERR_LOOPBACK);

        // allow http scheme
        enable(ALLOW_HTTP_SCHEME);

        // OAuth 2.1 compliant
        enable(OAUTH_2_1_COMPLIANT);
        Stream.of(
                "http://[::1]:8080/auth/admin",
                "http://[::1]/*",
                "http://[::1]/auth/admin#fragment",
                "https://[0:0:0:0:0:0:0:1]:8080"
        ).forEach(i->checkFail(i, false, SecureRedirectUrisEnforcerExecutor.ERR_LOOPBACK));
        // authorization code request redirect_uri parameter
        Stream.of(
                "http://[::1]:123456/oauth2redirect/example-provider",
                "http://[::1]/oauth2redirect/example-provider"
        ).forEach(i->checkFail(i, true, SecureRedirectUrisEnforcerExecutor.ERR_LOOPBACK));
    }

    @Test
    public void successValiIPv6LoopbackAddress() {
        // allow IPv6 loopback address
        enable(ALLOW_IPV6_LOOPBACK_ADDRESS);
        for (String i : Arrays.asList(
                "https://[::1]/oauth2redirect/example-provider",
                "https://[::1]:8443"
        )) {
            checkSuccess(i, false);
        }

        // allow wildcard context path
        enable(ALLOW_WILDCARD_CONTEXT_PATH);
        checkSuccess("https://[::1]/*", false);

        // allow http scheme
        enable(ALLOW_HTTP_SCHEME);
        checkSuccess("http://[::1]:8080/oauth2redirect", false);

        // OAuth 2.1 compliant
        enable(OAUTH_2_1_COMPLIANT);
        checkSuccess("http://[::1]/oauth2redirect/example-provider", false);
        checkSuccess("http://[::1]:43321/oauth2redirect/example-provider", true);
    }

    @Test
    public void failNormalUri() {
        // default config
        checkFail("http://192.168.1.211:9088/oauth2redirect/example-provider/test", false, SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI);

        // allow wildcard context path
        enable(ALLOW_WILDCARD_CONTEXT_PATH);
        checkFail("https://192.168.1.211/*/efg/*/abc", false, SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI);

        // allow http scheme
        enable(ALLOW_HTTP_SCHEME);

        // allow permitted domains
        permittedDomains("oauth.redirect", "((dev|test)-)*example.org");
        checkFail("http://192.168.1.211/oauth2redirect/example-provider/test", false, SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI);

        // OAuth 2.1 compliant
        enable(OAUTH_2_1_COMPLIANT);
        Stream.of(
                "http://dev-example.com/auth/callback",
                "https://test-example.com:8443/*",
                "https://oauth.redirect:8443/auth/callback#fragment"
        ).forEach(i->checkFail(i, true, SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI));
    }

    @Test
    public void successNormalUri() {
        // default config
        Stream.of(
                "https://example.org/realms/master",
                "https://192.168.1.211:9088/oauth2redirect/example-provider/test",
                "https://192.168.1.211/"
        ).forEach(i->checkSuccess(i, false));

        // allow wildcard context path
        enable(ALLOW_WILDCARD_CONTEXT_PATH);
        checkSuccess("https://example.org/*", false);

        // allow http scheme
        enable(ALLOW_HTTP_SCHEME);

        // allow permitted domains
        permittedDomains("oauth.redirect", "((dev|test)-)*example.org");
        checkSuccess("http://test-example.org:8080/*", false);

        // OAuth 2.1 compliant
        enable(OAUTH_2_1_COMPLIANT);
        checkSuccess("https://dev-example.org:8080/redirect", false);
    }

    @Test
    public void successDefaultConfiguration() {
        Stream.of(
                "https://example.org/realms/master",
                "https://192.168.1.211:9088/oauth2redirect/example-provider/test",
                "https://192.168.1.211/"
        ).forEach(i->checkSuccess(i, false));
    }

    @Test
    public void successAllConfigurationEnabled() {
        // except ALLOW_OPEN_REDIRECT
        enable(
                ALLOW_WILDCARD_CONTEXT_PATH,
                ALLOW_IPV4_LOOPBACK_ADDRESS,
                ALLOW_IPV6_LOOPBACK_ADDRESS,
                ALLOW_HTTP_SCHEME,
                ALLOW_PRIVATE_USE_URI_SCHEME
        );

        permittedDomains("oauth.redirect", "((dev|test)-)*example.org");

        Stream.of(
                "http://127.0.0.1/*/realms/master",
                "http://[::1]/*/realms/master",
                "myapp.example.com://oauth.redirect",
                "https://test-example.org/*/auth/admin"
        ).forEach(i->checkSuccess(i, false));
    }

    @Test
    public void successAllConfigurationDisabled() {
        disable(
                ALLOW_WILDCARD_CONTEXT_PATH,
                ALLOW_IPV4_LOOPBACK_ADDRESS,
                ALLOW_IPV6_LOOPBACK_ADDRESS,
                ALLOW_HTTP_SCHEME,
                ALLOW_PRIVATE_USE_URI_SCHEME,
                ALLOW_OPEN_REDIRECT
        );

        Stream.of(
                "https://keycloak.org/sso/silent-callback.html",
                "https://example.org/auth/realms/master/broker/oidc/endpoint",
                "https://192.168.8.1:12345/auth/realms/master/broker/oidc/endpoint"
        ).forEach(i->checkSuccess(i, false));
    }

    private void permittedDomains(String... domains) {
        ArrayNode arrayNode = JsonSerialization.mapper.createArrayNode();
        Arrays.stream(domains).forEach(arrayNode::add);
        configuration.set(ALLOW_PERMITTED_DOMAINS, arrayNode);
    }

    private void disable(String... config) {
        Arrays.stream(config).forEach(it -> configuration.set(it, BooleanNode.getFalse()));
    }

    private void enable(String... config) {
        Arrays.stream(config).forEach(it -> configuration.set(it, BooleanNode.getTrue()));
    }

    private void setupConfiguration(JsonNode node) {
        Configuration configuration = JsonSerialization.mapper.convertValue(node, executor.getExecutorConfigurationClass());
        executor.setupConfiguration(configuration);
    }

    private void checkFail(String redirectUri, boolean isRedirectUriParameter, String errorDetail) {
        try {
            doValidate(redirectUri, isRedirectUriParameter);
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, cpe.getMessage());
            assertEquals(errorDetail, cpe.getErrorDetail());
        }
    }

    private void checkSuccess(String redirectUri, boolean isRedirectUriParameter) {
        try {
            doValidate(redirectUri, isRedirectUriParameter);
        } catch (ClientPolicyException e) {
            assertNull(e.getErrorDetail(), e);
        }
    }

    private void doValidate(String redirectUri, boolean isRedirectUriParameter) throws ClientPolicyException {
        setupConfiguration(configuration);
        executor.verifyRedirectUri(redirectUri, isRedirectUriParameter);
    }

    @Test
    public void matchDomains() throws URISyntaxException {
        SecureRedirectUrisEnforcerExecutor.Configuration config = new SecureRedirectUrisEnforcerExecutor.Configuration();

        // no domains
        UriValidation validation = new UriValidation("http://localhost:8080/auth/realms/master/account", false, config);
        boolean matches = validation.matchDomains(Collections.emptyList());
        assertFalse(matches);

        // 1 domain not match
        matches = validation.matchDomains(Collections.singletonList("local-\\w+"));
        assertFalse(matches);

        // 1 domain match
        matches = validation.matchDomains(Collections.singletonList("localhost"));
        assertTrue(matches);

        matches = validation.matchDomains(Collections.singletonList("local\\w+"));
        assertTrue(matches);

        // 2 domains not match
        matches = validation.matchDomains(Arrays.asList(
                "local-\\w+",
                "localhost2"
        ));
        assertFalse(matches);

        // 2 domain match
        matches = validation.matchDomains(Arrays.asList(
                "local\\w+",
                "localhost"
        ));
        assertTrue(matches);

        // 3 more cases
        String givenPattern = "((dev|test)-)*example.org";
        String[] expectMatches = new String[]{
                "https://dev-example.org",
                "https://test-example.org",
                "https://example.org",
        };

        for (String match : expectMatches) {
            validation = new UriValidation(match, false, config);
            assertTrue(match, validation.matchDomain(givenPattern));
        }

        String[] expectNoneMatches = new String[]{
                "https://prod-example.org",
                "https://testexample.org"
        };

        for (String match : expectNoneMatches) {
            validation = new UriValidation(match, false, config);
            assertFalse(match, validation.matchDomain(givenPattern));
        }
    }

}
