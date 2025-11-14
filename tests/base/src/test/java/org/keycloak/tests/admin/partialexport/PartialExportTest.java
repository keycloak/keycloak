package org.keycloak.tests.admin.partialexport;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.utils.Assert;
import org.keycloak.util.JsonSerialization;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest
public class PartialExportTest {

    private static final String EXPORT_TEST_REALM = "partial-export-test";

    @InjectAdminClient
    private Keycloak adminClient;

    @BeforeEach
    public void initializeRealm() {
        RealmRepresentation realmRepresentation = loadJson(PartialExportTest.class.getResourceAsStream("partialexport-testrealm.json"), RealmRepresentation.class);
        adminClient.realms().create(realmRepresentation);
    }

    @AfterEach
    public void cleanupRealm() {
        adminClient.realms().realm(EXPORT_TEST_REALM).remove();
    }

    @Test
    public void testExport() {
        // exportGroupsAndRoles == false, exportClients == false
        RealmRepresentation rep = adminClient.realm(EXPORT_TEST_REALM).partialExport(false, false);
        Assert.assertNull(rep.getUsers(), "Users are null");
        Assert.assertNull(rep.getDefaultGroups(), "Default groups are empty");
        Assert.assertNull(rep.getGroups(), "Groups are empty");

        Assert.assertNull(rep.getRoles(), "Realm and client roles are empty");
        Assert.assertNull(rep.getClients(), "Clients are empty");

        checkScopeMappings(rep.getScopeMappings(), true);
        Assert.assertNull(rep.getClientScopeMappings(), "Client scope mappings empty");


        // exportGroupsAndRoles == true, exportClients == false
        rep = adminClient.realm(EXPORT_TEST_REALM).partialExport(true, false);
        Assert.assertNull(rep.getUsers(), "Users are null");
        Assert.assertNull(rep.getDefaultGroups(), "Default groups are empty");
        Assert.assertNotNull(rep.getGroups(), "Groups not empty");
        checkGroups(rep.getGroups());

        Assert.assertNotNull(rep.getRoles(), "Realm and client roles not empty");
        Assert.assertNotNull(rep.getRoles().getRealm(), "Realm roles not empty");
        checkRealmRoles(rep.getRoles().getRealm());

        Assert.assertNull(rep.getRoles().getClient(), "Client roles are empty");
        Assert.assertNull(rep.getClients(), "Clients are empty");

        checkScopeMappings(rep.getScopeMappings(), true);
        Assert.assertNull(rep.getClientScopeMappings(), "Client scope mappings empty");


        // exportGroupsAndRoles == false, exportClients == true
        rep = adminClient.realm(EXPORT_TEST_REALM).partialExport(false, true);
        Assert.assertNotNull(rep.getUsers(), "The service account user should be exported");
        Assert.assertEquals(1, rep.getUsers().size(), "Only one client has a service account");
        checkServiceAccountRoles(rep.getUsers().get(0), false); // export but without roles
        Assert.assertNull(rep.getDefaultGroups(), "Default groups are empty");
        Assert.assertNull(rep.getGroups(), "Groups are empty");

        Assert.assertNull(rep.getRoles(), "Realm and client roles are empty");
        Assert.assertNotNull(rep.getClients(), "Clients not empty");
        checkClients(rep.getClients());

        checkScopeMappings(rep.getScopeMappings(), false);
        checkClientScopeMappings(rep.getClientScopeMappings());


        // exportGroupsAndRoles == true, exportClients == true
        rep = adminClient.realm(EXPORT_TEST_REALM).partialExport(true, true);
        // service accounts are only exported if roles/groups and clients are asked to be exported
        Assert.assertNotNull(rep.getUsers(), "The service accout user should be exported");
        Assert.assertEquals(1, rep.getUsers().size(), "Only one client has a service account");
        checkServiceAccountRoles(rep.getUsers().get(0), true); // exported with roles
        Assert.assertNull(rep.getDefaultGroups(), "Default groups are empty");
        Assert.assertNotNull(rep.getGroups(), "Groups not empty");
        checkGroups(rep.getGroups());


        Assert.assertNotNull(rep.getRoles(), "Realm and client roles not empty");
        Assert.assertNotNull(rep.getRoles().getRealm());
        Assert.assertNotNull(rep.getRoles().getRealm());
        checkRealmRoles(rep.getRoles().getRealm());

        Assert.assertNotNull(rep.getRoles().getClient(), "Client roles not empty");
        checkClientRoles(rep.getRoles().getClient());

        Assert.assertNotNull(rep.getClients(), "Clients not empty");
        checkClients(rep.getClients());

        checkScopeMappings(rep.getScopeMappings(), false);
        checkClientScopeMappings(rep.getClientScopeMappings());


        // check that secrets are masked
        checkSecretsAreMasked(rep);
    }

