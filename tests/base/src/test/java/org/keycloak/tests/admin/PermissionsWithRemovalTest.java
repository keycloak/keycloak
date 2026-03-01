package org.keycloak.tests.admin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.representations.idm.TestLdapConnectionRepresentation;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.tests.utils.Assert;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class PermissionsWithRemovalTest extends AbstractPermissionsTest {

    @BeforeEach
    @Override
    public void beforeEach() {
        recreatePermissionRealm();

        RealmConfig realm2Config = new PermissionsTestRealmConfig2();
        RealmRepresentation realm2 = realm2Config.configure(RealmConfigBuilder.create()).build();
        adminClient.realms().create(realm2);

        super.beforeEach();
    }

    public void recreatePermissionRealm() {
        RealmConfig realm1Config = new PermissionsTestRealmConfig1();
        RealmRepresentation realm1 = realm1Config.configure(RealmConfigBuilder.create()).build();
        adminClient.realms().create(realm1);
    }

    @AfterEach
    public void afterEach() {
        adminClient.realms().realm(REALM_2_NAME).remove();
        adminClient.realms().realm(REALM_NAME).remove();
    }

    @Test
    public void realms() throws Exception {
        // Check returned realms
        invoke((RealmResource realm) -> clients.get("master-none").realms().findAll(), clients.get("none"), false);
        invoke((RealmResource realm) -> clients.get("none").realms().findAll(), clients.get("none"), false);
        Assert.assertNames(clients.get("master-admin").realms().findAll(), "master", REALM_NAME, "realm2");
        Assert.assertNames(clients.get(AdminRoles.REALM_ADMIN).realms().findAll(), REALM_NAME);
        Assert.assertNames(clients.get("REALM2").realms().findAll(), "realm2");

        // Check realm only contains name if missing view realm permission
        List<RealmRepresentation> realms = clients.get(AdminRoles.VIEW_USERS).realms().findAll();
        Assert.assertNames(realms, REALM_NAME);
        assertGettersEmpty(realms.get(0));

        realms = clients.get(AdminRoles.VIEW_REALM).realms().findAll();
        Assert.assertNames(realms, REALM_NAME);
        assertNotNull(realms.get(0).getAccessTokenLifespan());

        // Check the same when access with users from 'master' realm
        realms = clients.get("master-" + AdminRoles.VIEW_USERS).realms().findAll();
        Assert.assertNames(realms, REALM_NAME);
        assertGettersEmpty(realms.get(0));

        realms = clients.get("master-" + AdminRoles.VIEW_REALM).realms().findAll();
        Assert.assertNames(realms, REALM_NAME);
        assertNotNull(realms.get(0).getAccessTokenLifespan());

        // Create realm
        invoke(realm -> clients.get("master-admin").realms().create(RealmConfigBuilder.create().name("master").build()),
                adminClient, true);
        invoke(realm -> clients.get("master-" + AdminRoles.MANAGE_USERS).realms().create(RealmConfigBuilder.create().name("master").build()),
                adminClient, false);
        invoke(realm -> clients.get(AdminRoles.REALM_ADMIN).realms().create(RealmConfigBuilder.create().name("master").build()),
                adminClient, false);

        // Get realm
        invoke(RealmResource::toRepresentation, AdminAuth.Resource.REALM, false, true);

        RealmRepresentation realm = clients.get(AdminRoles.QUERY_REALMS).realm(REALM_NAME).toRepresentation();
        assertGettersEmpty(realm);
        assertNull(realm.isRegistrationEmailAsUsername());
        assertNull(realm.getAttributes());

        realm = clients.get(AdminRoles.VIEW_USERS).realm(REALM_NAME).toRepresentation();
        assertNotNull(realm.isRegistrationEmailAsUsername());

        realm = clients.get(AdminRoles.MANAGE_USERS).realm(REALM_NAME).toRepresentation();
        assertNotNull(realm.isRegistrationEmailAsUsername());

        // query users only if granted through fine-grained admin
        realm = clients.get(AdminRoles.QUERY_USERS).realm(REALM_NAME).toRepresentation();
        assertNull(realm.isRegistrationEmailAsUsername());
        assertNull(realm.getAttributes());

        // this should pass given that users granted with "query" roles are allowed to access the realm with limited access
        for (String role : AdminRoles.ALL_QUERY_ROLES) {
            invoke(realmm -> clients.get(role).realms().realm(REALM_NAME).toRepresentation(), clients.get(role), true);
        }

        invoke(realm1 -> realm1.update(new RealmRepresentation()), AdminAuth.Resource.REALM, true);
        invoke(RealmResource::pushRevocation, AdminAuth.Resource.REALM, true);
        invoke(realm4 -> realm4.deleteSession("nosuch", false), AdminAuth.Resource.USER, true);
        invoke(RealmResource::getClientSessionStats, AdminAuth.Resource.REALM, false);

        invoke(RealmResource::getDefaultGroups, AdminAuth.Resource.REALM, false);
        invoke(realm7 -> realm7.addDefaultGroup("nosuch"), AdminAuth.Resource.REALM, true);
        invoke(realm9 -> realm9.removeDefaultGroup("nosuch"), AdminAuth.Resource.REALM, true);
        GroupRepresentation newGroup = new GroupRepresentation();
        newGroup.setName("sample");
        adminClient.realm(REALM_NAME).groups().add(newGroup);
        GroupRepresentation group = adminClient.realms().realm(REALM_NAME).getGroupByPath("sample");

        invoke(realm2 -> realm2.getGroupByPath("sample"), AdminAuth.Resource.USER, false);

        adminClient.realms().realm(REALM_NAME).groups().group(group.getId()).remove();

        invoke((realm5, response) -> {
            TestLdapConnectionRepresentation config = new TestLdapConnectionRepresentation(
                    "nosuch", "nosuch", "nosuch", "nosuch", "nosuch", "nosuch");
            response.set(realm5.testLDAPConnection(config.getAction(), config.getConnectionUrl(), config.getBindDn(),
                    config.getBindCredential(), config.getUseTruststoreSpi(), config.getConnectionTimeout()));
            response.set(realm5.testLDAPConnection(config));
        }, AdminAuth.Resource.REALM, true);

        invoke((realm3, response) ->
                        response.set(realm3.partialImport(new PartialImportRepresentation())),
                AdminAuth.Resource.REALM, true);

        invoke(RealmResource::clearRealmCache, AdminAuth.Resource.REALM, true);
        invoke(RealmResource::clearUserCache, AdminAuth.Resource.REALM, true);

        // Delete realm
        invoke(realm6 -> clients.get("master-admin").realms().realm("nosuch").remove(), adminClient, true);
        invoke(realm8 -> clients.get("REALM2").realms().realm(REALM_NAME).remove(), adminClient, false);
        invoke(realm11 -> clients.get(AdminRoles.MANAGE_USERS).realms().realm(REALM_NAME).remove(), adminClient, false);
        invoke(realm10 -> clients.get(AdminRoles.REALM_ADMIN).realms().realm(REALM_NAME).remove(), adminClient, true);

        // Revert realm removal
        recreatePermissionRealm();
    }

    private void assertGettersEmpty(RealmRepresentation rep) {
        assertGettersEmpty(rep, "getRealm", "getAttributesOrEmpty", "getDisplayNameHtml",
                "getDisplayName", "getDefaultLocale", "getSupportedLocales");
    }

    private void assertGettersEmpty(Object rep, String... ignore) {
        List<String> ignoreList = Arrays.asList(ignore);

        for (Method m : rep.getClass().getDeclaredMethods()) {
            if (m.getParameterCount() == 0 && m.getName().startsWith("get") && !ignoreList.contains(m.getName())) {
                try {
                    Object o = m.invoke(rep);
                    assertNull(o, "Expected " + m.getName() + " to be null");
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        }
    }

    @Test
    public void flows() {
        invoke(realm -> realm.flows().getFormProviders(), AdminAuth.Resource.REALM, false);
        invoke(realm -> realm.flows().getAuthenticatorProviders(), AdminAuth.Resource.REALM, false);
        invoke(realm -> realm.flows().getClientAuthenticatorProviders(), AdminAuth.Resource.REALM, false, true);
        invoke(realm -> realm.flows().getFormActionProviders(), AdminAuth.Resource.REALM, false);
        invoke(realm -> realm.flows().getFlows(), AdminAuth.Resource.REALM, false, true);
        invoke((realm, response) -> response.set(realm.flows().createFlow(new AuthenticationFlowRepresentation())),
                AdminAuth.Resource.REALM, true);
        invoke(realm -> realm.flows().getFlow("nosuch"), AdminAuth.Resource.REALM, false);
        invoke(realm -> realm.flows().deleteFlow("nosuch"), AdminAuth.Resource.REALM, true);
        invoke((realm, response) -> response.set(realm.flows().copy("nosuch", Map.of())), AdminAuth.Resource.REALM, true);
        invoke(realm -> realm.flows().addExecutionFlow("nosuch", Map.of()), AdminAuth.Resource.REALM, true);
        invoke(realm -> realm.flows().addExecution("nosuch", Map.of()), AdminAuth.Resource.REALM, true);
        invoke(realm -> realm.flows().getExecutions("nosuch"), AdminAuth.Resource.REALM, false);
        invoke((RealmResource realm) -> realm.flows().getExecution("nosuch"), AdminAuth.Resource.REALM, false);
        invoke(realm -> realm.flows().updateExecutions("nosuch", new AuthenticationExecutionInfoRepresentation()),
                AdminAuth.Resource.REALM, true);
        invoke((realm, response) -> {
            AuthenticationExecutionRepresentation rep = new AuthenticationExecutionRepresentation();
            rep.setAuthenticator("auth-cookie");
            rep.setRequirement("CONDITIONAL");
            response.set(realm.flows().addExecution(rep));
        }, AdminAuth.Resource.REALM, true);
        invoke(realm -> realm.flows().raisePriority("nosuch"), AdminAuth.Resource.REALM, true);
        invoke(realm -> realm.flows().lowerPriority("nosuch"), AdminAuth.Resource.REALM, true);
        invoke(realm -> realm.flows().removeExecution("nosuch"), AdminAuth.Resource.REALM, true);
        invoke((realm, response) ->
                        response.set(realm.flows().newExecutionConfig("nosuch", new AuthenticatorConfigRepresentation())),
                AdminAuth.Resource.REALM, true);
        invoke(realm -> realm.flows().getAuthenticatorConfig("nosuch"), AdminAuth.Resource.REALM, false);
        invoke(realm -> realm.flows().getUnregisteredRequiredActions(), AdminAuth.Resource.REALM, false);
        invoke(realm -> realm.flows().registerRequiredAction(new RequiredActionProviderSimpleRepresentation()),
                AdminAuth.Resource.REALM, true);
        invoke(realm -> realm.flows().getRequiredActions(), AdminAuth.Resource.REALM, false, true);
        invoke(realm -> realm.flows().getRequiredAction("nosuch"), AdminAuth.Resource.REALM, false);
        invoke(realm -> realm.flows().removeRequiredAction("nosuch"), AdminAuth.Resource.REALM, true);
        invoke(realm -> realm.flows().updateRequiredAction("nosuch", new RequiredActionProviderRepresentation()),
                AdminAuth.Resource.REALM, true);
        invoke(realm -> realm.flows().getAuthenticatorConfigDescription("nosuch"), AdminAuth.Resource.REALM, false);
        invoke(realm -> realm.flows().getPerClientConfigDescription(), AdminAuth.Resource.REALM, false, true);
        invoke(realm -> realm.flows().getAuthenticatorConfig("nosuch"), AdminAuth.Resource.REALM, false);
        invoke(realm -> realm.flows().removeAuthenticatorConfig("nosuch"), AdminAuth.Resource.REALM, true);
        invoke(realm -> realm.flows().updateAuthenticatorConfig("nosuch", new AuthenticatorConfigRepresentation()), AdminAuth.Resource.REALM, true);
        invoke(realm -> {
            clients.get(AdminRoles.VIEW_REALM).realm(REALM_NAME).flows().getPerClientConfigDescription();
            clients.get(AdminRoles.VIEW_REALM).realm(REALM_NAME).flows().getClientAuthenticatorProviders();
            clients.get(AdminRoles.VIEW_REALM).realm(REALM_NAME).flows().getRequiredActions();
        }, adminClient, true);

        // Re-create realm
        adminClient.realm(REALM_NAME).remove();

        recreatePermissionRealm();
    }
}
