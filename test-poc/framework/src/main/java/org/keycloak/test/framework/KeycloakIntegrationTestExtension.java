package org.keycloak.test.framework;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.keycloak.test.framework.injection.Registry;

public class KeycloakIntegrationTestExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        if (isExtensionEnabled(context)) {
            getRegistry(context).beforeAll(context.getRequiredTestClass());
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (isExtensionEnabled(context)) {
            getRegistry(context).beforeEach(context.getRequiredTestInstance());
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (isExtensionEnabled(context)) {
            getRegistry(context).afterAll();
        }
    }

    private boolean isExtensionEnabled(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(KeycloakIntegrationTest.class);
    }

    private Registry getRegistry(ExtensionContext context) {
        ExtensionContext.Store store = getStore(context);
        Registry registry = (Registry) store.getOrComputeIfAbsent(Registry.class, r -> new Registry());
        registry.setCurrentContext(context);
        return registry;
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        while (context.getParent().isPresent()) {
            context = context.getParent().get();
        }
        return context.getStore(ExtensionContext.Namespace.create(getClass()));
    }

}
