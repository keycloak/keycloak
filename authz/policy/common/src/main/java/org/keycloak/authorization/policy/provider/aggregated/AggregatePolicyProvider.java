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
package org.keycloak.authorization.policy.provider.aggregated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationPolicyProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.DecisionResultCollector;
import org.keycloak.authorization.policy.evaluation.DefaultEvaluation;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.evaluation.Result;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ResourceType;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AggregatePolicyProvider implements PolicyProvider, PartialEvaluationPolicyProvider {
    private static final Logger logger = Logger.getLogger(AggregatePolicyProvider.class);

    @Override
    public void evaluate(Evaluation evaluation) {
        logger.debugf("Aggregate policy %s evaluating using parent class", evaluation.getPolicy().getName());
        DecisionResultCollector decision = new DecisionResultCollector() {
            @Override
            protected void onComplete(Result result) {
                var aggregate = result.getResults().iterator().next();
                if (isGranted(aggregate)) {
                    evaluation.grant();
                } else {
                    if (aggregate.getPolicy().getLogic() == Logic.POSITIVE && aggregate.getAssociatedPolicies().stream()
                            .anyMatch(child -> result.getPermission().isExplicitDenial(child.getPolicy(), child.getEffect()))) {
                        result.getPermission().setExplicitAggregateDenial(aggregate.getPolicy(), true);
                    }
                    evaluation.deny();
                }
            }
        };
        AuthorizationProvider authorization = evaluation.getAuthorizationProvider();
        Policy policy = evaluation.getPolicy();
        DefaultEvaluation defaultEvaluation = DefaultEvaluation.class.cast(evaluation);
        Map<Policy, Map<Object, Decision.Effect>> decisionCache = defaultEvaluation.getDecisionCache();
        ResourcePermission permission = evaluation.getPermission();
        permission.setExplicitAggregateDenial(policy, false);

        for (Policy associatedPolicy : policy.getAssociatedPolicies()) {
            Map<Object, Decision.Effect> decisions = decisionCache.computeIfAbsent(associatedPolicy, p -> new HashMap<>());
            Decision.Effect effect = decisions.get(permission);
            DefaultEvaluation eval = new DefaultEvaluation(evaluation.getPermission(), evaluation.getContext(), policy, associatedPolicy, decision, authorization, decisionCache);

            if (effect == null) {
                PolicyProvider policyProvider = authorization.getProvider(associatedPolicy.getType());

                policyProvider.evaluate(eval);

                eval.denyIfNoEffect();
                decisions.put(permission, eval.getEffect());
            } else {
                eval.setEffect(effect);
            }

            if (eval.getEffect() == Decision.Effect.INVALID) {
                evaluation.setEffect(Decision.Effect.INVALID);
                return;
            }
        }

        decision.onComplete(permission);
    }

    @Override
    public void close() {

    }

    @Override
    public boolean supports(Policy policy) {
        return AggregatePolicyProviderFactory.ID.equals(policy.getType());
    }

    @Override
    public Stream<Policy> getPermissions(KeycloakSession session, ResourceType resourceType, ResourceType groupResourceType, UserModel subject) {
        AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
        RealmModel realm = session.getContext().getRealm();
        ClientModel adminPermissionsClient = realm.getAdminPermissionsClient();
        StoreFactory storeFactory = provider.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findByClient(adminPermissionsClient);

        return storeFactory.getPolicyStore().findDependentPolicies(resourceServer, resourceType.getType(), groupResourceType == null ? null : groupResourceType.getType(), AggregatePolicyProviderFactory.ID, null, List.of());
    }

    @Override
    public boolean evaluate(KeycloakSession session, Policy policy, UserModel subject) {
        return Outcome.GRANT.equals(evaluateOutcome(session, policy, subject));
    }

    @Override
    public Outcome evaluateOutcome(KeycloakSession session, Policy policy, UserModel subject) {
        return evaluateChildren(session, policy, subject, false);
    }

    @Override
    public Outcome evaluateMemberOutcome(KeycloakSession session, Policy policy, UserModel subject) {
        return evaluateChildren(session, policy, subject, true);
    }

    private Outcome evaluateChildren(KeycloakSession session, Policy policy, UserModel subject, boolean memberScope) {
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        DecisionStrategy decisionStrategy = policy.getDecisionStrategy();
        Set<Policy> associatedPolicies = policy.getAssociatedPolicies();
        int grants = 0;
        int evaluated = 0;
        int forceDeny = 0;
        boolean explicitDenial = false;

        for (Policy associatedPolicy : associatedPolicies) {
            PolicyProvider policyProvider = authorization.getProvider(associatedPolicy.getType());

            if (policyProvider != null && !policyProvider.isValid(associatedPolicy, authorization)) {
                return Outcome.INVALID;
            }

            if (policyProvider instanceof PartialEvaluationPolicyProvider partialPolicyProvider) {
                Outcome childOutcome = memberScope ? partialPolicyProvider.evaluateMemberOutcome(session, associatedPolicy, subject)
                        : partialPolicyProvider.evaluateOutcome(session, associatedPolicy, subject);

                if (Outcome.INVALID.equals(childOutcome)) {
                    return Outcome.INVALID;
                }

                if (Outcome.FORCE_DENY.equals(childOutcome)) {
                    forceDeny++;
                    continue;
                }

                if (Outcome.SKIP.equals(childOutcome)) {
                    continue;
                }

                evaluated++;

                boolean childGranted = Outcome.GRANT.equals(childOutcome);

                if (Logic.NEGATIVE.equals(associatedPolicy.getLogic())) {
                    childGranted = !childGranted;
                }

                if (!childGranted && (associatedPolicy.getLogic() == Logic.NEGATIVE || childOutcome == Outcome.EXPLICIT_DENY)) {
                    explicitDenial = true;
                }
                if (childGranted) {
                    grants++;
                }
            } else {
                forceDeny++;
            }
        }

        if (memberScope && evaluated < associatedPolicies.size() && explicitDenial) {
            // An unresolved child combined with an explicit denial may change
            // whether that denial propagates to another membership.
            return Outcome.FORCE_DENY;
        }
        if (evaluated == 0) {
            return forceDeny == 0 ? Outcome.SKIP: Outcome.FORCE_DENY;
        }

        Outcome denial = memberScope && explicitDenial ? Outcome.EXPLICIT_DENY : Outcome.DENY;
        if (grants == 0) {
            return denial;
        }

        // uses total policy count, not just evaluated, so skipped and unsupported children count against unanimity/consensus;
        // this is intentionally conservative — partial evaluation cannot resolve unsupported policies, so it denies rather
        // than risk granting access that runtime evaluation would deny
        boolean granted = switch (decisionStrategy) {
            case AFFIRMATIVE -> true;
            case UNANIMOUS -> grants == associatedPolicies.size();
            case CONSENSUS -> grants > associatedPolicies.size() - grants;
        };

        return granted ? Outcome.GRANT : denial;
    }
}
