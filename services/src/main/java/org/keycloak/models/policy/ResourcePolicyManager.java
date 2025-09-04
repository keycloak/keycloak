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

import static java.util.Optional.ofNullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.policy.ResourcePolicyStateProvider.ScheduledAction;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.resources.policies.ResourcePolicyActionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyConditionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;

public class ResourcePolicyManager {

    private static final Logger log = Logger.getLogger(ResourcePolicyManager.class);

    private final ResourcePolicyStateProvider policyStateProvider;

    public static boolean isFeatureEnabled() {
        return Profile.isFeatureEnabled(Feature.RESOURCE_LIFECYCLE);
    }

    private final KeycloakSession session;

    public ResourcePolicyManager(KeycloakSession session) {
        this.session = session;
        this.policyStateProvider = session.getKeycloakSessionFactory().getProviderFactory(ResourcePolicyStateProvider.class).create(session);
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
        List<ResourceAction> oldActions = getActions(policy.getId());
        Set<String> oldActionIds = oldActions.stream()
                .map(ResourceAction::getId)
                .collect(Collectors.toSet());

        // find which action IDs were deleted
        oldActionIds.removeAll(newActionIds); // The remaining IDs are the deleted ones

        // delete orphaned state records - this means that we actually reset the flow for users which completed the action which is being removed
        // it seems like the best way to handle this
        if (!oldActionIds.isEmpty()) {
            policyStateProvider.removeByCompletedActions(policy.getId(), oldActionIds);
        }

        RealmModel realm = getRealm();
        // remove all existing actions of the policy
        realm.removeComponents(policy.getId());

        // add the new actions
        for (int i = 0; i < actions.size(); i++) {
            ResourceAction action = actions.get(i);

            // assign priority based on index.
            action.setPriority(i + 1);

            List<ResourceAction> subActions = Optional.ofNullable(action.getActions()).orElse(List.of());

            // persist the new action component.
            action = addAction(policy.getId(), action);

            for (int j = 0; j < subActions.size(); j++) {
                ResourceAction subAction = subActions.get(j);
                // assign priority based on index.
                subAction.setPriority(j + 1);
                addAction(action.getId(), subAction);
            }
        }
    }

