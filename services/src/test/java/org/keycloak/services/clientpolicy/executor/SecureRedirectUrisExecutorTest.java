package org.keycloak.services.clientpolicy.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUrisExecutor.Configuration;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUrisExecutor.UriValidation;
import org.keycloak.util.JsonSerialization;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisExecutorFactory.ALLOW_HTTP;
import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisExecutorFactory.ALLOW_LOOPBACK_INTERFACE;
import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisExecutorFactory.ALLOW_OPEN;
import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisExecutorFactory.ALLOW_PRIVATE_USE_SCHEMA;
import static org.keycloak.services.clientpolicy.executor.SecureRedirectUrisExecutorFactory.ALLOW_WILDCARD_CONTEXT_PATH;

@RunWith(Enclosed.class)
public class SecureRedirectUrisExecutorTest {

    public static class ExecutorTest {

        private static SecureRedirectUrisExecutor executor;
        private ObjectNode configuration;

        @BeforeClass
        public static void setupAll() {
            executor = new SecureRedirectUrisExecutor();
        }

        @Before
        public void setup() {
            configuration = JsonSerialization.createObjectNode();
        }

        @Test
        public void defaultConfiguration() {
            setupConfiguration(configuration);
            Configuration configuration = executor.getConfiguration();

            assertTrue(configuration.isAllowLoopbackInterface());
            assertTrue(configuration.isAllowWildcardContextPath());

            assertFalse(configuration.isAllowPrivateUseSchema());
            assertFalse(configuration.isAllowHttp());
            assertFalse(configuration.isAllowOpen());

            assertTrue(configuration.getPermittedDomains().isEmpty());
        }

        @Test
        public void error_uriSyntax() {
            assertThrows("Invalid Redirect Uri: invalid uri syntax", ClientPolicyException.class, () ->
                    doValidate("https://keycloak.org\n"));
        }

        @Test
        public void error_allOpen() {
            enable(SecureRedirectUrisExecutorFactory.ALLOW_OPEN);
            System.setProperty("kc.profile", "prod");

            assertThrows("Invalid Redirect Uri: allow open redirect uris only in dev mode",
                    ClientPolicyException.class, () -> doValidate("https://keycloak.org"));

            System.clearProperty("kc.profile");
        }

        @Test
        public void error_privateUseSchema() {
            assertThrows("Invalid Redirect Uri: not allowed private use schema",
                    ClientPolicyException.class, () -> doValidate("myapp://oauth.redirect"));
        }

        @Test
        public void error_loopbackInterface() {
            disable(ALLOW_LOOPBACK_INTERFACE);

            assertThrows("Invalid Redirect Uri: not allowed loopback interface",
                    ClientPolicyException.class, () -> doValidate("http://127.0.0.1:8080/auth/admin"));
        }

        @Test
        public void error_http() {
            assertThrows("Invalid Redirect Uri: not allowed HTTP",
                    ClientPolicyException.class, () -> doValidate("http://example.com/auth/callback"));
        }

        @Test
        public void error_wildcardContextPath() {
            disable(ALLOW_WILDCARD_CONTEXT_PATH);

            assertThrows("Invalid Redirect Uri: not allowed wildcard context path",
                    ClientPolicyException.class, () -> doValidate("https://example.com/*/endpoint"));
        }

        @Test
        public void error_permittedDomains() {
            permittedDomains("permitted-domains.org", "block-domains.org");

            assertThrows("Invalid Redirect Uri: not allowed domain",
                    ClientPolicyException.class, () -> doValidate("https://example.com/"));
        }

        @Test
        public void success_allOpen() throws ClientPolicyException {
            enable(SecureRedirectUrisExecutorFactory.ALLOW_OPEN);
            System.setProperty("kc.profile", "dev");

            doValidate("http://any-domains.org/auth/realms/master/broker/oidc/endpoint");

            System.clearProperty("kc.profile");
        }

        @Test
        public void success_defaultConfiguration() {
            List<String> redirectUris = Arrays.asList(
                    "https://example.org/*/realms/master",
                    "/*/realms/master",
                    "/realms/master/account/*"
            );

            for (String s : redirectUris) {
                try {
                    doValidate(s);
                } catch (ClientPolicyException e) {
                    assertNull(e.getErrorDetail() + " " + s, e);
                }
            }
        }

        @Test
        public void success_allConfigurationEnabled() {
            // except ALLOW_OPEN
            enable(
                    ALLOW_WILDCARD_CONTEXT_PATH,
                    ALLOW_PRIVATE_USE_SCHEMA,
                    ALLOW_LOOPBACK_INTERFACE,
                    ALLOW_HTTP
            );

            permittedDomains("oauth.redirect", "((dev|test)-)*example.org");

            List<String> redirectUris = Arrays.asList(
                    "http://127.0.0.1:8080/*/realms/master",
                    "myapp://oauth.redirect",
                    "https://test-example.org/auth/admin"
            );

            for (String s : redirectUris) {
                try {
                    doValidate(s);
                } catch (ClientPolicyException e) {
                    assertNull(e.getErrorDetail() + " " + s, e);
                }
            }
        }

