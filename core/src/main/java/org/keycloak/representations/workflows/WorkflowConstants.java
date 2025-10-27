package org.keycloak.representations.workflows;

public final class WorkflowConstants {

    public static final String DEFAULT_WORKFLOW = "event-based-workflow";

    public static final String CONFIG_USES = "uses";
    public static final String CONFIG_WITH = "with";

    // Entry configuration keys for Workflow
    public static final String CONFIG_ON_EVENT = "on";
    public static final String CONFIG_CONCURRENCY = "concurrency";
    public static final String CONFIG_CANCEL_IF_RUNNING = "cancel-if-running";
    public static final String CONFIG_NAME = "name";
    public static final String CONFIG_ENABLED = "enabled";
    public static final String CONFIG_CONDITIONS = "conditions";
    public static final String CONFIG_STEPS = "steps";
    public static final String CONFIG_ERROR = "error";
    public static final String CONFIG_STATE = "state";

    // Entry configuration keys for WorkflowCondition
    public static final String CONFIG_IF = "if";

    // Entry configuration keys for WorkflowStep
    public static final String CONFIG_AFTER = "after";
    public static final String CONFIG_PRIORITY = "priority";
}
