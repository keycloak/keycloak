package org.keycloak.tests.admin.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.federation.UserMapStorageFactory;

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

        UserRepresentation build = UserConfigBuilder.create()
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
