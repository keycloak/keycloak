package org.keycloak.testframework.server;

import java.lang.annotation.Annotation;

import org.keycloak.testframework.injection.InstanceContext;

public interface KeycloakServerConfigInterceptor<T, S extends Annotation> {

    KeycloakServerConfigBuilder intercept(KeycloakServerConfigBuilder serverConfig, InstanceContext<T, S> instanceContext);

}
