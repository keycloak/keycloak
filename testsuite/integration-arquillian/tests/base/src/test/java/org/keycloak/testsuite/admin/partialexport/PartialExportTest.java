package org.keycloak.testsuite.admin.partialexport;

import java.util.Arrays;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.AbstractAdminTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.representations.idm.UserRepresentation;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class PartialExportTest extends AbstractAdminTest {

    private static final String EXPORT_TEST_REALM = "partial-export-test";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);

        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/export/partialexport-testrealm.json"), RealmRepresentation.class);
        testRealms.add(realmRepresentation);
    }

    @Test
    public void testExport() {

        // exportGroupsAndRoles == false, exportClients == false
        RealmRepresentation rep = adminClient.realm(EXPORT_TEST_REALM).partialExport(false, false);
        Assert.assertNull("Users are null", rep.getUsers());
        Assert.assertNull("Default groups are empty", rep.getDefaultGroups());
        Assert.assertNull("Groups are empty", rep.getGroups());

        Assert.assertNull("Realm and client roles are empty", rep.getRoles());
        Assert.assertNull("Clients are empty", rep.getClients());

        checkScopeMappings(rep.getScopeMappings(), true);
        Assert.assertNull("Client scope mappings empty", rep.getClientScopeMappings());


        // exportGroupsAndRoles == true, exportClients == false
        rep = adminClient.realm(EXPORT_TEST_REALM).partialExport(true, false);
        Assert.assertNull("Users are null", rep.getUsers());
        Assert.assertNull("Default groups are empty", rep.getDefaultGroups());
        Assert.assertNotNull("Groups not empty", rep.getGroups());
        checkGroups(rep.getGroups());

        Assert.assertNotNull("Realm and client roles not empty", rep.getRoles());
        Assert.assertNotNull("Realm roles not empty", rep.getRoles().getRealm());
        checkRealmRoles(rep.getRoles().getRealm());

        Assert.assertNull("Client roles are empty", rep.getRoles().getClient());
        Assert.assertNull("Clients are empty", rep.getClients());

        checkScopeMappings(rep.getScopeMappings(), true);
        Assert.assertNull("Client scope mappings empty", rep.getClientScopeMappings());


        // exportGroupsAndRoles == false, exportClients == true
        rep = adminClient.realm(EXPORT_TEST_REALM).partialExport(false, true);
        Assert.assertNotNull("The service accout user should be exported", rep.getUsers());
        Assert.assertEquals("Only one client has a service account", 1, rep.getUsers().size());
        checkServiceAccountRoles(rep.getUsers().get(0), false); // export but without roles
        Assert.assertNull("Default groups are empty", rep.getDefaultGroups());
        Assert.assertNull("Groups are empty", rep.getGroups());

        Assert.assertNull("Realm and client roles are empty", rep.getRoles());
        Assert.assertNotNull("Clients not empty", rep.getClients());
        checkClients(rep.getClients());

        checkScopeMappings(rep.getScopeMappings(), false);
        checkClientScopeMappings(rep.getClientScopeMappings());


        // exportGroupsAndRoles == true, exportClients == true
        rep = adminClient.realm(EXPORT_TEST_REALM).partialExport(true, true);
        // service accounts are only exported if roles/groups and clients are asked to be exported
        Assert.assertNotNull("The service accout user should be exported", rep.getUsers());
        Assert.assertEquals("Only one client has a service account", 1, rep.getUsers().size());
        checkServiceAccountRoles(rep.getUsers().get(0), true); // exported with roles
        Assert.assertNull("Default groups are empty", rep.getDefaultGroups());
        Assert.assertNotNull("Groups not empty", rep.getGroups());
        checkGroups(rep.getGroups());


        Assert.assertNotNull("Realm and client roles not empty", rep.getRoles());
        Assert.assertNotNull("Realm roles not empty", rep.getRoles().getRealm());
        checkRealmRoles(rep.getRoles().getRealm());

        Assert.assertNotNull("Client roles not empty", rep.getRoles().getClient());
        checkClientRoles(rep.getRoles().getClient());

        Assert.assertNotNull("Clients not empty", rep.getClients());
        checkClients(rep.getClients());

        checkScopeMappings(rep.getScopeMappings(), false);
        checkClientScopeMappings(rep.getClientScopeMappings());


        // check that secrets are masked
        checkSecretsAreMasked(rep);
    }

    private void checkServiceAccountRoles(UserRepresentation serviceAccount, boolean rolesExpected) {
        Assert.assertTrue("User is a service account", serviceAccount.getUsername().startsWith(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX));
        Assert.assertNull("Password should be null", serviceAccount.getCredentials());
        if (rolesExpected) {
            List<String> realmRoles = serviceAccount.getRealmRoles();
            Assert.assertThat("Realm roles are OK", realmRoles, Matchers.containsInAnyOrder("uma_authorization", "user", "offline_access"));

            Map<String, List<String>> clientRoles = serviceAccount.getClientRoles();
            Assert.assertNotNull("Client roles are exported", clientRoles);
            Assert.assertThat("Client roles for test-app-service-account are OK", clientRoles.get("test-app-service-account"),
                    Matchers.containsInAnyOrder("test-app-service-account", "test-app-service-account-parent"));
            Assert.assertThat("Client roles for account are OK", clientRoles.get("account"),
                    Matchers.containsInAnyOrder("manage-account", "view-profile"));
        } else {
            Assert.assertNull("Service account should be exported without realm roles", serviceAccount.getRealmRoles());
            Assert.assertNull("Service account should be exported without client roles", serviceAccount.getClientRoles());
        }
    }

    private void checkSecretsAreMasked(RealmRepresentation rep) {

        // Client secret
        for (ClientRepresentation client: rep.getClients()) {
            if (Boolean.FALSE.equals(client.isPublicClient()) && Boolean.FALSE.equals(client.isBearerOnly())) {
                Assert.assertEquals("Client secret masked", ComponentRepresentation.SECRET_VALUE, client.getSecret());
            }
        }

        // IdentityProvider clientSecret
        for (IdentityProviderRepresentation idp: rep.getIdentityProviders()) {
            Assert.assertEquals("IdentityProvider clientSecret masked", ComponentRepresentation.SECRET_VALUE, idp.getConfig().get("clientSecret"));
        }

        // smtpServer password
        Assert.assertEquals("SMTP password masked", ComponentRepresentation.SECRET_VALUE, rep.getSmtpServer().get("password"));

        // components rsa KeyProvider privateKey
        MultivaluedHashMap<String, ComponentExportRepresentation> components = rep.getComponents();

        List<ComponentExportRepresentation> keys = components.get("org.keycloak.keys.KeyProvider");
        Assert.assertNotNull("Keys not null", keys);
        Assert.assertTrue("At least one key returned", keys.size() > 0);
        boolean found = false;
        for (ComponentExportRepresentation component: keys) {
            if ("rsa".equals(component.getProviderId())) {
                Assert.assertEquals("RSA KeyProvider privateKey masked", ComponentRepresentation.SECRET_VALUE, component.getConfig().getFirst("privateKey"));
                found = true;
            }
        }
        Assert.assertTrue("Found rsa private key", found);

        // components ldap UserStorageProvider bindCredential
        List<ComponentExportRepresentation> userStorage = components.get("org.keycloak.storage.UserStorageProvider");
        Assert.assertNotNull("UserStorageProvider not null", userStorage);
        Assert.assertTrue("At least one UserStorageProvider returned", userStorage.size() > 0);
        found = false;
        for (ComponentExportRepresentation component: userStorage) {
            if ("ldap".equals(component.getProviderId())) {
                Assert.assertEquals("LDAP provider bindCredential masked", ComponentRepresentation.SECRET_VALUE, component.getConfig().getFirst("bindCredential"));
                found = true;
            }
        }
        Assert.assertTrue("Found ldap bindCredential", found);
    }

    private void checkClientScopeMappings(Map<String, List<ScopeMappingRepresentation>> mappings) {
        Map<String, Set<String>> map = extractScopeMappings(mappings.get("test-app"));
        Set<String> set = map.get("test-app-scope");
        Assert.assertTrue("Client test-app / test-app-scope contains customer-admin-composite-role", set.contains("customer-admin-composite-role"));

        set = map.get("third-party");
        Assert.assertTrue("Client test-app / third-party contains customer-user", set.contains("customer-user"));

        map = extractScopeMappings(mappings.get("test-app-scope"));
        set = map.get("test-app-scope");
        Assert.assertTrue("Client test-app-scope / test-app-scope contains test-app-allowed-by-scope", set.contains("test-app-allowed-by-scope"));
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
        Assert.assertTrue("Client test-app contains user", set.contains("user"));

        set = map.get("test-app-scope");
        Assert.assertTrue("Client test-app contains user", set.contains("user"));
        Assert.assertTrue("Client test-app contains admin", set.contains("admin"));

        set = map.get("third-party");
        Assert.assertTrue("Client test-app contains third-party", set.contains("user"));
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
        Assert.assertTrue("Client role customer-admin for test-app", roles.containsKey("customer-admin"));
        Assert.assertTrue("Client role sample-client-role for test-app", roles.containsKey("sample-client-role"));
        Assert.assertTrue("Client role customer-user for test-app", roles.containsKey("customer-user"));

        Assert.assertTrue("Client role customer-admin-composite-role for test-app", roles.containsKey("customer-admin-composite-role"));
        RoleRepresentation.Composites cmp = roles.get("customer-admin-composite-role").getComposites();
        Assert.assertTrue("customer-admin-composite-role / realm / customer-user-premium", cmp.getRealm().contains("customer-user-premium"));
        Assert.assertTrue("customer-admin-composite-role / client['test-app'] / customer-admin", cmp.getClient().get("test-app").contains("customer-admin"));

        roles = collectRoles(clientRoles.get("test-app-scope"));
        Assert.assertTrue("Client role test-app-disallowed-by-scope for test-app-scope", roles.containsKey("test-app-disallowed-by-scope"));
        Assert.assertTrue("Client role test-app-allowed-by-scope for test-app-scope", roles.containsKey("test-app-allowed-by-scope"));

        roles = collectRoles(clientRoles.get("test-app-service-account"));
        Assert.assertThat("Client roles are OK for test-app-service-account", roles.keySet(),
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
        Assert.assertTrue("Client test-app", set.contains("test-app"));
        Assert.assertTrue("Client test-app-scope", set.contains("test-app-scope"));
        Assert.assertTrue("Client third-party", set.contains("third-party"));
    }

    private void checkRealmRoles(List<RoleRepresentation> realmRoles) {
        Set<String> set = new HashSet<>();
        for (RoleRepresentation r: realmRoles) {
            set.add(r.getName());
        }
        Assert.assertTrue("Role sample-realm-role", set.contains("sample-realm-role"));
        Assert.assertTrue("Role realm-composite-role", set.contains("realm-composite-role"));
        Assert.assertTrue("Role customer-user-premium", set.contains("customer-user-premium"));
        Assert.assertTrue("Role admin", set.contains("admin"));
        Assert.assertTrue("Role user", set.contains("user"));
    }

    private void checkGroups(List<GroupRepresentation> groups) {
        HashSet<String> set = new HashSet<>();
        for (GroupRepresentation g: groups) {
            compileGroups(set, g);
        }
        Assert.assertTrue("Group /roleRichGroup", set.contains("/roleRichGroup"));
        Assert.assertTrue("Group /roleRichGroup/level2group", set.contains("/roleRichGroup/level2group"));
        Assert.assertTrue("Group /topGroup", set.contains("/topGroup"));
        Assert.assertTrue("Group /topGroup/level2group", set.contains("/topGroup/level2group"));
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
        Assert.assertTrue("Default role 'uma_authorization'", roles.contains("uma_authorization"));
        Assert.assertTrue("Default role 'offline_access'", roles.contains("offline_access"));
        Assert.assertTrue("Default role 'user'", roles.contains("user"));
    }
}
