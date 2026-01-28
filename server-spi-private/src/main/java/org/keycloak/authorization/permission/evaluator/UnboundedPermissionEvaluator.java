package org.keycloak.authorization.permission.evaluator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.permission.Permissions;
import org.keycloak.authorization.policy.evaluation.DecisionPermissionCollector;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.authorization.policy.evaluation.PolicyEvaluator;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.Permission;

public class UnboundedPermissionEvaluator implements PermissionEvaluator {

    private final EvaluationContext executionContext;
    private final AuthorizationProvider authorizationProvider;
    private final PolicyEvaluator policyEvaluator;
    private final ResourceServer resourceServer;
    private final AuthorizationRequest request;

    UnboundedPermissionEvaluator(EvaluationContext executionContext,
            AuthorizationProvider authorizationProvider, ResourceServer resourceServer,
            AuthorizationRequest request) {
        this.executionContext = executionContext;
        this.authorizationProvider = authorizationProvider;
        this.policyEvaluator = authorizationProvider.getPolicyEvaluator(resourceServer);
        this.resourceServer = resourceServer;
        this.request = request;
    }

    @Override
    public Decision evaluate(Decision decision) {
        StoreFactory storeFactory = authorizationProvider.getStoreFactory();

        try {
            Map<Policy, Map<Object, Decision.Effect>> decisionCache = new HashMap<>();

            storeFactory.setReadOnly(true);

            Permissions.all(resourceServer, executionContext.getIdentity(), authorizationProvider, request,
                    permission -> policyEvaluator.evaluate(permission, authorizationProvider, executionContext, decision, decisionCache));

            decision.onComplete();
        } catch (Throwable cause) {
            decision.onError(cause);
        } finally {
            storeFactory.setReadOnly(false);
        }

        return decision;
    }

    @Override
    public Collection<Permission> evaluate(ResourceServer resourceServer, AuthorizationRequest request) {
        DecisionPermissionCollector decision = getDecision(resourceServer, request, DecisionPermissionCollector.class);
        return decision.results();
    }

    @Override
    public <D extends Decision<?>> D getDecision(ResourceServer resourceServer, AuthorizationRequest request, Class<D> decisionType) {
        DecisionPermissionCollector decision = new DecisionPermissionCollector(authorizationProvider, resourceServer, request);

        evaluate(decision);

        return decisionType.cast(decision);
    }
}
