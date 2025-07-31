/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.policy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

public class ResourcePolicyManager {

    private final KeycloakSession session;
    private final ResourcePolicyProvider policyProvider;
    private final ResourcePolicyStateProvider stateProvider;
    
    private static final Logger log = Logger.getLogger(ResourcePolicyManager.class);

    public ResourcePolicyManager(KeycloakSession session) {
        this.session = session;
        this.policyProvider = session.getProvider(ResourcePolicyProvider.class);
        this.stateProvider = session.getProvider(ResourcePolicyStateProvider.class);
    }

    public void runPolicies() {
        List<ResourcePolicy> policies = policyProvider.getPolicies();

        for (ResourcePolicy policy : policies) {
            runPolicy(policy);
        }
    }

    private void runPolicy(ResourcePolicy policy) {
        log.tracev("Running policy {0}", policy.getProviderId());

        // no actions -> skip
        List<ResourceAction> actions = policyProvider.getActions(policy.getId());
        if (actions.isEmpty()) {
            return;
        }

        // fetch all candidate lists for subsequent actions
        // need to load all candidates before creation a state record for initial action
        // if we don't do this, we risk executing more actions for single resource (user) in one run (in case the actions were modified by admin)
        Map<String, List<String>> candidatesForAction = new HashMap<>();
        for (int i = 1; i < actions.size(); i++) {
            ResourceAction previousAction = actions.get(i - 1);
            List<String> candidateIds = stateProvider.findResourceIdsByLastCompletedAction(policy.getId(), previousAction.getId());
            candidatesForAction.put(actions.get(i).getId(), candidateIds);
        }

        // Process the Initial action (State Zero) - look for eligable users NOT present in the state table.
        ResourceAction initialAction = actions.get(0);
        ResourceActionProvider actionProvider = getActionProvider(initialAction);
        log.tracev("Initial action {0}", initialAction.getProviderId());

        TimeBasedResourcePolicyProvider timeBasedProvider = session.getProvider(TimeBasedResourcePolicyProvider.class, policy.getProviderId());
        
        List<String> newResourceIds = timeBasedProvider.getEligibleResourcesForInitialAction(policy, initialAction.getAfter());
        log.tracev("Eligable resource IDs for initial action {0}", newResourceIds);
        // <comment> todo: do we want to wrap it into separate tx? So we have more granular approach for handling errors & possible retries??
        if (!newResourceIds.isEmpty()) {
            // run action
            actionProvider.run(newResourceIds);

            // create state record
            stateProvider.updateState(policy.getId(), policy.getProviderId(), newResourceIds, initialAction.getId());
        }
        // </comment>

        // Process the rest of the actions
        for (ResourceAction action : actions) {
            // Find all resources that have completed the PREVIOUS action.
            List<String> candidateIds = candidatesForAction.getOrDefault(action.getId(), Collections.emptyList());

            if (candidateIds.isEmpty()) {
                continue; // No users are at this stage yet.
            }

            // Ask the policyProvider to filter these candidates based on time.
            List<String> eligibleIds = timeBasedProvider.filterEligibleResources(candidateIds, action.getAfter());

            // <comment> todo: do we want to wrap it into separate tx? So we have more granular approach for handling errors & possible retries??
            if (!eligibleIds.isEmpty()) {
                // Get the action provider and run the action on the eligible users.
                actionProvider = getActionProvider(action);
                actionProvider.run(eligibleIds);

                // Update the state for the users that were processed.
                stateProvider.updateState(policy.getId(), policy.getProviderId(), eligibleIds, action.getId());
            }
            // </comment>
        }
    }

    private ResourceActionProvider getActionProvider(ResourceAction action) {
        return session.getProvider(ResourceActionProvider.class, action.getProviderId());
    }

}
