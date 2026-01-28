package org.keycloak.testframework;

import java.util.Optional;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.infinispan.util.function.SerializableComparator;
import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

public class ServerConfigClassOrderer implements ClassOrderer {

    @Override
    public void orderClasses(ClassOrdererContext classOrdererContext) {
        classOrdererContext.getClassDescriptors().sort(new ServerConfigComparator());
    }

    static class ServerConfigComparator implements SerializableComparator<ClassDescriptor> {

        @Override
        public int compare(ClassDescriptor o1, ClassDescriptor o2) {
            Optional<KeycloakIntegrationTest> a1 = o1.findAnnotation(KeycloakIntegrationTest.class);
            Optional<KeycloakIntegrationTest> a2 = o2.findAnnotation(KeycloakIntegrationTest.class);

            if (a1.isPresent() && a2.isPresent()) {
                return a1.get().config().getName().compareTo(a2.get().config().getName());
            } else if (a1.isPresent()) {
                return 1;
            } else if (a2.isPresent()) {
                return 2;
            } else {
                return 0;
            }
        }

    }

}
