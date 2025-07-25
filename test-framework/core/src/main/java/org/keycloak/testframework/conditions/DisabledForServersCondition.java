package org.keycloak.testframework.conditions;

import org.keycloak.testframework.server.KeycloakServer;

import java.lang.annotation.Annotation;

class DisabledForServersCondition extends AbstractDisabledForSupplierCondition {

    @Override
    Class<?> valueType() {
        return KeycloakServer.class;
    }

    Class<? extends Annotation> annotation() {
        return DisabledForServers.class;
    }

}
