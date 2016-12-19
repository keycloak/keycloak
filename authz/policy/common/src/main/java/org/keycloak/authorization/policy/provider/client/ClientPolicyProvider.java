package org.keycloak.authorization.policy.provider.client;

import static org.keycloak.authorization.policy.provider.client.ClientPolicyProviderFactory.getClients;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;

public class ClientPolicyProvider implements PolicyProvider {

    @Override
    public void evaluate(Evaluation evaluation) {
        Policy policy = evaluation.getPolicy();
        EvaluationContext context = evaluation.getContext();
        String[] clients = getClients(policy);
        AuthorizationProvider authorizationProvider = evaluation.getAuthorizationProvider();
        RealmModel realm = authorizationProvider.getKeycloakSession().getContext().getRealm();

        if (clients.length > 0) {
            for (String client : clients) {
                ClientModel clientModel = realm.getClientById(client);
                if (context.getAttributes().containsValue("kc.client.id", clientModel.getClientId())) {
                    evaluation.grant();
                    return;
                }
            }
        }
    }

    @Override
    public void close() {

    }
}
