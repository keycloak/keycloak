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

package org.keycloak.subsystem.adapter.extension;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.elytron.KeycloakHttpServerAuthenticationMechanismFactory;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;
import org.wildfly.security.http.util.SetMechanismInformationMechanismFactory;

import java.io.ByteArrayInputStream;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakHttpAuthenticationFactoryService implements Service<HttpServerAuthenticationMechanismFactory> {

    private final String factoryName;
    private HttpServerAuthenticationMechanismFactory httpAuthenticationFactory;

    public KeycloakHttpAuthenticationFactoryService(String factoryName) {
        this.factoryName = factoryName;
    }

    @Override
    public void start(StartContext context) throws StartException {
        KeycloakAdapterConfigService adapterConfigService = KeycloakAdapterConfigService.getInstance();
        String config = adapterConfigService.getJSON(this.factoryName);
        this.httpAuthenticationFactory = new KeycloakHttpServerAuthenticationMechanismFactory(createDeploymentContext(config.getBytes()));
    }

    @Override
    public void stop(StopContext context) {
        this.httpAuthenticationFactory = null;
    }

    @Override
    public HttpServerAuthenticationMechanismFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return new SetMechanismInformationMechanismFactory(this.httpAuthenticationFactory);
    }

    private AdapterDeploymentContext createDeploymentContext(byte[] config) {
        return new AdapterDeploymentContext(KeycloakDeploymentBuilder.build(new ByteArrayInputStream(config)));
    }
}
