package org.keycloak.testsuite.authz;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.runonserver.RunOnServer;

import static org.keycloak.authorization.model.Policy.FilterOption.OWNER;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserManagedPermissionServiceRunOnServerHelpers {

    static RunOnServer testRemovePoliciesOnResourceDelete() {
        return session -> {
            RealmModel realm = session.realms().getRealmByName("authz-test");
            ClientModel client = realm.getClientByClientId("resource-server-test");
            AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
            UserModel user = session.users().getUserByUsername(realm, "marta");
            ResourceServer resourceServer = provider.getStoreFactory().getResourceServerStore().findByClient(client);
            Map<Policy.FilterOption, String[]> filters = new HashMap<>();

            filters.put(Policy.FilterOption.TYPE, new String[] {"uma"});
            filters.put(OWNER, new String[] {user.getId()});

            List<Policy> policies = provider.getStoreFactory().getPolicyStore()
                    .find(resourceServer, filters, null, null);
            assertEquals(1, policies.size());

            Policy policy = policies.get(0);
            assertFalse(policy.getResources().isEmpty());

            Resource resource = policy.getResources().iterator().next();
            assertEquals("Resource A", resource.getName());

            provider.getStoreFactory().getResourceStore().delete(resource.getId());

            filters = new HashMap<>();

            filters.put(OWNER, new String[] {user.getId()});
            policies = provider.getStoreFactory().getPolicyStore()
                    .find(resourceServer, filters, null, null);
            assertTrue(policies.isEmpty());
        };
    }

    static RunOnServer testRemovePolicyWhenOwnerDeleted() {
        return session -> {
            RealmModel realm = session.realms().getRealmByName("authz-test");
            ClientModel client = realm.getClientByClientId("resource-server-test");
            AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
            ResourceServer resourceServer = provider.getStoreFactory().getResourceServerStore().findByClient(client);
            Map<Policy.FilterOption, String[]> filters = new HashMap<>();

            filters.put(Policy.FilterOption.TYPE, new String[]{"uma"});

            PolicyStore policyStore = provider.getStoreFactory().getPolicyStore();
            List<Policy> policies = policyStore
                    .find(resourceServer, filters, null, null);
            assertTrue(policies.isEmpty());

            policies = policyStore
                    .find(resourceServer, Collections.emptyMap(), null, null);
            assertTrue(policies.isEmpty());
        };
    }

    static RunOnServer createOwnerBoundResourceTypePolicy() {
        return session -> {
            RealmModel realm = session.realms().getRealmByName("authz-test");
            session.getContext().setRealm(realm);
            ClientModel client = realm.getClientByClientId("resource-server-test");
            AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
            ResourceServer resourceServer = provider.getStoreFactory().getResourceServerStore().findByClient(client);
            UserModel marta = session.users().getUserByUsername(realm, "marta");

            UserPolicyRepresentation userPolicyRep = new UserPolicyRepresentation();
            userPolicyRep.setName(KeycloakModelUtils.generateId());
            userPolicyRep.addUser("kolo");

            PolicyStore policyStore = provider.getStoreFactory().getPolicyStore();
            Policy userPolicy = policyStore.create(resourceServer, userPolicyRep);
            userPolicy.setOwner(marta.getId());

            PolicyRepresentation policyRep = new PolicyRepresentation();
            policyRep.setName(KeycloakModelUtils.generateId());
            policyRep.setType("uma");
            policyRep.addPolicy(userPolicy.getId());

            Policy policy = policyStore.create(resourceServer, policyRep);
            policy.setOwner(marta.getId());
            policy.setResourceType("patient-data");

            Scope scope = provider.getStoreFactory().getScopeStore().findByName(resourceServer, "read");
            policy.addScope(scope);
        };
    }

}
