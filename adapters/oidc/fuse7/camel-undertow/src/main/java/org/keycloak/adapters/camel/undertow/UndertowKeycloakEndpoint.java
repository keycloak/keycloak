/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.adapters.camel.undertow;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;
import io.undertow.server.HttpServerExchange;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.undertow.UndertowComponent;
import org.apache.camel.component.undertow.UndertowEndpoint;
import static org.keycloak.adapters.camel.undertow.UndertowKeycloakConsumer.KEYCLOAK_PRINCIPAL_KEY;

/**
 *
 * @author hmlnarik
 */
public class UndertowKeycloakEndpoint extends UndertowEndpoint {

    private static final Logger LOG = Logger.getLogger(UndertowKeycloakEndpoint.class.getName());

    private KeycloakConfigResolver configResolver;

    private AdapterConfig adapterConfig;

    private String skipPattern;

    private List<String> allowedRoles = Collections.emptyList();

    private int confidentialPort = 8443;

    public UndertowKeycloakEndpoint(String uri, UndertowComponent component) {
        super(uri, component);
    }

    public AdapterConfig getAdapterConfig() {
        return adapterConfig;
    }

    public void setAdapterConfig(AdapterConfig adapterConfig) {
        LOG.info("adapterConfig");
        this.adapterConfig = adapterConfig;
    }

    public String getSkipPattern() {
        return skipPattern;
    }

    public void setSkipPattern(String skipPattern) {
        this.skipPattern = skipPattern;
    }

    public List<String> getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(List<String> allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public void setAllowedRoles(String allowedRoles) {
        this.allowedRoles = allowedRoles == null ? null : Arrays.asList(allowedRoles.split("\\s*,\\s*"));
    }

    public int getConfidentialPort() {
        return confidentialPort;
    }

    public void setConfidentialPort(int confidentialPort) {
        this.confidentialPort = confidentialPort;
    }

    public KeycloakConfigResolver getConfigResolver() {
        return configResolver;
    }

    public void setConfigResolver(KeycloakConfigResolver configResolver) {
        this.configResolver = configResolver;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new UndertowKeycloakConsumer(this, processor, getDeploymentContext(), getSkipPatternAsPattern(), computeAllowedRoles(), this.confidentialPort);
    }

    public List<String> computeAllowedRoles() {
        List<String> res = this.allowedRoles == null ? Collections.<String>emptyList() : this.allowedRoles;
        if (res.isEmpty()) {
            LOG.warning("No roles were configured, Keycloak will deny every request");
        }
        LOG.log(Level.FINE, "Allowed roles: {0}", res);
        return res;
    }

    @Override
    public Exchange createExchange(HttpServerExchange httpExchange) throws Exception {
        final Exchange res = super.createExchange(httpExchange);

        KeycloakPrincipal principal = httpExchange.getAttachment(KEYCLOAK_PRINCIPAL_KEY);
        LOG.log(Level.FINE, "principal: {0}", principal);
        if (principal != null) {
            res.setProperty(KeycloakPrincipal.class.getName(), principal);
        }

        return res;
    }

    private AdapterDeploymentContext getDeploymentContext() {
        if (configResolver != null) {
            LOG.log(Level.INFO, "Using {0} to resolve Keycloak configuration on a per-request basis.", configResolver.getClass());
            return new AdapterDeploymentContext(configResolver);
        } else if (adapterConfig != null) {
            KeycloakDeployment kd = KeycloakDeploymentBuilder.build(adapterConfig);
            return new AdapterDeploymentContext(kd);
        }

        LOG.warning("Adapter is unconfigured, Keycloak will deny every request");
        return new AdapterDeploymentContext();
    }

    private Pattern getSkipPatternAsPattern() {
        return skipPattern == null
          ? null
          : Pattern.compile(skipPattern, Pattern.DOTALL);
    }
}
