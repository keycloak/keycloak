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

import jakarta.ws.rs.BadRequestException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.policy.ResourcePolicyStateProvider.ScheduledAction;
import org.keycloak.provider.ProviderFactory;

public class ResourcePolicyManager {

    private static final Logger log = Logger.getLogger(ResourcePolicyManager.class);

    public static boolean isFeatureEnabled() {
        return Profile.isFeatureEnabled(Feature.RESOURCE_LIFECYCLE);
    }

    private final KeycloakSession session;

    public ResourcePolicyManager(KeycloakSession session) {
        this.session = session;
    }

    public ResourcePolicy addPolicy(String providerId) {
        return addPolicy(new ResourcePolicy(providerId));
    }

    public ResourcePolicy addPolicy(String providerId, Map<String, List<String>> config) {
        return addPolicy(new ResourcePolicy(providerId, config));
    }

    public ResourcePolicy addPolicy(ResourcePolicy policy) {
        RealmModel realm = getRealm();
        ComponentModel model = new ComponentModel();

        model.setParentId(realm.getId());
        model.setProviderId(policy.getProviderId());
        model.setProviderType(ResourcePolicyProvider.class.getName());

        MultivaluedHashMap<String, String> config = policy.getConfig();

        if (config != null) {
            model.setConfig(config);
        }

        return new ResourcePolicy(realm.addComponentModel(model));
    }

