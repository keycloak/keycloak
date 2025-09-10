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

package org.keycloak.models.workflow;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
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
import org.keycloak.models.workflow.WorkflowStateProvider.ScheduledAction;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.workflows.WorkflowActionRepresentation;
import org.keycloak.representations.workflows.WorkflowConditionRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;

public class WorkflowsManager {

    private static final Logger log = Logger.getLogger(WorkflowsManager.class);

    private final KeycloakSession session;
    private final WorkflowStateProvider workflowStateProvider;

    public static boolean isFeatureEnabled() {
        return Profile.isFeatureEnabled(Feature.WORKFLOWS);
    }

    public WorkflowsManager(KeycloakSession session) {
        this.session = session;
        this.workflowStateProvider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowStateProvider.class).create(session);
    }

    public Workflow addWorkflow(String providerId) {
        return addWorkflow(new Workflow(providerId));
    }

    public Workflow addWorkflow(String providerId, Map<String, List<String>> config) {
        return addWorkflow(new Workflow(providerId, config));
    }

    public Workflow addWorkflow(Workflow workflow) {
        RealmModel realm = getRealm();
        ComponentModel model = new ComponentModel();

        model.setParentId(realm.getId());
        model.setProviderId(workflow.getProviderId());
        model.setProviderType(WorkflowProvider.class.getName());

        MultivaluedHashMap<String, String> config = workflow.getConfig();

        if (config != null) {
            model.setConfig(config);
        }

        return new Workflow(realm.addComponentModel(model));
    }

    // This method takes an ordered list of actions. First action in the list has the highest priority, last action has the lowest priority
    public void createActions(Workflow workflow, List<WorkflowAction> actions) {
        for (int i = 0; i < actions.size(); i++) {
            WorkflowAction action = actions.get(i);

            // assign priority based on index.
            action.setPriority(i + 1);

            List<WorkflowAction> subActions = Optional.ofNullable(action.getActions()).orElse(List.of());

            // persist the new action component.
            action = addAction(workflow.getId(), action);

            for (int j = 0; j < subActions.size(); j++) {
                WorkflowAction subAction = subActions.get(j);
                // assign priority based on index.
                subAction.setPriority(j + 1);
                addAction(action.getId(), subAction);
            }
        }
    }

    private WorkflowAction addAction(String parentId, WorkflowAction action) {
        RealmModel realm = getRealm();
        ComponentModel workflowModel = realm.getComponent(parentId);
        ComponentModel actionModel = new ComponentModel();

        actionModel.setId(action.getId());//need to keep stable UUIDs not to break a link in state table
        actionModel.setParentId(workflowModel.getId());
        actionModel.setProviderId(action.getProviderId());
        actionModel.setProviderType(WorkflowActionProvider.class.getName());
        actionModel.setConfig(action.getConfig());

        return new WorkflowAction(realm.addComponentModel(actionModel));
    }

    public List<Workflow> getWorkflows() {
        RealmModel realm = getRealm();
        return realm.getComponentsStream(realm.getId(), WorkflowProvider.class.getName())
                .map(Workflow::new).toList();
    }

    public List<WorkflowAction> getActions(String workflowId) {
        return getActionsStream(workflowId).toList();
    }

    public Stream<WorkflowAction> getActionsStream(String parentId) {
        RealmModel realm = session.getContext().getRealm();
        return realm.getComponentsStream(parentId, WorkflowActionProvider.class.getName())
                .map(this::toResourceAction).sorted();
    }

    private WorkflowAction toResourceAction(ComponentModel model) {
        WorkflowAction action = new WorkflowAction(model);

        action.setActions(getActions(action.getId()));

        return action;
    }

    public WorkflowAction getActionById(KeycloakSession session, String id) {
        RealmModel realm = session.getContext().getRealm();
        ComponentModel component = realm.getComponent(id);

        if (component == null) {
            return null;
        }

        return toResourceAction(component);
    }

    private WorkflowAction getFirstAction(Workflow workflow) {
        WorkflowAction action = getActions(workflow.getId()).get(0);
        Long notBefore = workflow.getNotBefore();

        if (notBefore != null) {
            action.setAfter(notBefore);
        }

        return action;
    }

    private WorkflowProvider getWorkflowProvider(Workflow workflow) {
        ComponentFactory<?, ?> factory = (ComponentFactory<?, ?>) session.getKeycloakSessionFactory()
                .getProviderFactory(WorkflowProvider.class, workflow.getProviderId());
        return (WorkflowProvider) factory.create(session, getRealm().getComponent(workflow.getId()));
    }

    public WorkflowActionProvider getActionProvider(WorkflowAction action) {
        ComponentFactory<?, ?> actionFactory = (ComponentFactory<?, ?>) session.getKeycloakSessionFactory()
                .getProviderFactory(WorkflowActionProvider.class, action.getProviderId());
        return (WorkflowActionProvider) actionFactory.create(session, getRealm().getComponent(action.getId()));
    }

    private RealmModel getRealm() {
        return session.getContext().getRealm();
    }

    public void removeWorkflows() {
        RealmModel realm = getRealm();
        realm.getComponentsStream(realm.getId(), WorkflowProvider.class.getName()).forEach(workflow -> {
            realm.getComponentsStream(workflow.getId(), WorkflowActionProvider.class.getName()).forEach(realm::removeComponent);
            realm.removeComponent(workflow);
        });
    }

    public void scheduleAllEligibleResources(Workflow workflow) {
        if (workflow.isEnabled()) {
            WorkflowProvider provider = getWorkflowProvider(workflow);
            provider.getEligibleResourcesForInitialAction()
                    .forEach(resourceId -> processEvent(List.of(workflow), new AdhocWorkflowEvent(ResourceType.USERS, resourceId)));
        }
    }

    public void processEvent(WorkflowEvent event) {
        processEvent(getWorkflows(), event);
    }

    public void processEvent(List<Workflow> workflows, WorkflowEvent event) {
        List<String> currentlyAssignedWorkflows = workflowStateProvider.getScheduledActionsByResource(event.getResourceId())
                .stream().map(ScheduledAction::workflowId).toList();

        // iterate through the workflows, and for those not yet assigned to the user check if they can be assigned
        workflows.stream()
                .filter(workflow -> workflow.isEnabled() && !getActions(workflow.getId()).isEmpty())
                .forEach(workflow -> {
                    WorkflowProvider provider = getWorkflowProvider(workflow);
                    try {
                        if (!currentlyAssignedWorkflows.contains(workflow.getId())) {
                            // if workflow is not active for the resource, check if the provider allows activating based on the event
                            if (provider.activateOnEvent(event)) {
                                if (workflow.isScheduled()) {
                                    // workflow is scheduled, so we schedule the first action
                                    log.debugf("Scheduling first action of workflow %s for resource %s based on event %s",
                                            workflow.getId(), event.getResourceId(), event.getOperation());
                                    workflowStateProvider.scheduleAction(workflow, getFirstAction(workflow), event.getResourceId());
                                } else {
                                    // workflow is not scheduled, so we run all actions immediately
                                    log.debugf("Running all actions of workflow %s for resource %s based on event %s",
                                            workflow.getId(), event.getResourceId(), event.getOperation());
                                    KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s -> {
                                        getActions(workflow.getId()).forEach(action -> getActionProvider(action).run(List.of(event.getResourceId())));
                                    });
                                }
                            }
                        } else {
                            if (provider.resetOnEvent(event)) {
                                workflowStateProvider.scheduleAction(workflow, getFirstAction(workflow), event.getResourceId());
                            } else if (provider.deactivateOnEvent(event)) {
                                workflowStateProvider.remove(workflow.getId(), event.getResourceId());
                            }
                        }
                    } catch (WorkflowInvalidStateException e) {
                        workflow.getConfig().putSingle("enabled", "false");
                        workflow.getConfig().putSingle("validation_error", e.getMessage());
                        updateWorkflow(workflow, workflow.getConfig());
                        log.debugf("Workflow %s was disabled due to: %s", workflow.getId(), e.getMessage());
                    }
                });
    }

    public void runScheduledActions() {
            this.getWorkflows().stream().filter(Workflow::isEnabled).forEach(workflow -> {

            for (ScheduledAction scheduled : workflowStateProvider.getDueScheduledActions(workflow)) {
                List<WorkflowAction> actions = getActions(workflow.getId());

                for (int i = 0; i < actions.size(); i++) {
                    WorkflowAction currentAction = actions.get(i);

                    if (currentAction.getId().equals(scheduled.actionId())) {
                        getActionProvider(currentAction).run(List.of(scheduled.resourceId()));

                        if (actions.size() > i + 1) {
                            // schedule the next action using the time offset difference between the actions.
                            WorkflowAction nextAction = actions.get(i + 1);
                            workflowStateProvider.scheduleAction(workflow, nextAction, nextAction.getAfter() - currentAction.getAfter(), scheduled.resourceId());
                        } else {
                            // this was the last action, check if the workflow is recurring - i.e. if we need to schedule the first action again
                            if (workflow.isRecurring()) {
                                WorkflowAction firstAction = getFirstAction(workflow);
                                workflowStateProvider.scheduleAction(workflow, firstAction, scheduled.resourceId());
                            } else {
                                // not recurring, remove the state record
                                workflowStateProvider.remove(workflow.getId(), scheduled.resourceId());
                            }
                        }
                    }
                }
            }
        });
    }

    public void removeWorkflow(String id) {
        RealmModel realm = getRealm();
        realm.getComponentsStream(realm.getId(), WorkflowProvider.class.getName())
                .filter(workflow -> workflow.getId().equals(id))
                .forEach(workflow -> {
                    realm.getComponentsStream(workflow.getId(), WorkflowActionProvider.class.getName()).forEach(realm::removeComponent);
                    realm.removeComponent(workflow);
                });
        workflowStateProvider.remove(id);
    }

    public Workflow getWorkflow(String id) {
        return new Workflow(getWorkflowComponent(id));
    }

    public void updateWorkflow(Workflow workflow, MultivaluedHashMap<String, String> config) {
        ComponentModel component = getWorkflowComponent(workflow.getId());
        component.setConfig(config);
        getRealm().updateComponent(component);
    }

    private ComponentModel getWorkflowComponent(String id) {
        ComponentModel component = getRealm().getComponent(id);

        if (component == null || !WorkflowProvider.class.getName().equals(component.getProviderType())) {
            throw new BadRequestException("Not a valid resource workflow: " + id);
        }

        return component;
    }

    public WorkflowRepresentation toRepresentation(Workflow workflow) {
        WorkflowRepresentation rep = new WorkflowRepresentation(workflow.getId(), workflow.getProviderId(), workflow.getConfig());

        for (WorkflowAction action : getActions(workflow.getId())) {
            rep.addAction(toRepresentation(action));
        }

        return rep;
    }

    private WorkflowActionRepresentation toRepresentation(WorkflowAction action) {
        List<WorkflowActionRepresentation> actions = action.getActions().stream().map(this::toRepresentation).toList();
        return new WorkflowActionRepresentation(action.getId(), action.getProviderId(), action.getConfig(), actions);
    }

    public Workflow toModel(WorkflowRepresentation rep) {
        MultivaluedHashMap<String, String> config = ofNullable(rep.getConfig()).orElse(new MultivaluedHashMap<>());
        List<WorkflowConditionRepresentation> conditions = ofNullable(rep.getConditions()).orElse(List.of());

        for (WorkflowConditionRepresentation condition : conditions) {
            String conditionProviderId = condition.getProviderId();
            config.computeIfAbsent("conditions", key -> new ArrayList<>()).add(conditionProviderId);

            for (Entry<String, List<String>> configEntry : condition.getConfig().entrySet()) {
                config.put(conditionProviderId + "." + configEntry.getKey(), configEntry.getValue());
            }
        }

        Workflow workflow = addWorkflow(rep.getProviderId(), config);
        List<WorkflowAction> actions = new ArrayList<>();

        for (WorkflowActionRepresentation actionRep : rep.getActions()) {
            actions.add(toModel(actionRep));
        }

        createActions(workflow, actions);

        return workflow;
    }

    private WorkflowAction toModel(WorkflowActionRepresentation rep) {
        List<WorkflowAction> subActions = new ArrayList<>();

        for (WorkflowActionRepresentation subAction : ofNullable(rep.getActions()).orElse(List.of())) {
            subActions.add(toModel(subAction));
        }

        return new WorkflowAction(rep.getProviderId(), rep.getConfig(), subActions);
    }

    public void bind(Workflow policy, ResourceType type, String resourceId) {
        processEvent(List.of(policy), new AdhocWorkflowEvent(type, resourceId));
    }

    public Object resolveResource(ResourceType type, String resourceId) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(type, "resourceId");
        return type.resolveResource(session, resourceId);
    }
}
