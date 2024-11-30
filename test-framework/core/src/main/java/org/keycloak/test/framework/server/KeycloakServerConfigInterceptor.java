package org.keycloak.test.framework.server;

import org.keycloak.test.framework.injection.InstanceContext;

import java.lang.annotation.Annotation;

public interface KeycloakServerConfigInterceptor<T, S extends Annotation> {

    KeycloakServerConfigBuilder intercept(KeycloakServerConfigBuilder serverConfig, InstanceContext<T, S> instanceContext);

}
