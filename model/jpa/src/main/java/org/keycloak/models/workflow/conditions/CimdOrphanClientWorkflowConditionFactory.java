package org.keycloak.models.workflow.conditions;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;

public class CimdOrphanClientWorkflowConditionFactory implements WorkflowConditionProviderFactory<CimdOrphanClientWorkflowConditionProvider> {

    public static final String ID = "cimd-orphan-client";

    @Override
    public CimdOrphanClientWorkflowConditionProvider create(KeycloakSession session, String thresholdInSeconds) {
        return new CimdOrphanClientWorkflowConditionProvider(session, thresholdInSeconds);
    }

    @Override
    public String getId() {
        return ID;
    }
}
