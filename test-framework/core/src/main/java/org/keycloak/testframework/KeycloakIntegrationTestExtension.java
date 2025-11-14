package org.keycloak.testframework;

import java.util.Optional;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.Registry;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

public class KeycloakIntegrationTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback, TestWatcher {

    private static final LogHandler logHandler = new LogHandler();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (isExtensionEnabled(context)) {
            logHandler.beforeAll(context);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (isExtensionEnabled(context)) {
            logHandler.beforeEachStarting(context);
            getRegistry(context).beforeEach(context.getRequiredTestInstance());
            logHandler.beforeEachCompleted(context);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (isExtensionEnabled(context)) {
            logHandler.afterEachStarting(context);
            getRegistry(context).afterEach();
            logHandler.afterEachCompleted(context);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (isExtensionEnabled(context)) {
            logHandler.afterAll(context);
            getRegistry(context).afterAll();
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (isExtensionEnabled(context)) {
            logHandler.testFailed(context);
        }
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        if (isExtensionEnabled(context)) {
            logHandler.testDisabled(context);
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        if (isExtensionEnabled(context)) {
            logHandler.testSuccessful(context);
        }
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        if (isExtensionEnabled(context)) {
            logHandler.testAborted(context);
        }
    }

    private boolean isExtensionEnabled(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(KeycloakIntegrationTest.class);
    }

    private Registry getRegistry(ExtensionContext context) {
        ExtensionContext.Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        Registry registry = (Registry) store.getOrComputeIfAbsent(Registry.class, r -> new Registry());
        registry.setCurrentContext(context);
        return registry;
    }

}
