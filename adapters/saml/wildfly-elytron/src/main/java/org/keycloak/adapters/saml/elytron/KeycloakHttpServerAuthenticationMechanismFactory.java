/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.saml.elytron;

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.callback.CallbackHandler;

import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapperUpdater;

import org.wildfly.security.http.HttpAuthenticationException;
import org.wildfly.security.http.HttpServerAuthenticationMechanism;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakHttpServerAuthenticationMechanismFactory implements HttpServerAuthenticationMechanismFactory {

    private final SessionIdMapper idMapper = new InMemorySessionIdMapper();
    private final SamlDeploymentContext deploymentContext;

    /**
     * <p>Creates a new instance.
     *
     * <p>A default constructor is necessary in order to allow this factory to be loaded via {@link java.util.ServiceLoader}.
     */
    public KeycloakHttpServerAuthenticationMechanismFactory() {
        this(null);
    }

    public KeycloakHttpServerAuthenticationMechanismFactory(SamlDeploymentContext deploymentContext) {
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
            KeycloakHttpServerAuthenticationMechanism mech = new KeycloakHttpServerAuthenticationMechanism(properties, callbackHandler, this.deploymentContext, idMapper, SessionIdMapperUpdater.DIRECT);
            return mech;
        }

        return null;
    }
}
