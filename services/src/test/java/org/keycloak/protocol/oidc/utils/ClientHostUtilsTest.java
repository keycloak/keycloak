package org.keycloak.protocol.oidc.utils;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
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
 * Note: when registering cluster nodes in admin client, some symbols '/', ':' etc. are not allowed.
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
    public void testHostMatchesManagedNodesOnly() {
        TestClientModel client = new TestClientModel("test-client");
        client.registerNode("app.example.com", (int) Instant.now().getEpochSecond());
        client.registerNode("app2.example.com", (int) Instant.now().getEpochSecond());
        assertTrue(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("app2.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("evil.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("other.example.com", client, session));
    }

    @Test
    public void testHostMatchesManagementUrlOnly() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of("https://app.example.com/callback").collect(Collectors.toSet()));
        client.setManagementUrl("https://admin.example.com/management");
        assertTrue(ClientHostUtils.isHostAllowedForClient("admin.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
    }

    @Test
    public void testHostMatchesAnyManagedNodeOrManagementUrl() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of("https://app3.example.com/callback").collect(Collectors.toSet()));
        client.registerNode("app.example.com", (int) Instant.now().getEpochSecond());
        client.registerNode("app2.example.com", (int) Instant.now().getEpochSecond());
        client.setManagementUrl("https://admin.example.com/mgmt");
        assertTrue(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("app2.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("admin.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("app3.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("evil.com", client, session));
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
    public void testLocalhostHostsInManagementUrl() {
        TestClientModel client = new TestClientModel("test-client");
        client.registerNode("127.0.0.1", (int) Instant.now().getEpochSecond());
        client.registerNode("app2.example.com", (int) Instant.now().getEpochSecond());
        client.setManagementUrl("http://localhost:8080/mgmt");
        assertTrue(ClientHostUtils.isHostAllowedForClient("localhost", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("localhost:8080", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("127.0.0.1", client, session));
    }

    @Test
    public void testSSRFAttackPrevention() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("https://public.example.com/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("public.example.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("evil.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("internal.company.local", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("192.168.1.1", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("169.254.169.254", client, session));
    }

    @Test
    public void testIPv6WithBracketsAndPort() {
        TestClientModel client = new TestClientModel("test-client");

        client.setManagementUrl( "http://[::1]:8080/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("[::1]:8080", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[::1]:8080", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[::1]", client, session));
        client.setManagementUrl("http://[2001:db8::1]:8443/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("[2001:db8::1]:8443", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[2001:db8::1]", client, session));
    }

    @Test
    public void testIPv6WithBracketsNoPort() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("http://[::1]/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("[::1]", client, session));
        client.setManagementUrl("http://[fe80::1]/auth");
        assertTrue(ClientHostUtils.isHostAllowedForClient("[fe80::1]", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("fe80::1", client, session));
    }

    @Test
    public void testIPv6BareAddress() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("http://[2001:db8:85a3::8a2e:370:7334]/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("[2001:db8:85a3::8a2e:370:7334]", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("2001:db8:85a3::8a2e:370:7334", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[2001:db8:85a3::8a2e:370:7334]:443", client, session));
    }

    @Test
    public void testIPv6LinkLocal() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("http://[fe80::a00:27ff:fe4e:66a1]:3000/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("[fe80::a00:27ff:fe4e:66a1]:3000", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("[fe80::a00:27ff:fe4e:66a1]", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("fe80::a00:27ff:fe4e:66a1", client, session));
    }

    @Test
    public void testIPv6DoesNotMatchDifferentAddress() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("http://[::1]/callback");
        assertFalse(ClientHostUtils.isHostAllowedForClient("::1", client, session)); // brackets required
        assertFalse(ClientHostUtils.isHostAllowedForClient("[2001:db8::1]", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("2001:db8::1", client, session));
    }

    @Test
    public void testCaseInsensitiveMatching() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("https://App.Example.COM/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("APP.EXAMPLE.COM", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("App.Example.Com", client, session));
    }

    @Test
    public void testMalformedURL() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("https:///App.Example.COM/callback");
        assertFalse(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
    }

    @Test
    public void testDuplicatedManagedNodes() {
        TestClientModel client = new TestClientModel("test-client");
        client.registerNode("Node1", (int) Instant.now().getEpochSecond());
        client.registerNode("node1", (int) Instant.now().getEpochSecond());
        client.registerNode("node2", (int) Instant.now().getEpochSecond());
        client.setManagementUrl("https://app.example.com/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("node1", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("node2", client, session));
    }

    @Test
    public void testNoManagedNodesAndNoManagementURLSet() {
        TestClientModel client = new TestClientModel("test-client");
        client.setRedirectUris(Stream.of("https://app3.example.com/callback").collect(Collectors.toSet()));
        assertFalse(ClientHostUtils.isHostAllowedForClient("app3", client, session));
    }

    @Test
    public void testIPv4WithPort() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("http://192.168.1.100:8080/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("192.168.1.100:8080", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("192.168.1.100", client, session));
        client.setManagementUrl("https://10.0.0.5:443/mgmt");
        assertTrue(ClientHostUtils.isHostAllowedForClient("10.0.0.5:443", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("10.0.0.5", client, session));
    }

    @Test
    public void testIPv4WithoutPort() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("http://192.168.1.100/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("192.168.1.100", client, session));
        client.setManagementUrl("http://172.16.0.1/auth");
        assertTrue(ClientHostUtils.isHostAllowedForClient("172.16.0.1", client, session));
    }

    @Test
    public void testIPv4PrivateRanges() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("http://10.0.0.1:8080/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("10.0.0.1", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("10.0.0.1:8080", client, session));
        client.setManagementUrl("http://172.16.0.1/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("172.16.0.1", client, session));
        client.setManagementUrl("http://192.168.1.1/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("192.168.1.1", client, session));
    }

    @Test
    public void testIPv4PublicAddresses() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("http://8.8.8.8:53/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("8.8.8.8", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("8.8.8.8:53", client, session));
        client.setManagementUrl("https://1.1.1.1/auth");
        assertTrue(ClientHostUtils.isHostAllowedForClient("1.1.1.1", client, session));
    }

    @Test
    public void testIPv4DoesNotMatchDifferentAddress() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("http://192.168.1.100/callback");
        assertFalse(ClientHostUtils.isHostAllowedForClient("192.168.1.101", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("192.168.2.100", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("10.0.0.1", client, session));
    }

    @Test
    public void testIPv4InManagedNodes() {
        TestClientModel client = new TestClientModel("test-client");
        client.registerNode("192.168.1.100", (int) Instant.now().getEpochSecond());
        client.registerNode("10.0.0.5", (int) Instant.now().getEpochSecond());
        assertTrue(ClientHostUtils.isHostAllowedForClient("192.168.1.100", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("10.0.0.5", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("192.168.1.101", client, session));
    }

    @Test
    public void testIPv4WithNonStandardPorts() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("http://192.168.1.100:3000/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("192.168.1.100:3000", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("192.168.1.100", client, session));
        client.setManagementUrl("http://10.0.0.1:9090/mgmt");
        assertTrue(ClientHostUtils.isHostAllowedForClient("10.0.0.1:9090", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("10.0.0.1", client, session));
    }

    @Test
    public void testIPv4LoopbackAddresses() {
        TestClientModel client = new TestClientModel("test-client");
        client.setManagementUrl("http://127.0.0.2:8080/callback");
        assertTrue(ClientHostUtils.isHostAllowedForClient("127.0.0.2", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("127.0.0.2:8080", client, session));
    }

    @Test
    public void testIPv4MixedWithHostnames() {
        TestClientModel client = new TestClientModel("test-client");
        client.registerNode("app.example.com", (int) Instant.now().getEpochSecond());
        client.registerNode("192.168.1.100", (int) Instant.now().getEpochSecond());
        client.setManagementUrl("http://10.0.0.5:8080/mgmt");
        assertTrue(ClientHostUtils.isHostAllowedForClient("app.example.com", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("192.168.1.100", client, session));
        assertTrue(ClientHostUtils.isHostAllowedForClient("10.0.0.5", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("evil.com", client, session));
        assertFalse(ClientHostUtils.isHostAllowedForClient("192.168.1.101", client, session));
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
        protected Map<String, Integer> managedNodes = new HashMap<String, Integer>();
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
        public Map<String, Integer> getRegisteredNodes() {
            return managedNodes;
        }

        @Override
        public void registerNode(String nodeHost, int registrationTime) {
            managedNodes.put(nodeHost, registrationTime);
        }

        @Override
        public void unregisterNode(String nodeHost) {
            managedNodes.remove(nodeHost);
        }

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
