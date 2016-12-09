package org.keycloak.authorization.policy.provider.client;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;

import static org.keycloak.authorization.policy.provider.client.ClientPolicyProviderFactory.getClients;

public class ClientPolicyProvider implements PolicyProvider {

    private final Policy policy;
    private final AuthorizationProvider authorization;

    public ClientPolicyProvider(Policy policy, AuthorizationProvider authorization) {
        this.policy = policy;
        this.authorization = authorization;
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        EvaluationContext context = evaluation.getContext();
        String[] clients = getClients(this.policy);

        if (clients.length > 0) {
            for (String client : clients) {
                ClientModel clientModel = getCurrentRealm().getClientById(client);
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

    private RealmModel getCurrentRealm() {
        return this.authorization.getKeycloakSession().getContext().getRealm();
    }
}
