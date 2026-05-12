package org.keycloak.protocol.oidc.utils;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.services.resteasy.HttpRequestImpl;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;
import org.keycloak.storage.client.UnsupportedOperationsClientStorageAdapter;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for ClientHostUtils methods validating client_session_host against client URLs.
 *
 * @author Keycloak Security Team
 */
public class ClientHostUtilsTest {

    private static KeycloakSession session;

    @BeforeAll
    public static void beforeAll() {
        HttpRequest httpRequest = new HttpRequestImpl(MockHttpRequest.create("GET", URI.create("https://keycloak.org/"), URI.create("https://keycloak.org")));
        Profile.defaults();
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        ResteasyKeycloakSessionFactory sessionFactory = new ResteasyKeycloakSessionFactory();
        sessionFactory.init();
        session = new ResteasyKeycloakSession(sessionFactory);
        session.getContext().setHttpRequest(httpRequest);
    }

    @Test
    public void testHostMatchesRedirectUri() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of(
                "https://app.example.com/callback",
                "https://app2.example.com/auth"
        ).collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("app2.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("evil.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("other.example.com", client, session));
    }

    @Test
    public void testHostMatchesRedirectUriWithPort() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of(
                "https://app.example.com:8443/callback"
        ).collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("app.example.com:8443", client, session));
    }

    @Test
    public void testHostMatchesRootUrl() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRootUrl("https://root.example.com");

        assertTrue(ClientHostUtils.isHostAllowedForClient("root.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("other.example.com", client, session));
    }

    @Test
    public void testHostMatchesManagementUrl() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("https://admin.example.com/management");

        assertTrue(ClientHostUtils.isHostAllowedForClient("admin.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("evil.com", client, session));
    }

    @Test
    public void testHostMatchesBaseUrl() {
        TestClientModel client = new TestClientModel("test-client");
        client.setBaseUrl("https://base.example.com");

        assertTrue(ClientHostUtils.isHostAllowedForClient("base.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("evil.com", client, session));
    }

    @Test
    public void testHostMatchesAnyConfiguredUrl() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of("https://app.example.com/callback").collect(Collectors.toSet()));
        client.setRootUrl("https://root.example.com");
        client.setManagementUrl("https://admin.example.com/mgmt");
        client.setBaseUrl("https://base.example.com");

        assertTrue(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("root.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("admin.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("base.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("evil.com", client, session));
    }

    @Test
    public void testWildcardRedirectUri() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of("*").collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("any.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("evil.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("localhost", client, session));
    }

    @Test
    public void testNullAndEmptyHosts() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of("https://app.example.com/callback").collect(Collectors.toSet()));

        assertFalse(ClientHostUtils.isHostAllowedForClient(null, client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("   ", client, session));
    }

    @Test
    public void testNullClient() {
        assertFalse(ClientHostUtils.isHostAllowedForClient("any.example.com", null, session));
    }

    @Test
    public void testNoUrlsConfigured() {
        TestClientModel client = new TestClientModel("test-client");

        assertFalse(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("localhost", client, session));
    }

    @Test
    public void testLocalhostHosts() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of(
                "http://localhost:8080/callback",
                "http://127.0.0.1:8080/callback"
        ).collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("localhost", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("localhost:8080", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("127.0.0.1", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("127.0.0.1:8080", client, session));
    }

    @Test
    public void testRelativeRedirectUris() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of("/callback").collect(Collectors.toSet()));
        client.setRootUrl("https://root.example.com");

        assertTrue(ClientHostUtils.isHostAllowedForClient("root.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("evil.com", client, session));
    }

    @Test
    public void testSubdomainsDontMatch() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of("https://app.example.com/callback").collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("sub.app.example.com", client, session));
    }

    @Test
    public void testHttpsAndHttpDifferentSchemes() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of(
                "https://app.example.com/callback",
                "http://dev.example.com/callback"
        ).collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("dev.example.com", client, session));
    }

    @Test
    public void testSSRFAttackPrevention() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of("https://public.example.com/callback").collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("public.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("evil.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("internal.company.local", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("192.168.1.1", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("169.254.169.254", client, session));
    }

    @Test
    public void testIPv6WithBracketsAndPort() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of(
                "http://[::1]:8080/callback",
                "http://[2001:db8::1]:8443/callback"
        ).collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("[::1]:8080", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[2001:db8::1]:8443", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[::1]", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[2001:db8::1]", client, session));
    }

    @Test
    public void testIPv6WithBracketsNoPort() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of(
                "http://[::1]/callback",
                "http://[fe80::1]/auth"
        ).collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("[::1]", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[fe80::1]", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("fe80::1", client, session));
    }

    @Test
    public void testIPv6BareAddress() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of(
                "http://[2001:db8:85a3::8a2e:370:7334]/callback"
        ).collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("[2001:db8:85a3::8a2e:370:7334]", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("2001:db8:85a3::8a2e:370:7334", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[2001:db8:85a3::8a2e:370:7334]:443", client, session));
    }

    @Test
    public void testIPv6LinkLocal() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of(
                "http://[fe80::a00:27ff:fe4e:66a1]:3000/callback"
        ).collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("[fe80::a00:27ff:fe4e:66a1]:3000", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[fe80::a00:27ff:fe4e:66a1]", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("fe80::a00:27ff:fe4e:66a1", client, session));
    }

    @Test
    public void testIPv6DoesNotMatchDifferentAddress() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of(
                "http://[::1]/callback"
        ).collect(Collectors.toSet()));

        assertFalse(ClientHostUtils.isHostAllowedForClient("::1", client, session)); // brackets required
        assertFalse(ClientHostUtils.isHostAllowedForClient("[2001:db8::1]", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("2001:db8::1", client, session));
    }

    @Test
    public void testMixedIPv4AndIPv6() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of(
                "http://127.0.0.1:8080/callback",
                "http://[::1]:8080/callback",
                "https://localhost:8443/auth"
        ).collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("127.0.0.1", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("127.0.0.1:8080", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("::1", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[::1]", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[::1]:8080", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("localhost", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("localhost:443", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("localhost:8443", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("192.168.1.1", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("[fe80::1]", client, session));
    }

    @Test
    public void testHostWithOnlyRootUrlNoRedirectUris() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRootUrl("https://app.example.com");
        // No redirect URIs set

        assertTrue(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("evil.com", client, session));
    }

    @Test
    public void testCaseInsensitiveMatching() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of("https://App.Example.COM/callback").collect(Collectors.toSet()));

        assertTrue(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("APP.EXAMPLE.COM", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("App.Example.Com", client, session));
    }

    @Test
    public void testIPv6InRootUrl() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRootUrl("http://[::1]:8080");

        assertFalse(ClientHostUtils.isHostAllowedForClient("::1", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[::1]", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[::1]:8080", client, session));
    }

    @Test
    public void testIPv6InManagementUrl() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("http://[2001:db8::1]:9990/management");

        assertTrue(ClientHostUtils.isHostAllowedForClient("[2001:db8::1]", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[2001:db8::1]:9990", client, session));
    }

    /**
     * Minimal test implementation of ClientModel for testing purposes.
     * Only implements methods needed for ClientHostUtils validation.
     */
    private static class TestClientModel extends UnsupportedOperationsClientStorageAdapter {
        private final String clientId;
        private Set<String> redirectUris = Set.of();
        private String rootUrl;
        private String managementUrl;
        private String baseUrl;

        public TestClientModel(String clientId) {
            this.clientId = clientId;
        }

        @Override
        public void updateClient() {
        }

        @Override
        public String getId() {
            return "";
        }

        @Override
        public String getClientId() {
            return clientId;
        }

        @Override
        public void setClientId(String clientId) { }

        @Override
        public String getName() { return ""; }

        @Override
        public void setName(String name) { }

        @Override
        public String getDescription() { return ""; }

        @Override
        public void setDescription(String description) { }

        @Override
        public boolean isEnabled() { return false; }

        @Override
        public void setEnabled(boolean enabled) { }

        @Override
        public boolean isAlwaysDisplayInConsole() { return false; }

        @Override
        public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) { }

        @Override
        public boolean isSurrogateAuthRequired() { return false; }

        @Override
        public void setSurrogateAuthRequired(boolean surrogateAuthRequired) { }

        @Override
        public Set<String> getWebOrigins() { return null; }

        @Override
        public void setWebOrigins(Set<String> webOrigins) { }

        @Override
        public void addWebOrigin(String webOrigin) { }

        @Override
        public void removeWebOrigin(String webOrigin) { }

        @Override
        public Set<String> getRedirectUris() { return redirectUris; }

        public void setRedirectUris(Set<String> redirectUris) {
            this.redirectUris = redirectUris;
        }

        @Override
        public void addRedirectUri(String redirectUri) { }

        @Override
        public void removeRedirectUri(String redirectUri) { }

        @Override
        public String getManagementUrl() {
            return managementUrl;
        }

        public void setRootUrl(String rootUrl) {
            this.rootUrl = rootUrl;
        }

        @Override
        public String getBaseUrl() {
            return baseUrl;
        }

        public void setManagementUrl(String managementUrl) {
            this.managementUrl = managementUrl;
        }

        @Override
        public String getRootUrl() {
            return rootUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        @Override
        public boolean isBearerOnly() {
            return false;
        }

        @Override
        public void setBearerOnly(boolean only) { }

        @Override
        public int getNodeReRegistrationTimeout() { return 0; }

        @Override
        public void setNodeReRegistrationTimeout(int timeout) { }

        @Override
        public String getClientAuthenticatorType() { return ""; }

        @Override
        public void setClientAuthenticatorType(String clientAuthenticatorType) { }

        @Override
        public boolean validateSecret(String secret) { return false; }

        @Override
        public String getSecret() {
            return "";
        }

        @Override
        public void setSecret(String secret) { }

        @Override
        public String getRegistrationToken() {
            return "";
        }

        @Override
        public void setRegistrationToken(String registrationToken) { }

        @Override
        public String getProtocol() {
            return "";
        }

        @Override
        public void setProtocol(String protocol) { }

        @Override
        public void setAttribute(String name, String value) { }

        @Override
        public void removeAttribute(String name) { }

        @Override
        public String getAttribute(String name) {
            return "";
        }

        @Override
        public Map<String, String> getAttributes() {
            return Map.of();
        }

        @Override
        public String getAuthenticationFlowBindingOverride(String binding) {
            return "";
        }

        @Override
        public Map<String, String> getAuthenticationFlowBindingOverrides() {
            return Map.of();
        }

        @Override
        public void removeAuthenticationFlowBindingOverride(String binding) { }

        @Override
        public void setAuthenticationFlowBindingOverride(String binding, String flowId) { }

        @Override
        public boolean isFrontchannelLogout() { return false; }

        @Override
        public void setFrontchannelLogout(boolean flag) { }

        @Override
        public boolean isFullScopeAllowed() { return false; }

        @Override
        public void setFullScopeAllowed(boolean value) { }

        @Override
        public boolean isPublicClient() { return false; }

        @Override
        public void setPublicClient(boolean flag) { }

        @Override
        public boolean isConsentRequired() { return false; }

        @Override
        public void setConsentRequired(boolean consentRequired) { }

        @Override
        public boolean isStandardFlowEnabled() { return false; }

        @Override
        public void setStandardFlowEnabled(boolean standardFlowEnabled) { }

        @Override
        public boolean isImplicitFlowEnabled() { return false; }

        @Override
        public void setImplicitFlowEnabled(boolean implicitFlowEnabled) { }

        @Override
        public boolean isDirectAccessGrantsEnabled() { return false; }

        @Override
        public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) { }

        @Override
        public boolean isServiceAccountsEnabled() { return false; }

        @Override
        public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) { }

        @Override
        public RealmModel getRealm() { return null; }

        @Override
        public void addClientScope(ClientScopeModel clientScope, boolean defaultScope) { }

        @Override
        public void addClientScopes(Set<ClientScopeModel> clientScopes, boolean defaultScope) { }

        @Override
        public void removeClientScope(ClientScopeModel clientScope) { }

        @Override
        public Map<String, ClientScopeModel> getClientScopes(boolean defaultScope) {
            return Map.of();
        }

        @Override
        public int getNotBefore() { return 0; }

        @Override
        public void setNotBefore(int notBefore) { }

        @Override
        public Map<String, Integer> getRegisteredNodes() { return null; }

        @Override
        public void registerNode(String nodeHost, int registrationTime) { }

        @Override
        public void unregisterNode(String nodeHost) { }

        @Override
        public Stream<ProtocolMapperModel> getProtocolMappersStream() {
            return Stream.empty();
        }

        @Override
        public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
            return null;
        }

        @Override
        public void removeProtocolMapper(ProtocolMapperModel mapping) { }

        @Override
        public void updateProtocolMapper(ProtocolMapperModel mapping) { }

        @Override
        public ProtocolMapperModel getProtocolMapperById(String id) {
            return null;
        }

        @Override
        public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
            return null;
        }

        @Override
        public Stream<RoleModel> getScopeMappingsStream() {
            return Stream.empty();
        }

        @Override
        public Stream<RoleModel> getRealmScopeMappingsStream() {
            return Stream.empty();
        }

        @Override
        public void addScopeMapping(RoleModel role) { }

        @Override
        public void deleteScopeMapping(RoleModel role) { }

        @Override
        public boolean hasScope(RoleModel role) { return false; }
    }
}
