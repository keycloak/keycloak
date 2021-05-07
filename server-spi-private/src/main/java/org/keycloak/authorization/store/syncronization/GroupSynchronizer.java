/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.store.syncronization;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class GroupSynchronizer implements Synchronizer<GroupModel.GroupRemovedEvent> {

    @Override
    public void synchronize(GroupModel.GroupRemovedEvent event, KeycloakSessionFactory factory) {
        ProviderFactory<AuthorizationProvider> providerFactory = factory.getProviderFactory(AuthorizationProvider.class);
        AuthorizationProvider authorizationProvider = providerFactory.create(event.getKeycloakSession());

        StoreFactory storeFactory = authorizationProvider.getStoreFactory();
        PolicyStore policyStore = storeFactory.getPolicyStore();
        GroupModel group = event.getGroup();
        Map<Policy.FilterOption, String[]> attributes = new EnumMap<>(Policy.FilterOption.class);

        attributes.put(Policy.FilterOption.TYPE, new String[] {"group"});
        attributes.put(Policy.FilterOption.CONFIG, new String[] {"groups", group.getId()});
        attributes.put(Policy.FilterOption.ANY_OWNER, Policy.FilterOption.EMPTY_FILTER);

        List<Policy> search = policyStore.findByResourceServer(attributes, null, -1, -1);

        for (Policy policy : search) {
            PolicyProviderFactory policyFactory = authorizationProvider.getProviderFactory(policy.getType());
            GroupPolicyRepresentation representation = GroupPolicyRepresentation.class.cast(policyFactory.toRepresentation(policy, authorizationProvider));
            Set<GroupPolicyRepresentation.GroupDefinition> groups = representation.getGroups();

            groups.removeIf(groupDefinition -> groupDefinition.getId().equals(group.getId()));

            if (groups.isEmpty()) {
                policyFactory.onRemove(policy, authorizationProvider);
                policyStore.delete(policy.getId());
            } else {
                policyFactory.onUpdate(policy, representation, authorizationProvider);
            }
        }
    }
}