    /*
        This method takes an ordered list of actions. First action in the list has the highest priority, last action has the lowest priority
        It is used for both create and update actions
        ---------------------------------------------------------------------------------------
        using delete-and-recreate approach for now as it seems more simple and robust solution
        todo: consider changing it to "diff-and-update" (more complex) approach where we'd need to
            * keep existing actions
            * create newly added actions
            * delete removed actions
            * reorder existing action according to new order (we may add gaps between priority so that we won't need to update all existing actions)
                * with the gap approach, it may eventually happen that there won't be any space between the two action, in that case we'd have to trigger recalculation of priorities
    */
    public void updateActions(ResourcePolicy policy, List<ResourceAction> actions) {

        validateActions(actions);

        // get the stable IDs of the new actions
        Set<String> newActionIds = actions.stream()
                .map(ResourceAction::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // get the stable IDs of the old actions
        List<ResourceAction> oldActions = getActions(policy);
        Set<String> oldActionIds = oldActions.stream()
                .map(ResourceAction::getId)
                .collect(Collectors.toSet());

        // find which action IDs were deleted
        oldActionIds.removeAll(newActionIds); // The remaining IDs are the deleted ones

        ResourcePolicyStateProvider stateProvider = getResourcePolicyStateProvider();
        // delete orphaned state records - this means that we actually reset the flow for users which completed the action which is being removed
        // it seems like the best way to handle this
        if (!oldActionIds.isEmpty()) {
            stateProvider.removeByCompletedActions(policy.getId(), oldActionIds);
        }

        RealmModel realm = getRealm();
        // remove all existing actions of the policy
        realm.removeComponents(policy.getId());

        // add the new actions
        for (int i = 0; i < actions.size(); i++) {
            ResourceAction action = actions.get(i);

            // assign priority based on index.
            action.setPriority(i + 1);

            // persist the new action component.
            addAction(policy, action);
        }
    }

    private ResourceAction addAction(ResourcePolicy policy, ResourceAction action) {
        RealmModel realm = getRealm();
        ComponentModel policyModel = realm.getComponent(policy.getId());
        ComponentModel actionModel = new ComponentModel();

        actionModel.setId(action.getId());//need to keep stable UUIDs not to break a link in state table
        actionModel.setParentId(policyModel.getId());
        actionModel.setProviderId(action.getProviderId());
        actionModel.setProviderType(ResourceActionProvider.class.getName());
        actionModel.setConfig(action.getConfig());

        return new ResourceAction(realm.addComponentModel(actionModel));
    }

    public List<ResourcePolicy> getPolicies() {
        RealmModel realm = getRealm();
        return realm.getComponentsStream(realm.getId(), ResourcePolicyProvider.class.getName())
                .map(ResourcePolicy::new).toList();
    }

    public List<ResourceAction> getActions(ResourcePolicy policy) {
        RealmModel realm = getRealm();
        return realm.getComponentsStream(policy.getId(), ResourceActionProvider.class.getName())
                .map(ResourceAction::new).sorted().toList();
    }

    public void runPolicies() {
        List<ResourcePolicy> policies = getPolicies();

        for (ResourcePolicy policy : policies) {
            runPolicy(policy);
        }
    }

    private void runPolicy(ResourcePolicy policy) {
        log.tracev("Running policy {0}", policy.getProviderId());

        // no actions -> skip
        List<ResourceAction> actions = getActions(policy);
        if (actions.isEmpty()) {
            return;
        }

        ResourcePolicyProvider policyProvider = getPolicyProvider(policy);
        ResourcePolicyStateProvider stateProvider = getResourcePolicyStateProvider();

        // fetch all candidate lists for subsequent actions
        // need to load all candidates before creation a state record for initial action
        // if we don't do this, we risk executing more actions for single resource (user) in one run (in case the actions were modified by admin)
        Map<String, List<String>> candidatesForAction = new HashMap<>();
        for (int i = 1; i < actions.size(); i++) {
            ResourceAction previousAction = actions.get(i - 1);
            List<String> candidateIds = stateProvider.findResourceIdsByScheduledAction(policy.getId(), previousAction.getId());
            candidatesForAction.put(actions.get(i).getId(), candidateIds);
        }

        // Process the Initial action (State Zero) - look for eligable users NOT present in the state table.
        ResourceAction initialAction = actions.get(0);
        ResourceActionProvider actionProvider = getActionProvider(initialAction);
        log.tracev("Initial action {0}", initialAction.getProviderId());

        List<String> newResourceIds = policyProvider.getEligibleResourcesForInitialAction(initialAction.getAfter());
        log.tracev("Eligable resource IDs for initial action {0}", newResourceIds);
        // <comment> todo: do we want to wrap it into separate tx? So we have more granular approach for handling errors & possible retries??
        if (!newResourceIds.isEmpty()) {
            // run action
            runAction(actionProvider, newResourceIds);

            // create state record
            stateProvider.update(policy.getId(), policy.getProviderId(), newResourceIds, initialAction.getId());
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
            List<String> eligibleIds = policyProvider.filterEligibleResources(candidateIds, action.getAfter());

            // <comment> todo: do we want to wrap it into separate tx? So we have more granular approach for handling errors & possible retries??
            if (!eligibleIds.isEmpty()) {
                // Get the action provider and run the action on the eligible users.
                actionProvider = getActionProvider(action);
                runAction(actionProvider, eligibleIds);

                // Update the state for the users that were processed.
                stateProvider.update(policy.getId(), policy.getProviderId(), eligibleIds, action.getId());
            }
            // </comment>
        }
    }

    private void runAction(ResourceActionProvider actionProvider, List<String> newResourceIds) {
        actionProvider.run(newResourceIds == null ? List.of() : newResourceIds);
    }

    private ResourcePolicyProvider getPolicyProvider(ResourcePolicy policy) {
        ComponentFactory<?, ?> factory = (ComponentFactory<?, ?>) session.getKeycloakSessionFactory()
                .getProviderFactory(ResourcePolicyProvider.class, policy.getProviderId());
        return (ResourcePolicyProvider) factory.create(session, getRealm().getComponent(policy.getId()));
    }

    private ResourceActionProvider getActionProvider(ResourceAction action) {
        ComponentFactory<?, ?> actionFactory = (ComponentFactory<?, ?>) session.getKeycloakSessionFactory()
                .getProviderFactory(ResourceActionProvider.class, action.getProviderId());
        return (ResourceActionProvider) actionFactory.create(session, getRealm().getComponent(action.getId()));
    }

    private ResourcePolicyStateProvider getResourcePolicyStateProvider() {
        ProviderFactory<ResourcePolicyStateProvider> providerFactory = session.getKeycloakSessionFactory().getProviderFactory(ResourcePolicyStateProvider.class);
        return providerFactory.create(session);
    }

    private RealmModel getRealm() {
        return session.getContext().getRealm();
    }

    private void validateActions(List<ResourceAction> actions) {
        // the list should be in the desired priority order
        for (int i = 0; i < actions.size(); i++) {
            ResourceAction currentAction = actions.get(i);

            // check that each action's duration is positive.
            if (currentAction.getAfter() <= 0) {
                throw new BadRequestException("Validation Error: 'after' duration must be positive.");
            }

            if (i > 0) {// skip for initial action
                ResourceAction previousAction = actions.get(i - 1);
                // compare current with the previous action in the list
                if (currentAction.getAfter() < previousAction.getAfter()) {
                    throw new BadRequestException(
                        String.format("Validation Error: The 'after' duration for action #%d (%s) cannot be less than the duration of the preceding action #%d (%s).",
                            i + 1, formatDuration(currentAction.getAfter()),
                            i, formatDuration(previousAction.getAfter()))
                    );
                }
            }
        }
    }

    private String formatDuration(long millis) {
        long days = Duration.ofMillis(millis).toDays();
        if (days > 0) {
            return String.format("%d day(s)", days);
        } else {
            long hours = Duration.ofMillis(millis).toHours();
            return String.format("%d hour(s)", hours);
        }
    }

    public void removePolicies() {
        RealmModel realm = getRealm();
        realm.getComponentsStream(realm.getId(), ResourcePolicyProvider.class.getName()).forEach(policy -> {
            realm.getComponentsStream(policy.getId(), ResourceActionProvider.class.getName()).forEach(realm::removeComponent);
            realm.removeComponent(policy);
        });
    }

    public void processEvent(ResourcePolicyEvent event) {

        ResourcePolicyStateProvider state = getResourcePolicyStateProvider();
        List<String> currentlyAssignedPolicies = state.getScheduledActionsByResource(event.getResourceId())
                .stream().map(ScheduledAction::policyId).toList();
        List<ResourcePolicy> policies = this.getPolicies();

        // iterate through the policies, and for those not yet assigned to the user check if they can be assigned
        policies.stream()
                .filter(policy -> !getActions(policy).isEmpty())
                .forEach(policy -> {
                            ResourcePolicyProvider provider = getPolicyProvider(policy);
                            if (!currentlyAssignedPolicies.contains(policy.getId())) {
                                // if policy is not assigned, check if the provider allows assigning based on the event
                                if (provider.scheduleOnEvent(event)) {
                                    state.scheduleAction(policy, getFirstAction(policy), event.getResourceId());
                                }
                            } else {
                                if (provider.resetOnEvent(event)) {
                                    state.scheduleAction(policy, getFirstAction(policy), event.getResourceId());
                                }
                                // TODO add a removeOnEvent to allow policies to detach from resources on specific events (e.g. unlinking an identity)
                            }
                        });
    }

    private ResourceAction getFirstAction(ResourcePolicy policy) {
        return getActions(policy).get(0);
    }

    public void runScheduledTasks() {
        for (ResourcePolicy policy : getPolicies()) {
            ResourcePolicyStateProvider state = getResourcePolicyStateProvider();

            for (ScheduledAction scheduled : state.getDueScheduledActions(policy)) {
                List<ResourceAction> actions = getActions(policy);

                for (int i = 0; i < actions.size(); i++) {
                    ResourceAction currentAction = actions.get(i);

                    if (currentAction.getId().equals(scheduled.actionId())) {
                        runAction(getActionProvider(currentAction), List.of(scheduled.resourceId()));

                        if (actions.size() > i + 1) {
                            // schedule the next action using the time offset difference between the actions.
                            ResourceAction nextAction = actions.get(i + 1);
                            state.scheduleAction(policy, nextAction,nextAction.getAfter() - currentAction.getAfter(), scheduled.resourceId());
                        } else {
                            state.remove(policy.getId(), scheduled.resourceId());
                        }
                    }
                }
            }
        }
    }
}
