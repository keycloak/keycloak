package org.keycloak.testframework.conditions;

import java.lang.annotation.Annotation;

import org.keycloak.testframework.server.KeycloakServer;

class DisabledForServersCondition extends AbstractDisabledForSupplierCondition {

    @Override
    protected Class<?> valueType() {
        return KeycloakServer.class;
    }

    @Override
    protected Class<? extends Annotation> annotation() {
        return DisabledForServers.class;
    }

}
