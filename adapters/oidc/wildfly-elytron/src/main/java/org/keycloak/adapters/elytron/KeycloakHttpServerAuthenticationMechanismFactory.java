/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.elytron;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.wildfly.security.http.HttpAuthenticationException;
import org.wildfly.security.http.HttpServerAuthenticationMechanism;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;

import javax.security.auth.callback.CallbackHandler;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakHttpServerAuthenticationMechanismFactory implements HttpServerAuthenticationMechanismFactory {

    private final AdapterDeploymentContext deploymentContext;

    /**
     * <p>Creates a new instance.
     *
     * <p>A default constructor is necessary in order to allow this factory to be loaded via {@link java.util.ServiceLoader}.
     */
    public KeycloakHttpServerAuthenticationMechanismFactory() {
        this(null);
    }

    public KeycloakHttpServerAuthenticationMechanismFactory(AdapterDeploymentContext deploymentContext) {
        this.deploymentContext = deploymentContext;
    }

    @Override
    public String[] getMechanismNames(Map<String, ?> properties) {
        return new String[] {KeycloakHttpServerAuthenticationMechanism.NAME};
    }

    @Override
    public HttpServerAuthenticationMechanism createAuthenticationMechanism(String mechanismName, Map<String, ?> properties, CallbackHandler callbackHandler) throws HttpAuthenticationException {
        Map<String, Object> mechanismProperties = new HashMap();

        mechanismProperties.putAll(properties);

        if (KeycloakHttpServerAuthenticationMechanism.NAME.equals(mechanismName)) {
            return new KeycloakHttpServerAuthenticationMechanism(properties, callbackHandler, this.deploymentContext);
        }

        return null;
    }
}
