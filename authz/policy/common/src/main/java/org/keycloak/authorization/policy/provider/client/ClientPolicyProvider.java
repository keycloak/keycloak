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
        String[] clientIds = getClients(this.policy);

        if (clientIds.length > 0) {
            for (String clientId : clientIds) {
                if (context.getIdentity().getId().equals(clientId)) {
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
