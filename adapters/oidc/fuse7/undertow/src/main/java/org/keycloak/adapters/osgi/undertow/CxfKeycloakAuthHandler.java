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
package org.keycloak.adapters.osgi.undertow;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.undertow.UndertowAuthenticationMechanism;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;
import org.keycloak.representations.adapters.config.AdapterConfig;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.cxf.transport.http_undertow.CXFUndertowHttpHandler;

/**
 *
 * @author hmlnarik
 */
public class CxfKeycloakAuthHandler implements CXFUndertowHttpHandler {

    private static final Logger LOG = Logger.getLogger(CxfKeycloakAuthHandler.class.getName());

    private static final IdentityManager IDENTITY_MANAGER = new IdentityManager() {
        @Override
        public Account verify(Account account) {
            return account;
        }

        @Override
        public Account verify(String id, Credential credential) {
            throw new IllegalStateException("Should never be called in Keycloak flow");
        }

        @Override
        public Account verify(Credential credential) {
            throw new IllegalStateException("Should never be called in Keycloak flow");
        }
    };

    private final UndertowUserSessionManagement userSessionManagement = new UndertowUserSessionManagement();

    protected final NodesRegistrationManagement nodesRegistrationManagement = new NodesRegistrationManagement();

    protected final SessionIdMapper idMapper = new InMemorySessionIdMapper();

    private final AtomicReference<HttpHandler> securityHandler = new AtomicReference<>();

    private Pattern skipPattern;

    private int confidentialPort = 8443;

    private HttpHandler next;

    private KeycloakConfigResolver configResolver;

    private AdapterConfig adapterConfig;

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (shouldSkip(exchange.getRequestPath())) {
            next.handleRequest(exchange);
        } else {
            getSecurityHandler().handleRequest(exchange);
        }
    }

    private HttpHandler getSecurityHandler() {
        if (this.securityHandler.get() == null) {
            HttpHandler handler = this.next;

            handler = new AuthenticationCallHandler(handler);
            handler = new AuthenticationConstraintHandler(handler);

            AdapterDeploymentContext deploymentContext = buildDeploymentContext();

            final List<AuthenticationMechanism> mechanisms
                = Collections.<AuthenticationMechanism>singletonList(
                  new UndertowAuthenticationMechanism(deploymentContext, userSessionManagement, nodesRegistrationManagement, confidentialPort, null));
            handler = new AuthenticationMechanismsHandler(handler, mechanisms);

            this.securityHandler.compareAndSet(null, new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, IDENTITY_MANAGER, "KEYCLOAK", handler));
        }

        return this.securityHandler.get();
    }

    private AdapterDeploymentContext buildDeploymentContext() {
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

    @Override
    public void setNext(HttpHandler nextHandler) {
        this.next = nextHandler;
    }

    private boolean shouldSkip(String requestPath) {
        return skipPattern != null && skipPattern.matcher(requestPath).matches();
    }

    public KeycloakConfigResolver getConfigResolver() {
        return configResolver;
    }

    public void setConfigResolver(KeycloakConfigResolver configResolver) {
        this.configResolver = configResolver;
    }

    public int getConfidentialPort() {
        return confidentialPort;
    }

    public void setConfidentialPort(int confidentialPort) {
        this.confidentialPort = confidentialPort;
    }

    public AdapterConfig getAdapterConfig() {
        return adapterConfig;
    }

    public void setAdapterConfig(AdapterConfig adapterConfig) {
        this.adapterConfig = adapterConfig;
    }

    public String getSkipPattern() {
        return skipPattern.pattern();
    }

    public void setSkipPattern(String skipPattern) {
        this.skipPattern = Pattern.compile(skipPattern, Pattern.DOTALL);
    }

}
