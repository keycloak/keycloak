package org.keycloak.exportimport.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStep;
import org.keycloak.models.workflow.WorkflowStepProvider;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;

/** Converts workflow definitions through their provider rather than the generic component importer. */
public final class WorkflowExportImportUtils {

    private WorkflowExportImportUtils() {
    }

    public static List<WorkflowRepresentation> exportWorkflows(KeycloakSession session, RealmModel realm) {
        return withProvider(session, realm, provider -> provider.getWorkflows().map(provider::toRepresentation).toList());
    }

    private static <T> T withProvider(KeycloakSession session, RealmModel realm, Function<WorkflowProvider, T> action) {
        RealmModel previousRealm = session.getContext().getRealm();
        session.getContext().setRealm(realm);
        // A session may export several realms; a cached provider retains its original realm.
        try {
            WorkflowProvider provider = session.getKeycloakSessionFactory().getProviderFactory(WorkflowProvider.class).create(session);
            try {
                return action.apply(provider);
            } finally {
                provider.close();
            }
        } finally {
            session.getContext().setRealm(previousRealm);
        }
    }

    public static void importWorkflows(KeycloakSession session, RealmModel realm, RealmRepresentation representation) {
        List<WorkflowRepresentation> workflows = new ArrayList<>();
        if (representation.getWorkflows() != null) {
            workflows.addAll(representation.getWorkflows());
        }

        if (representation.getComponents() != null) {
            List<ComponentExportRepresentation> legacy = representation.getComponents().get(WorkflowProvider.class.getName());
            if (legacy != null && !legacy.isEmpty()) {
                if (!workflows.isEmpty()) {
                    throw new ModelException("Realm contains both workflow definitions and legacy workflow components");
                }
                legacy.stream().map(WorkflowExportImportUtils::fromComponent).forEach(workflows::add);
            }
        }

        if (workflows.isEmpty()) {
            return;
        }
        if (!Profile.isFeatureEnabled(Profile.Feature.WORKFLOWS)) {
            throw new ModelException("Cannot import workflow definitions when the workflows feature is disabled");
        }

        withProvider(session, realm, provider -> {
            workflows.forEach(provider::toModel);
            return null;
        });
    }

    private static WorkflowRepresentation fromComponent(ComponentExportRepresentation component) {
        if (!"default".equals(component.getProviderId()) || component.getConfig() == null || component.getConfig().isEmpty()) {
            throw new ModelException("Legacy workflow component has an unsupported provider or missing configuration");
        }

        List<WorkflowStepRepresentation> steps = new ArrayList<>();
        if (component.getSubComponents() != null) {
            component.getSubComponents().forEach((type, components) -> {
                if (!WorkflowStepProvider.class.getName().equals(type)) {
                    throw new ModelException("Unsupported workflow sub-component type: " + type);
                }
                components.stream().sorted(Comparator.comparingInt(step ->
                        new WorkflowStep(step.getProviderId(), step.getConfig()).getPriority()))
                        .forEach(step -> {
                            if (step.getSubComponents() != null && !step.getSubComponents().isEmpty()) {
                                throw new ModelException("Workflow steps cannot contain sub-components");
                            }
                            steps.add(new WorkflowStepRepresentation(step.getId(), step.getProviderId(), step.getConfig()));
                        });
            });
        }
        return new WorkflowRepresentation(component.getId(), component.getConfig().getFirst("name"), component.getConfig(), steps);
    }
}
