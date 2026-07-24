package org.keycloak.testframework.conditions;

import java.lang.annotation.Annotation;

import org.keycloak.testframework.server.KeycloakServer;

class DisabledForServersCondition extends AbstractDisabledForSupplierCondition {

    @Override
    Class<?> valueType() {
        return KeycloakServer.class;
    }

    Class<? extends Annotation> annotation() {
        return DisabledForServers.class;
    }

}
