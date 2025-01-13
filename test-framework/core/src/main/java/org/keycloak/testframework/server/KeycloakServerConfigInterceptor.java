package org.keycloak.testframework.server;

import org.keycloak.testframework.injection.InstanceContext;

import java.lang.annotation.Annotation;

public interface KeycloakServerConfigInterceptor<T, S extends Annotation> {

    KeycloakServerConfigBuilder intercept(KeycloakServerConfigBuilder serverConfig, InstanceContext<T, S> instanceContext);

}