    private ResourceAction addAction(String parentId, ResourceAction action) {
        RealmModel realm = getRealm();
        ComponentModel policyModel = realm.getComponent(parentId);
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

    public List<ResourceAction> getActions(String policyId) {
        return getActionsStream(policyId).toList();
    }

    public Stream<ResourceAction> getActionsStream(String parentId) {
        RealmModel realm = session.getContext().getRealm();
        return realm.getComponentsStream(parentId, ResourceActionProvider.class.getName())
                .map(this::toResourceAction).sorted();
    }

    private ResourceAction toResourceAction(ComponentModel model) {
        ResourceAction action = new ResourceAction(model);

        action.setActions(getActions(action.getId()));

        return action;
    }

    public ResourceAction getActionById(KeycloakSession session, String id) {
        RealmModel realm = session.getContext().getRealm();
        ComponentModel component = realm.getComponent(id);

        if (component == null) {
            return null;
        }

        return toResourceAction(component);
    }

    private ResourceAction getFirstAction(ResourcePolicy policy) {
        return getActions(policy.getId()).get(0);
    }

    private ResourcePolicyProvider getPolicyProvider(ResourcePolicy policy) {
        ComponentFactory<?, ?> factory = (ComponentFactory<?, ?>) session.getKeycloakSessionFactory()
                .getProviderFactory(ResourcePolicyProvider.class, policy.getProviderId());
        return (ResourcePolicyProvider) factory.create(session, getRealm().getComponent(policy.getId()));
    }

    public ResourceActionProvider getActionProvider(ResourceAction action) {
        RealmModel realm = session.getContext().getRealm();
        ComponentFactory<?, ?> actionFactory = (ComponentFactory<?, ?>) session.getKeycloakSessionFactory()
                .getProviderFactory(ResourceActionProvider.class, action.getProviderId());
        return (ResourceActionProvider) actionFactory.create(session, realm.getComponent(action.getId()));
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

    public void scheduleAllEligibleResources(ResourcePolicy policy) {
        if (policy.isEnabled()) {
            ResourcePolicyProvider provider = getPolicyProvider(policy);
            ResourceAction firstAction = getFirstAction(policy);
            provider.getEligibleResourcesForInitialAction().forEach(resourceId -> {
                // TODO run each scheduling task in a separate tx as other txs might schedule an action while this is running.
                this.policyStateProvider.scheduleAction(policy, firstAction, resourceId);
            });
        }
    }

    public void processEvent(ResourcePolicyEvent event) {

        List<String> currentlyAssignedPolicies = policyStateProvider.getScheduledActionsByResource(event.getResourceId())
                .stream().map(ScheduledAction::policyId).toList();
        List<ResourcePolicy> policies = this.getPolicies();

        // iterate through the policies, and for those not yet assigned to the user check if they can be assigned
        policies.stream()
                .filter(policy -> policy.isEnabled() && !getActions(policy.getId()).isEmpty())
                .forEach(policy -> {
                    ResourcePolicyProvider provider = getPolicyProvider(policy);
                    if (!currentlyAssignedPolicies.contains(policy.getId())) {
                        // if policy is not active for the resource, check if the provider allows activating based on the event
                        if (provider.activateOnEvent(event)) {
                            if (policy.isScheduled()) {
                                // policy is scheduled, so we schedule the first action
                                log.debugf("Scheduling first action of policy %s for resource %s based on event %s",
                                        policy.getId(), event.getResourceId(), event.getOperation());
                                policyStateProvider.scheduleAction(policy, getFirstAction(policy), event.getResourceId());
                            } else {
                                // policy is not scheduled, so we run all actions immediately
                                log.debugf("Running all actions of policy %s for resource %s based on event %s",
                                        policy.getId(), event.getResourceId(), event.getOperation());
                                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s -> {
                                    getActions(policy.getId()).forEach(action -> getActionProvider(action).run(List.of(event.getResourceId())));
                                });
                            }
                        }
                    } else {
                        if (provider.resetOnEvent(event)) {
                            policyStateProvider.scheduleAction(policy, getFirstAction(policy), event.getResourceId());
                        } else if (provider.deactivateOnEvent(event)) {
                            policyStateProvider.remove(policy.getId(), event.getResourceId());
                        }
                    }
                });
    }

    public void runScheduledActions() {
            this.getPolicies().stream().filter(ResourcePolicy::isEnabled).forEach(policy -> {

            for (ScheduledAction scheduled : policyStateProvider.getDueScheduledActions(policy)) {
                List<ResourceAction> actions = getActions(policy.getId());

                for (int i = 0; i < actions.size(); i++) {
                    ResourceAction currentAction = actions.get(i);

                    if (currentAction.getId().equals(scheduled.actionId())) {
                        getActionProvider(currentAction).run(List.of(scheduled.resourceId()));

                        if (actions.size() > i + 1) {
                            // schedule the next action using the time offset difference between the actions.
                            ResourceAction nextAction = actions.get(i + 1);
                            policyStateProvider.scheduleAction(policy, nextAction, nextAction.getAfter() - currentAction.getAfter(), scheduled.resourceId());
                        } else {
                            // this was the last action, check if the policy is recurring - i.e. if we need to schedule the first action again
                            if (policy.isRecurring()) {
                                ResourceAction firstAction = getFirstAction(policy);
                                policyStateProvider.scheduleAction(policy, firstAction, scheduled.resourceId());
                            } else {
                                // not recurring, remove the state record
                                policyStateProvider.remove(policy.getId(), scheduled.resourceId());
                            }
                        }
                    }
                }
            }
        });
    }

    public void removePolicy(String id) {
        RealmModel realm = getRealm();
        realm.getComponentsStream(realm.getId(), ResourcePolicyProvider.class.getName())
                .filter(policy -> policy.getId().equals(id))
                .forEach(policy -> {
                    realm.getComponentsStream(policy.getId(), ResourceActionProvider.class.getName()).forEach(realm::removeComponent);
                    realm.removeComponent(policy);
                });
    }

    public ResourcePolicy getPolicy(String id) {
        return new ResourcePolicy(getPolicyComponent(id));
    }

    public void updatePolicy(ResourcePolicy policy, MultivaluedHashMap<String, String> config) {
        ComponentModel component = getPolicyComponent(policy.getId());
        component.setConfig(config);
        getRealm().updateComponent(component);
    }

    private ComponentModel getPolicyComponent(String id) {
        ComponentModel component = getRealm().getComponent(id);

        if (component == null || !ResourcePolicyProvider.class.getName().equals(component.getProviderType())) {
            throw new BadRequestException("Not a valid resource policy: " + id);
        }

        return component;
    }

    public ResourcePolicyRepresentation toRepresentation(ResourcePolicy policy) {
        ResourcePolicyRepresentation rep = new ResourcePolicyRepresentation(policy.getId(), policy.getProviderId(), policy.getConfig());

        for (ResourceAction action : getActions(policy.getId())) {
            rep.addAction(toRepresentation(action));
        }

        return rep;
    }

    private ResourcePolicyActionRepresentation toRepresentation(ResourceAction action) {
        List<ResourcePolicyActionRepresentation> actions = action.getActions().stream().map(this::toRepresentation).toList();
        return new ResourcePolicyActionRepresentation(action.getId(), action.getProviderId(), action.getConfig(), actions);
    }

    public ResourcePolicy toModel(ResourcePolicyRepresentation rep) {
        ResourcePolicyManager manager = new ResourcePolicyManager(session);
        MultivaluedHashMap<String, String> config = ofNullable(rep.getConfig()).orElse(new MultivaluedHashMap<>());

        for (ResourcePolicyConditionRepresentation condition : rep.getConditions()) {
            String conditionProviderId = condition.getProviderId();
            config.computeIfAbsent("conditions", key -> new ArrayList<>()).add(conditionProviderId);

            for (Entry<String, List<String>> configEntry : condition.getConfig().entrySet()) {
                config.put(conditionProviderId + "." + configEntry.getKey(), configEntry.getValue());
            }
        }

        ResourcePolicy policy = manager.addPolicy(rep.getProviderId(), config);
        List<ResourceAction> actions = new ArrayList<>();

        for (ResourcePolicyActionRepresentation actionRep : rep.getActions()) {
            actions.add(toModel(actionRep));
        }

        manager.updateActions(policy, actions);

        return policy;
    }

    private ResourceAction toModel(ResourcePolicyActionRepresentation rep) {
        List<ResourceAction> subActions = new ArrayList<>();

        for (ResourcePolicyActionRepresentation subAction : ofNullable(rep.getActions()).orElse(List.of())) {
            subActions.add(toModel(subAction));
        }

        return new ResourceAction(rep.getProviderId(), rep.getConfig(), subActions);
    }
}
