package org.keycloak.authorization.policy.provider.client;

import java.util.Set;
import java.util.function.BiFunction;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;

import org.jboss.logging.Logger;

public class ClientPolicyProvider implements PolicyProvider {

    private static final Logger logger = Logger.getLogger(ClientPolicyProvider.class);
    private final BiFunction<Policy, AuthorizationProvider, ClientPolicyRepresentation> representationFunction;

    public ClientPolicyProvider(BiFunction<Policy, AuthorizationProvider, ClientPolicyRepresentation> representationFunction) {
        this.representationFunction = representationFunction;
    }

    @Override
    public boolean isValid(Policy policy, AuthorizationProvider authorization) {
        Set<String> clients = representationFunction.apply(policy, authorization).getClients();
        RealmModel realm = authorization.getRealm();
        if (Logic.NEGATIVE.equals(policy.getLogic())) {
            return !clients.isEmpty() && clients.stream().allMatch(id -> realm.getClientById(id) != null);
        }
        return clients.stream().anyMatch(id -> realm.getClientById(id) != null);
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        Policy policy = evaluation.getPolicy();
        AuthorizationProvider authorizationProvider = evaluation.getAuthorizationProvider();
        Set<String> clients = representationFunction.apply(policy, authorizationProvider).getClients();
        RealmModel realm = authorizationProvider.getKeycloakSession().getContext().getRealm();
        EvaluationContext context = evaluation.getContext();
        boolean negative = Logic.NEGATIVE.equals(policy.getLogic());
        ClientModel matchedClient = null;
        boolean hasResolvedClient = false;

        for (String client : clients) {
            ClientModel clientModel = realm.getClientById(client);
            if (clientModel == null) {
                if (negative) {
                    evaluation.setEffect(Effect.INVALID);
                    return;
                }
                continue;
            }
            hasResolvedClient = true;

            if (matchedClient == null && context.getAttributes().containsValue(EvaluationContext.CLIENT_ID_ATTRIBUTE, clientModel.getClientId())) {
                matchedClient = clientModel;
                if (!negative) {
                    break;
                }
            }
        }

        if (!hasResolvedClient) {
            evaluation.setEffect(Effect.INVALID);
        } else if (matchedClient != null) {
            evaluation.grant();
            logger.debugf("Client policy %s matched with client %s", policy.getName(), matchedClient.getClientId());
        }
    }

    @Override
    public void close() {

    }
}
