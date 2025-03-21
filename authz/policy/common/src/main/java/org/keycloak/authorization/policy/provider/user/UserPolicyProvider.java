/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.policy.provider.user;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.provider.PartialEvaluationPolicyProvider;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.ResourceType;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserPolicyProvider implements PolicyProvider, PartialEvaluationPolicyProvider {

    private static final Logger logger = Logger.getLogger(UserPolicyProvider.class);

    private final BiFunction<Policy, AuthorizationProvider, UserPolicyRepresentation> representationFunction;

    public UserPolicyProvider(BiFunction<Policy, AuthorizationProvider, UserPolicyRepresentation> representationFunction) {
        this.representationFunction = representationFunction;
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        EvaluationContext context = evaluation.getContext();
        UserPolicyRepresentation policy = representationFunction.apply(evaluation.getPolicy(), evaluation.getAuthorizationProvider());

        if (isGranted(context.getIdentity().getId(), policy)) {
            evaluation.grant();
        }

        logger.debugf("User policy %s evaluated to status %s on identity %s with accepted users: %s", evaluation.getPolicy().getName(), evaluation.getEffect(), evaluation.getContext().getIdentity().getId(), policy.getUsers());
    }

    private boolean isGranted(String subject, UserPolicyRepresentation policy) {
        for (String userId : policy.getUsers()) {
            if (subject.equals(userId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Stream<Policy> getPermissions(KeycloakSession session, ResourceType resourceType, UserModel subject) {
        AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
        RealmModel realm = session.getContext().getRealm();
        ClientModel adminPermissionsClient = realm.getAdminPermissionsClient();
        StoreFactory storeFactory = provider.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(adminPermissionsClient);
        PolicyStore policyStore = storeFactory.getPolicyStore();

        return policyStore.findDependentPolicies(resourceServer, resourceType.getType(), UserPolicyProviderFactory.ID, "users", subject.getId());
    }

    @Override
    public boolean evaluate(KeycloakSession session, Policy policy, UserModel adminUser) {
        AuthorizationProvider authorizationProvider = session.getProvider(AuthorizationProvider.class);
        return isGranted(adminUser.getId(), representationFunction.apply(policy, authorizationProvider));
    }

    @Override
    public boolean supports(Policy policy) {
        return UserPolicyProviderFactory.ID.equals(policy.getType());
    }

    @Override
    public void close() {

    }
}