    private void checkServiceAccountRoles(UserRepresentation serviceAccount, boolean rolesExpected) {
        Assert.assertTrue(serviceAccount.getUsername().startsWith(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX), "User is a service account");
        Assert.assertNull(serviceAccount.getCredentials(), "Password should be null");
        if (rolesExpected) {
            List<String> realmRoles = serviceAccount.getRealmRoles();
            assertThat("Realm roles are OK", realmRoles, Matchers.containsInAnyOrder("uma_authorization", "user", "offline_access"));

            Map<String, List<String>> clientRoles = serviceAccount.getClientRoles();
            Assert.assertNotNull(clientRoles, "Client roles are exported");
            assertThat("Client roles for test-app-service-account are OK", clientRoles.get("test-app-service-account"),
                    Matchers.containsInAnyOrder("test-app-service-account", "test-app-service-account-parent"));
            assertThat("Client roles for account are OK", clientRoles.get("account"),
                    Matchers.containsInAnyOrder("manage-account", "view-profile"));
        } else {
            Assert.assertNull(serviceAccount.getRealmRoles(), "Service account should be exported without realm roles");
            Assert.assertNull(serviceAccount.getClientRoles(), "Service account should be exported without client roles");
        }
    }

    private void checkSecretsAreMasked(RealmRepresentation rep) {

        // Client secret
        for (ClientRepresentation client: rep.getClients()) {
            if (Boolean.FALSE.equals(client.isPublicClient()) && Boolean.FALSE.equals(client.isBearerOnly())) {
                Assert.assertEquals(ComponentRepresentation.SECRET_VALUE, client.getSecret(), "Client secret masked");
                String rotatedSecret = Optional.ofNullable(client.getAttributes())
                        .flatMap(attrs -> Optional.ofNullable(attrs.get(ClientSecretConstants.CLIENT_ROTATED_SECRET)))
                        .orElse(ComponentRepresentation.SECRET_VALUE);
                Assert.assertEquals(ComponentRepresentation.SECRET_VALUE, rotatedSecret, "Rotated client secret masked");
            }
        }

        // IdentityProvider clientSecret
        for (IdentityProviderRepresentation idp: rep.getIdentityProviders()) {
            Assert.assertEquals(ComponentRepresentation.SECRET_VALUE, idp.getConfig().get("clientSecret"), "IdentityProvider clientSecret masked");
        }

        // smtpServer password
        Assert.assertEquals(ComponentRepresentation.SECRET_VALUE, rep.getSmtpServer().get("password"), "SMTP password masked");

        // components rsa KeyProvider privateKey
        MultivaluedHashMap<String, ComponentExportRepresentation> components = rep.getComponents();

        List<ComponentExportRepresentation> keys = components.get("org.keycloak.keys.KeyProvider");
        Assert.assertNotNull(keys, "Keys not null");
        Assert.assertTrue(keys.size() > 0, "At least one key returned");
        boolean found = false;
        for (ComponentExportRepresentation component: keys) {
            if ("rsa".equals(component.getProviderId())) {
                Assert.assertEquals(ComponentRepresentation.SECRET_VALUE, component.getConfig().getFirst("privateKey"), "RSA KeyProvider privateKey masked");
                found = true;
            }
        }
        Assert.assertTrue(found, "Found rsa private key");

        // components ldap UserStorageProvider bindCredential
        List<ComponentExportRepresentation> userStorage = components.get("org.keycloak.storage.UserStorageProvider");
        Assert.assertNotNull(userStorage, "UserStorageProvider not null");
        Assert.assertTrue(userStorage.size() > 0, "At least one UserStorageProvider returned");
        found = false;
        for (ComponentExportRepresentation component: userStorage) {
            if ("ldap".equals(component.getProviderId())) {
                Assert.assertEquals(ComponentRepresentation.SECRET_VALUE, component.getConfig().getFirst("bindCredential"), "LDAP provider bindCredential masked");
                found = true;
            }
        }
        Assert.assertTrue(found, "Found ldap bindCredential");
    }

    private void checkClientScopeMappings(Map<String, List<ScopeMappingRepresentation>> mappings) {
        Map<String, Set<String>> map = extractScopeMappings(mappings.get("test-app"));
        Set<String> set = map.get("test-app-scope");
        Assert.assertTrue(set.contains("customer-admin-composite-role"), "Client test-app / test-app-scope contains customer-admin-composite-role");

        set = map.get("third-party");
        Assert.assertTrue(set.contains("customer-user"), "Client test-app / third-party contains customer-user");

        map = extractScopeMappings(mappings.get("test-app-scope"));
        set = map.get("test-app-scope");
        Assert.assertTrue(set.contains("test-app-allowed-by-scope"), "Client test-app-scope / test-app-scope contains test-app-allowed-by-scope");
    }

    private void checkScopeMappings(List<ScopeMappingRepresentation> scopeMappings, boolean expectOnlyOfflineAccess) {
        ScopeMappingRepresentation offlineAccessScope = scopeMappings.stream().filter((ScopeMappingRepresentation rep) -> {

            return "offline_access".equals(rep.getClientScope());
        }).findFirst().get();
        Assert.assertTrue(offlineAccessScope.getRoles().contains("offline_access"));

        if (expectOnlyOfflineAccess) {
            Assert.assertEquals(1, scopeMappings.size());
            return;
        }

        Map<String, Set<String>> map = extractScopeMappings(scopeMappings);

        Set<String> set = map.get("test-app");
        Assert.assertTrue(set.contains("user"), "Client test-app contains user");

        set = map.get("test-app-scope");
        Assert.assertTrue(set.contains("user"), "Client test-app contains user");
        Assert.assertTrue(set.contains("admin"), "Client test-app contains admin");

        set = map.get("third-party");
        Assert.assertTrue(set.contains("user"), "Client test-app contains third-party");
    }

