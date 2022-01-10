/*
 *  Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.policy.provider.clientscope;

import java.util.Set;
import java.util.function.BiFunction;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.authorization.ClientScopePolicyRepresentation;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class ClientScopePolicyProvider implements PolicyProvider {

    private final BiFunction<Policy, AuthorizationProvider, ClientScopePolicyRepresentation> representationFunction;

    public ClientScopePolicyProvider(
        BiFunction<Policy, AuthorizationProvider, ClientScopePolicyRepresentation> representationFunction) {
        this.representationFunction = representationFunction;
    }

    @Override
    public void close() {
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        Policy policy = evaluation.getPolicy();
        Set<ClientScopePolicyRepresentation.ClientScopeDefinition> clientScopeIds = representationFunction
            .apply(policy, evaluation.getAuthorizationProvider()).getClientScopes();
        AuthorizationProvider authorizationProvider = evaluation.getAuthorizationProvider();
        RealmModel realm = authorizationProvider.getKeycloakSession().getContext().getRealm();
        Identity identity = evaluation.getContext().getIdentity();

        for (ClientScopePolicyRepresentation.ClientScopeDefinition clientScopeDefinition : clientScopeIds) {
            ClientScopeModel clientScope = realm.getClientScopeById(clientScopeDefinition.getId());

            if (clientScope != null) {
                boolean hasClientScope = hasClientScope(identity, clientScope);

                if (!hasClientScope && clientScopeDefinition.isRequired()) {
                    evaluation.deny();
                    return;
                } else if (hasClientScope) {
                    evaluation.grant();
                }
            }
        }
    }

    private boolean hasClientScope(Identity identity, ClientScopeModel clientScope) {
        String clientScopeName = clientScope.getName();
        String[] clientScopes = identity.getAttributes().getValue("scope").asString(0).split(" ");
        for (String scope : clientScopes) {
            if (clientScopeName.equals(scope))
                return true;
        }
        return false;
    }

}
