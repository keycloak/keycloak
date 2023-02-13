package org.keycloak.authorization.policy.provider.client;

import java.util.function.BiFunction;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;

public class ClientPolicyProvider implements PolicyProvider {

    private final BiFunction<Policy, AuthorizationProvider, ClientPolicyRepresentation> representationFunction;

    public ClientPolicyProvider(BiFunction<Policy, AuthorizationProvider, ClientPolicyRepresentation> representationFunction) {
        this.representationFunction = representationFunction;
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        ClientPolicyRepresentation representation = representationFunction.apply(evaluation.getPolicy(), evaluation.getAuthorizationProvider());
        AuthorizationProvider authorizationProvider = evaluation.getAuthorizationProvider();
        RealmModel realm = authorizationProvider.getKeycloakSession().getContext().getRealm();
        EvaluationContext context = evaluation.getContext();

        for (String client : representation.getClients()) {
            ClientModel clientModel = realm.getClientById(client);

            if (context.getAttributes().containsValue("kc.client.id", clientModel.getClientId())) {
                evaluation.grant();
                return;
            }
        }
    }

    @Override
    public void close() {

    }
}