        @Test
        public void success_allConfigurationDisabled() {
            disable(
                    ALLOW_WILDCARD_CONTEXT_PATH,
                    ALLOW_PRIVATE_USE_SCHEMA,
                    ALLOW_LOOPBACK_INTERFACE,
                    ALLOW_HTTP,
                    ALLOW_OPEN
            );

            List<String> redirectUris = Arrays.asList(
                    "https://keycloak.org/sso/silent-callback.html",
                    "https://example.org/auth/realms/master/broker/oidc/endpoint"
            );

            for (String s : redirectUris) {
                try {
                    doValidate(s);
                } catch (ClientPolicyException e) {
                    assertNull(e.getErrorDetail() + " " + s, e);
                }
            }
        }

        private void doValidate(String redirectUri) throws ClientPolicyException {
            setupConfiguration(configuration);
            executor.validate(redirectUri);
        }

        private void permittedDomains(String... domains) {
            ArrayNode arrayNode = JsonSerialization.mapper.createArrayNode();
            Arrays.stream(domains).forEach(arrayNode::add);
            configuration.set(SecureRedirectUrisExecutorFactory.PERMITTED_DOMAINS, arrayNode);
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

    }

    @RunWith(Enclosed.class)
    public static class UriValidationTest {

        public static class NormalTest {

            @Test
            public void create() throws URISyntaxException {
                new UriValidation("https://keycloak.org");
            }

            @Test(expected = URISyntaxException.class)
            public void create_syntaxError() throws URISyntaxException {
                new UriValidation("https://keycloak.org\n");
            }

            @Test
            public void matchDomains() throws URISyntaxException {
                // no domains
                UriValidation validation = new UriValidation("http://localhost:8080/auth/realms/master/account");
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
                    validation = new UriValidation(match);
                    assertTrue(match, validation.matchDomain(givenPattern));
                }

                String[] expectNoneMatches = new String[]{
                        "https://prod-example.org",
                        "https://testexample.org"
                };

                for (String match : expectNoneMatches) {
                    validation = new UriValidation(match);
                    assertFalse(match, validation.matchDomain(givenPattern));
                }
            }
        }

        @RunWith(Parameterized.class)
        public static class ParameterizedTest {

            @Parameters(name = "{index}: {0}")
            public static Collection<Object[]> parameters() {
                return Arrays.asList(new Object[][]{
                        /* uri, expectedPrivateUseSchema, expectedLoopbackInterface, expectedWildcardContextPath, expectedHttp */
                        {"https://keycloak.org", false, false, false, false},
                        {"http://localhost:8080/auth/", false, true, false, true},
                        {"/auth/realms/master/account", false, true, false, false},
                        {"/auth/realms/master/*", false, true, false, false},

                        // wildcard context path
                        {"*/endpoint", false, true, true, false},
                        {"http://localhost:8080/*/master", false, true, true, true},
                        {"https://keycloak.org/*", false, false, true, false},

                        // private use schema
                        {"myapp://oauth.redirect", true, false, false, false},
                        {"myapp://oauth.redirect/*/home", true, false, true, false},

                        // loopback
                        {"http://127.0.0.1:8080/auth/", false, true, false, true},
                        {"http://[::1]:8899/admin", false, true, false, true},
                });
            }

            public ParameterizedTest(
                    String uri,
                    boolean expectedPrivateUseSchema,
                    boolean expectedLoopbackInterface,
                    boolean expectedWildcardContextPath,
                    boolean expectedHttp
            ) throws URISyntaxException {
                this.validation = new UriValidation(uri);
                this.expectedPrivateUseSchema = expectedPrivateUseSchema;
                this.expectedLoopbackInterface = expectedLoopbackInterface;
                this.expectedWildcardContextPath = expectedWildcardContextPath;
                this.expectedHttp = expectedHttp;
            }

            @Test
            public void isPrivateUseSchema() {
                assertEquals(expectedPrivateUseSchema, validation.isPrivateUseSchema());
            }

            @Test
            public void isLoopbackInterface() {
                assertEquals(expectedLoopbackInterface, validation.isLoopbackInterface());
            }

            @Test
            public void isWildcardContextPath() {
                assertEquals(expectedWildcardContextPath, validation.isWildcardContextPath());
            }

            @Test
            public void isHttp() {
                assertEquals(expectedHttp, validation.isHttp());
            }

            private final UriValidation validation;
            private final boolean expectedPrivateUseSchema;
            private final boolean expectedLoopbackInterface;
            private final boolean expectedWildcardContextPath;
            private final boolean expectedHttp;
        }
    }
}
