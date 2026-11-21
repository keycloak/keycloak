package org.keycloak.tests.admin.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityLinkRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.providers.federation.UserMapStorageFactory;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = UserFedarationTest.UserFederationServerConfig.class)
public class UserFedarationTest extends AbstractUserTest {

    @Test
    public void getFederatedIdentities() {
        // Add sample identity provider
        addSampleIdentityProvider();

        // Add sample user
        String id = createUser();
        UserResource user = managedRealm.admin().users().get(id);
        assertEquals(0, user.getFederatedIdentity().size());

        // Add social link to the user
        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setUserId("social-user-id");
        link.setUserName("social-username");
        addFederatedIdentity(id, "social-provider-id", link);

        // Verify social link is here
        List<FederatedIdentityRepresentation> federatedIdentities = user.getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        link = federatedIdentities.get(0);
        assertEquals("social-provider-id", link.getIdentityProvider());
        assertEquals("social-user-id", link.getUserId());
        assertEquals("social-username", link.getUserName());

        // Remove social link now
        user.removeFederatedIdentity("social-provider-id");
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.userFederatedIdentityLink(id, "social-provider-id"), ResourceType.USER);
        assertEquals(0, user.getFederatedIdentity().size());

        removeSampleIdentityProvider();
    }

    @Test
    public void federatedIdentityLinks() {
        addSampleIdentityProvider();

        String id = createUser();
        UserResource user = managedRealm.admin().users().get(id);

        // not linked yet: nothing in the linked list, the sample IdP is available to link
        assertEquals(0, user.getFederatedIdentityLinks(true, null, null, null).size());
        List<FederatedIdentityLinkRepresentation> available = user.getFederatedIdentityLinks(false, null, null, null);
        assertEquals(1, available.size());
        assertEquals("social-provider-id", available.get(0).getIdentityProvider());
        assertEquals("oidc", available.get(0).getProviderId());

        // a lone '"' is forwarded to the identity provider search (JPA layer); it must not crash the
        // "available" branch, and consistently with the "linked" branch it matches nothing
        assertEquals(0, user.getFederatedIdentityLinks(false, "\"", null, null).size());

        // non-exact searches are case-insensitive in the "available" branch (DB-backed); the "linked"
        // branch must behave the same way
        assertEquals(1, user.getFederatedIdentityLinks(false, "SOCIAL-PROVIDER", null, null).size());

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setUserId("social-user-id");
        link.setUserName("social-username");
        addFederatedIdentity(id, "social-provider-id", link);

        List<FederatedIdentityLinkRepresentation> linked = user.getFederatedIdentityLinks(true, null, null, null);
        assertEquals(1, linked.size());
        FederatedIdentityLinkRepresentation linkedRep = linked.get(0);
        assertEquals("social-provider-id", linkedRep.getIdentityProvider());
        assertEquals("oidc", linkedRep.getProviderId());
        assertEquals("social-user-id", linkedRep.getUserId());
        assertEquals("social-username", linkedRep.getUserName());

        // once linked, it should no longer be offered as available for linking
        assertEquals(0, user.getFederatedIdentityLinks(false, null, null, null).size());

        // a bare "*" or "\"" search should match everything rather than throwing
        assertEquals(1, user.getFederatedIdentityLinks(true, "*", null, null).size());
        assertEquals(0, user.getFederatedIdentityLinks(true, "\"", null, null).size());

        // non-exact searches must stay case-insensitive once linked too, consistently with the
        // "available" branch
        assertEquals(1, user.getFederatedIdentityLinks(true, "SOCIAL-PROVIDER", null, null).size());

        user.removeFederatedIdentity("social-provider-id");
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.userFederatedIdentityLink(id, "social-provider-id"), ResourceType.USER);

        removeSampleIdentityProvider();
    }

    @Test
    public void federatedIdentityLinksSearchByResolvedDisplayName() {
        // a social provider with no displayName configured falls back to the factory's display name (e.g.
        // "GitHub" for providerId "github") - searching by that resolved name must work the same way for both
        // the "linked" and "available" branches, even though it isn't stored in the alias/displayName columns
        IdentityProviderRepresentation rep = new IdentityProviderRepresentation();
        rep.setAlias("work");
        rep.setProviderId("github");
        rep.setConfig(Map.of("clientId", "dummy", "clientSecret", "dummy"));
        // AbstractUserTest#after() already removes every identity provider in the realm after each test
        managedRealm.admin().identityProviders().create(rep);
        adminEvents.clear();

        String id = createUser();
        UserResource user = managedRealm.admin().users().get(id);

        List<FederatedIdentityLinkRepresentation> available = user.getFederatedIdentityLinks(false, "GitHub", null, null);
        assertEquals(1, available.size());
        assertEquals("work", available.get(0).getIdentityProvider());
        assertEquals("GitHub", available.get(0).getDisplayName());

        // the exact (quoted) search is case-sensitive in both branches - "GitHub" matches the resolved
        // display name exactly, but the lowercase "github" must not
        assertEquals(1, user.getFederatedIdentityLinks(false, "\"GitHub\"", null, null).size());
        assertEquals(0, user.getFederatedIdentityLinks(false, "\"github\"", null, null).size());

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setUserId("social-user-id");
        link.setUserName("social-username");
        user.addFederatedIdentity("work", link);

        List<FederatedIdentityLinkRepresentation> linked = user.getFederatedIdentityLinks(true, "GitHub", null, null);
        assertEquals(1, linked.size());
        assertEquals("work", linked.get(0).getIdentityProvider());

        assertEquals(1, user.getFederatedIdentityLinks(true, "\"GitHub\"", null, null).size());
        assertEquals(0, user.getFederatedIdentityLinks(true, "\"github\"", null, null).size());

        user.removeFederatedIdentity("work");
        adminEvents.clear();
    }

    @Test
    public void federatedIdentityLinksResolveBeyondDefaultPageSize() {
        // reproduces https://github.com/keycloak/keycloak/issues/52482 - a linked identity provider whose alias
        // sorts past the default page size of the identity providers list must still resolve correctly.
        String linkedAlias = "zzz-linked-provider";
        for (int i = 0; i < 110; i++) {
            IdentityProviderRepresentation rep = new IdentityProviderRepresentation();
            rep.setAlias("idp-" + String.format("%03d", i));
            rep.setProviderId("oidc");
            managedRealm.admin().identityProviders().create(rep);
        }
        IdentityProviderRepresentation linkedRep = new IdentityProviderRepresentation();
        linkedRep.setAlias(linkedAlias);
        linkedRep.setProviderId("oidc");
        managedRealm.admin().identityProviders().create(linkedRep);
        adminEvents.clear();

        String id = createUser();
        UserResource user = managedRealm.admin().users().get(id);

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setUserId("social-user-id");
        link.setUserName("social-username");
        user.addFederatedIdentity(linkedAlias, link);

        try {
            List<FederatedIdentityLinkRepresentation> linked = user.getFederatedIdentityLinks(true, null, null, null);
            assertEquals(1, linked.size());
            assertEquals(linkedAlias, linked.get(0).getIdentityProvider());
            assertEquals("oidc", linked.get(0).getProviderId());

            // default page size for the "available" listing is 100, with 110 unlinked providers present
            assertEquals(100, user.getFederatedIdentityLinks(false, null, null, null).size());
            // raising max confirms the linked provider is correctly excluded and all others are still reachable
            List<FederatedIdentityLinkRepresentation> allAvailable = user.getFederatedIdentityLinks(false, null, 0, 1000);
            assertEquals(110, allAvailable.size());
            assertEquals(0, allAvailable.stream().filter(idp -> linkedAlias.equals(idp.getIdentityProvider())).count());
        } finally {
            // AbstractUserTest#after only removes the first page of identity providers (itself an instance of the
            // same pagination gap this test covers), so fully clean up the realm here to avoid leaking into other tests.
            List<IdentityProviderRepresentation> remaining;
            while (!(remaining = managedRealm.admin().identityProviders().findAll()).isEmpty()) {
                remaining.forEach(idp -> managedRealm.admin().identityProviders().get(idp.getAlias()).remove());
            }
        }
    }

    @Test
    public void testUpdateCredentialLabelForFederatedUser() {
        // Create user federation
        ComponentRepresentation memProvider = new ComponentRepresentation();
        memProvider.setName("memory");
        memProvider.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        memProvider.setProviderType(UserStorageProvider.class.getName());
        memProvider.setConfig(new MultivaluedHashMap<>());
        memProvider.getConfig().putSingle("priority", Integer.toString(0));
        memProvider.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(false));

        String memProviderId = ApiUtil.getCreatedId(managedRealm.admin().components().add(memProvider));
        managedRealm.cleanup().add(realm -> realm.components().component(memProviderId).remove());

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.componentPath(memProviderId), memProvider, ResourceType.COMPONENT);

        // Create federated user
        String username = "fed-user1";
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(username);
        userRepresentation.setEmail("feduser1@mail.com");
        userRepresentation.setRequiredActions(Collections.emptyList());
        userRepresentation.setEnabled(true);
        userRepresentation.setFederationLink(memProviderId);

        PasswordCredentialModel pcm = PasswordCredentialModel.createFromValues("my-algorithm", "theSalt".getBytes(), 22, "ABC");
        CredentialRepresentation hashedPassword = ModelToRepresentation.toRepresentation(pcm);
        hashedPassword.setCreatedDate(1001L);
        hashedPassword.setUserLabel("label");
        hashedPassword.setType(CredentialRepresentation.PASSWORD);

        userRepresentation.setCredentials(Arrays.asList(hashedPassword));
        String userId = ApiUtil.getCreatedId(managedRealm.admin().users().create(userRepresentation));
        Assert.assertFalse(StorageId.isLocalStorage(userId));

        UserResource user = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), username);
        List<CredentialRepresentation> credentials = user.credentials();
        Assertions.assertNotNull(credentials);
        Assertions.assertEquals(1, credentials.size());
        Assertions.assertEquals("label", credentials.get(0).getUserLabel());

        // Update federated credential user label
        user.setCredentialUserLabel(credentials.get(0).getId(), "updatedLabel");
        credentials = user.credentials();
        Assertions.assertNotNull(credentials);
        Assertions.assertEquals(1, credentials.size());
        Assertions.assertEquals("updatedLabel", credentials.get(0).getUserLabel());
    }

    @Test
    public void createFederatedIdentities() {
        String identityProviderAlias = "social-provider-id";
        String username = "federated-identities";
        String federatedUserId = "federated-user-id";

        addSampleIdentityProvider();

        UserRepresentation build = UserBuilder.create()
                .username(username)
                .federatedLink(identityProviderAlias, federatedUserId, username)
                .build();

        //when
        String userId = createUser(build, false);
        List<FederatedIdentityRepresentation> obtainedFederatedIdentities = managedRealm.admin().users().get(userId).getFederatedIdentity();

        //then
        assertEquals(1, obtainedFederatedIdentities.size());
        assertEquals(federatedUserId, obtainedFederatedIdentities.get(0).getUserId());
        assertEquals(username, obtainedFederatedIdentities.get(0).getUserName());
        assertEquals(identityProviderAlias, obtainedFederatedIdentities.get(0).getIdentityProvider());
    }

    private void removeSampleIdentityProvider() {
        IdentityProviderResource resource = managedRealm.admin().identityProviders().get("social-provider-id");
        Assertions.assertNotNull(resource);
        resource.remove();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.identityProviderPath("social-provider-id"), ResourceType.IDENTITY_PROVIDER);
    }

    public static class UserFederationServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}
