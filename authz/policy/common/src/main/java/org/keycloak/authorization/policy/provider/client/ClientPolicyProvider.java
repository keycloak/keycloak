package org.keycloak.authorization.policy.provider.client;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.provider.PolicyProvider;

import static org.keycloak.authorization.policy.provider.client.ClientPolicyProviderFactory.getClients;

public class ClientPolicyProvider implements PolicyProvider {

    private final Policy policy;

    public ClientPolicyProvider(Policy policy) {
        this.policy = policy;
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        EvaluationContext context = evaluation.getContext();
        String[] clients = getClients(this.policy);

        if (clients.length > 0) {
            for (String client : clients) {
                if (context.getAttributes().containsValue("kc.client.id", client)) {
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
