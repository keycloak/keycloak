package org.keycloak.testframework;

import java.lang.reflect.Method;
import java.util.Optional;

import org.keycloak.testframework.injection.Registry;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestWatcher;

public class KeycloakIntegrationTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback, TestWatcher, InvocationInterceptor, ParameterResolver {

    private static final LogHandler logHandler = new LogHandler();

    @Override
    public void beforeAll(ExtensionContext context) {
        logHandler.beforeAll(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        logHandler.beforeEachStarting(context);
        getRegistry(context).beforeEach(context.getRequiredTestInstance(), context.getRequiredTestMethod());
        logHandler.beforeEachCompleted(context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        logHandler.afterEachStarting(context);
        getRegistry(context).afterEach();
        logHandler.afterEachCompleted(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        logHandler.afterAll(context);
        getRegistry(context).afterAll();
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        logHandler.testFailed(context);
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        logHandler.testDisabled(context);
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        logHandler.testSuccessful(context);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        logHandler.testAborted(context);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        getRegistry(extensionContext).intercept(invocation, invocationContext);
    }

    public static Registry getRegistry(ExtensionContext context) {
        ExtensionContext.Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        Registry registry = (Registry) store.getOrComputeIfAbsent(Registry.class, r -> new Registry());
        registry.setCurrentContext(context);
        return registry;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        return getRegistry(context).supportsParameter(parameterContext, context);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        // As this is only used by custom test executors for now they are responsible for injecting the parameter, hence returning null here
        return null;
    }
}