    private Map<String, Set<String>> extractScopeMappings(List<ScopeMappingRepresentation> scopeMappings) {
        Map<String, Set<String>> map = new HashMap<>();
        for (ScopeMappingRepresentation r: scopeMappings) {
            map.put(r.getClient(), r.getRoles());
        }
        return map;
    }

    private void checkClientRoles(Map<String, List<RoleRepresentation>> clientRoles) {
        Map<String, RoleRepresentation> roles = collectRoles(clientRoles.get("test-app"));
        Assert.assertTrue(roles.containsKey("customer-admin"), "Client role customer-admin for test-app");
        Assert.assertTrue(roles.containsKey("sample-client-role"), "Client role sample-client-role for test-app");
        Assert.assertTrue(roles.containsKey("customer-user"), "Client role customer-user for test-app");

        Assert.assertTrue(roles.containsKey("customer-admin-composite-role"), "Client role customer-admin-composite-role for test-app");
        RoleRepresentation.Composites cmp = roles.get("customer-admin-composite-role").getComposites();
        Assert.assertTrue(cmp.getRealm().contains("customer-user-premium"), "customer-admin-composite-role / realm / customer-user-premium");
        Assert.assertTrue(cmp.getClient().get("test-app").contains("customer-admin"), "customer-admin-composite-role / client['test-app'] / customer-admin");

        roles = collectRoles(clientRoles.get("test-app-scope"));
        Assert.assertTrue(roles.containsKey("test-app-disallowed-by-scope"), "Client role test-app-disallowed-by-scope for test-app-scope");
        Assert.assertTrue(roles.containsKey("test-app-allowed-by-scope"), "Client role test-app-allowed-by-scope for test-app-scope");

        roles = collectRoles(clientRoles.get("test-app-service-account"));
        assertThat("Client roles are OK for test-app-service-account", roles.keySet(),
                Matchers.containsInAnyOrder("test-app-service-account", "test-app-service-account-parent", "test-app-service-account-child"));
    }

    private Map<String, RoleRepresentation> collectRoles(List<RoleRepresentation> roles) {
        HashMap<String, RoleRepresentation> map = new HashMap<>();
        if (roles == null) {
            return map;
        }
        for (RoleRepresentation r: roles) {
            map.put(r.getName(), r);
        }
        return map;
    }

    private void checkClients(List<ClientRepresentation> clients) {
        HashSet<String> set = new HashSet<>();
        for (ClientRepresentation c: clients) {
            set.add(c.getClientId());
        }
        Assert.assertTrue(set.contains("test-app"), "Client test-app");
        Assert.assertTrue(set.contains("test-app-scope"), "Client test-app-scope");
        Assert.assertTrue(set.contains("third-party"), "Client third-party");
    }

    private void checkRealmRoles(List<RoleRepresentation> realmRoles) {
        Set<String> set = new HashSet<>();
        for (RoleRepresentation r: realmRoles) {
            set.add(r.getName());
        }
        Assert.assertTrue(set.contains("sample-realm-role"), "Role sample-realm-role");
        Assert.assertTrue(set.contains("realm-composite-role"), "Role realm-composite-role");
        Assert.assertTrue(set.contains("customer-user-premium"), "Role customer-user-premium");
        Assert.assertTrue(set.contains("admin"), "Role admin");
        Assert.assertTrue(set.contains("user"), "Role user");
    }

    private void checkGroups(List<GroupRepresentation> groups) {
        HashSet<String> set = new HashSet<>();
        for (GroupRepresentation g: groups) {
            compileGroups(set, g);
        }
        Assert.assertTrue(set.contains("/roleRichGroup"), "Group /roleRichGroup");
        Assert.assertTrue(set.contains("/roleRichGroup/level2group"), "Group /roleRichGroup/level2group");
        Assert.assertTrue(set.contains("/topGroup"), "Group /topGroup");
        Assert.assertTrue(set.contains("/topGroup/level2group"), "Group /topGroup/level2group");
    }

    private void compileGroups(Set<String> found, GroupRepresentation g) {
        found.add(g.getPath());
        if (g.getSubGroups() != null) {
            for (GroupRepresentation s: g.getSubGroups()) {
                compileGroups(found, s);
            }
        }
    }
    private void checkDefaultRoles(List<String> defaultRoles) {
        HashSet<String> roles = new HashSet<>(defaultRoles);
        Assert.assertTrue(roles.contains("uma_authorization"), "Default role 'uma_authorization'");
        Assert.assertTrue(roles.contains("offline_access"), "Default role 'offline_access'");
        Assert.assertTrue(roles.contains("user"), "Default role 'user'");
    }

    private static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }
}
