package org.keycloak.testframework.remote;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.testframework.TestFrameworkExecutor;
import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Registry;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.runonserver.RunOnServerSupplier;
import org.keycloak.testframework.remote.runonserver.RunTestOnServer;
import org.keycloak.testframework.remote.runonserver.TestClassServerSupplier;
import org.keycloak.testframework.remote.timeoffset.TimeOffsetSupplier;

public class RemoteTestFrameworkExtension implements TestFrameworkExtension, TestFrameworkExecutor {
    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(
                new TimeOffsetSupplier(),
                new RunOnServerSupplier(),
                new RemoteProvidersSupplier(),
                new TestClassServerSupplier()
        );
    }

    @Override
    public List<Class<?>> alwaysEnabledValueTypes() {
        return List.of(RemoteProviders.class);
    }

    @Override
    public List<Class<?>> getMethodValueTypes(Method method) {
        return isTestOnServer(method) ? List.of(RunOnServerClient.class) : Collections.emptyList();
    }

    @Override
    public boolean supportsParameter(Method method, Class<?> parameterType) {
        return isTestOnServer(method) && parameterType.equals(KeycloakSession.class);
    }

    @Override
    public boolean shouldExecute(Method testMethod) {
        return isTestOnServer(testMethod);
    }

    @Override
    public void execute(Registry registry, Class<?> testClass, Method testMethod) {
        RunOnServerClient value = (RunOnServerClient) registry.getDeployedInstances().stream().filter(i -> i.getRequestedValueType() != null && i.getRequestedValueType().equals(RunOnServerClient.class)).findFirst().get().getValue();

        RunTestOnServer runTestOnServer = new RunTestOnServer(testClass.getName(), testMethod.getName());
        value.run(runTestOnServer);
    }

    private boolean isTestOnServer(Method method) {
        return method.isAnnotationPresent(TestOnServer.class);
    }

}
