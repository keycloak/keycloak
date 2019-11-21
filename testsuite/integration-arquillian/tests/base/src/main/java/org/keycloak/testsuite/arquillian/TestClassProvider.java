package org.keycloak.testsuite.arquillian;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;
import org.jboss.logging.Logger;

import java.net.URL;

public class TestClassProvider {

    private static final Logger LOGGER = Logger.getLogger(TestClassProvider.class);

    public static final String[] PERMITTED_PACKAGES = new String[] {
            "/org/keycloak/testsuite",
            "/org/junit",
            "/org/hamcrest",
            "/org/keycloak/admin/client",
            "/org/jboss/resteasy/client",
            "/org/jboss/arquillian",
            "/org/jboss/shrinkwrap",
            "/org/openqa/selenium"
    };

    private Undertow server;

    public void start() {
        server = Undertow.builder()
                .addHttpListener(8500, "localhost")
                .setHandler(Handlers.resource(new ClassPathResourceManager()))
                .build();
        server.start();

        LOGGER.infov("Started test class provider on http://localhost:8500");
    }

    public void stop() {
        server.stop();
    }

    public class ClassPathResourceManager implements ResourceManager {

        @Override
        public Resource getResource(String className) {
            LOGGER.infov("Request: {0}", className);

            URL resource = isPermittedPackage(className) ? TestClassProvider.class.getResource(className) : null;
            return resource != null ? new URLResource(resource, className) : null;
        }

        private boolean isPermittedPackage(String className) {
            for (String p : PERMITTED_PACKAGES) {
                if (className.startsWith(p)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isResourceChangeListenerSupported() {
            return false;
        }

        @Override
        public void registerResourceChangeListener(ResourceChangeListener listener) {
            throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
        }

        @Override
        public void removeResourceChangeListener(ResourceChangeListener listener) {
            throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
        }

        @Override
        public void close() {
        }
    }

}
