package org.keycloak.models.workflow;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.keycloak.Config.Scope;
import org.keycloak.component.ComponentModel;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class DefaultWorkflowProviderFactory implements WorkflowProviderFactory<DefaultWorkflowProvider> {

    static final String ID = "default";
    private static final long DEFAULT_EXECUTOR_TASK_TIMEOUT = 1000L;

    private WorkflowExecutor executor;
    private boolean blocking;
    private long taskTimeout;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public DefaultWorkflowProvider create(KeycloakSession session, ComponentModel model) {
        return new DefaultWorkflowProvider(session, executor);
    }

    @Override
    public DefaultWorkflowProvider create(KeycloakSession session) {
        return new DefaultWorkflowProvider(session, executor);
    }

    @Override
    public void init(Scope config) {
        blocking = config.getBoolean("executorBlocking", false);
        taskTimeout = config.getLong("executorTaskTimeout", DEFAULT_EXECUTOR_TASK_TIMEOUT);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.executor = new WorkflowExecutor(getTaskExecutor(factory), blocking, taskTimeout);
    }

    @Override
    public void close() {

    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("executor-task-timeout")
                .type("long")
                .helpText("The time in milliseconds before a workflow task is marked as timed out .")
                .defaultValue(DEFAULT_EXECUTOR_TASK_TIMEOUT)
                .add().build();
    }

    private ExecutorService getTaskExecutor(KeycloakSessionFactory factory) {
        return factory.getProviderFactory(ExecutorsProvider.class).create(null).getExecutor("workflow-event-executor");
    }
}
