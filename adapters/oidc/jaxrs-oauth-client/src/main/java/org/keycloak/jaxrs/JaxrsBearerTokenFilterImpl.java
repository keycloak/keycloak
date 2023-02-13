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

package org.keycloak.jaxrs;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.BasicAuthRequestAuthenticator;
import org.keycloak.adapters.BearerTokenRequestAuthenticator;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.UserSessionManagement;
import org.keycloak.common.constants.GenericConstants;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 * @deprecated Class is deprecated and may be removed in the future. If you want to maintain this class for Keycloak community, please
 * contact Keycloak team on keycloak-dev mailing list. You can fork it into your github repository and
 * Keycloak team will reference it from "Keycloak Extensions" page.
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
@Deprecated
public class JaxrsBearerTokenFilterImpl implements JaxrsBearerTokenFilter {

    private final static Logger log = Logger.getLogger("" + JaxrsBearerTokenFilterImpl.class);

    private String keycloakConfigFile;
    private String keycloakConfigResolverClass;
    protected volatile boolean started;

    protected AdapterDeploymentContext deploymentContext;

    // TODO: Should also somehow handle stop lifecycle for de-registration
    protected NodesRegistrationManagement nodesRegistrationManagement;
    protected UserSessionManagement userSessionManagement = new EmptyUserSessionManagement();

    public void setKeycloakConfigFile(String configFile) {
        this.keycloakConfigFile = configFile;
        attemptStart();
    }

    public String getKeycloakConfigFile() {
        return this.keycloakConfigFile;
    }

    public String getKeycloakConfigResolverClass() {
        return keycloakConfigResolverClass;
    }

    public void setKeycloakConfigResolverClass(String keycloakConfigResolverClass) {
        this.keycloakConfigResolverClass = keycloakConfigResolverClass;
        attemptStart();
    }

    // INITIALIZATION AND STARTUP

    protected void attemptStart() {
        if (started) {
            throw new IllegalStateException("Filter already started. Make sure to specify just keycloakConfigResolver or keycloakConfigFile but not both");
        }

        if (isInitialized()) {
            start();
        } else {
            log.fine("Not yet initialized");
        }
    }

    protected boolean isInitialized() {
        return this.keycloakConfigFile != null || this.keycloakConfigResolverClass != null;
    }

    protected void start() {
        if (started) {
            throw new IllegalStateException("Filter already started. Make sure to specify just keycloakConfigResolver or keycloakConfigFile but not both");
        }

        if (keycloakConfigResolverClass != null) {
            Class<? extends KeycloakConfigResolver> resolverClass = loadResolverClass();

            try {
                KeycloakConfigResolver resolver = resolverClass.newInstance();
                log.info("Using " + resolver + " to resolve Keycloak configuration on a per-request basis.");
                this.deploymentContext = new AdapterDeploymentContext(resolver);
            } catch (Exception e) {
                throw new RuntimeException("Unable to instantiate resolver " + resolverClass);
            }
        } else {
            if (keycloakConfigFile == null) {
                throw new IllegalArgumentException("You need to specify either keycloakConfigResolverClass or keycloakConfigFile in configuration");
            }
            InputStream is = loadKeycloakConfigFile();
            KeycloakDeployment kd = KeycloakDeploymentBuilder.build(is);
            deploymentContext = new AdapterDeploymentContext(kd);
            log.info("Keycloak is using a per-deployment configuration loaded from: " + keycloakConfigFile);
        }

        nodesRegistrationManagement = new NodesRegistrationManagement();
        started = true;
    }

    // TODO: Use 'Reflections.classForName'
    protected Class<? extends KeycloakConfigResolver> loadResolverClass() {
        try {
            return (Class<? extends KeycloakConfigResolver>)getClass().getClassLoader().loadClass(keycloakConfigResolverClass);
        } catch (ClassNotFoundException cnfe) {
            // Fallback to tccl
            try {
                return (Class<? extends KeycloakConfigResolver>)Thread.currentThread().getContextClassLoader().loadClass(keycloakConfigResolverClass);
            } catch (ClassNotFoundException cnfe2) {
                throw new RuntimeException("Unable to find resolver class: " + keycloakConfigResolverClass);
            }
        }
    }

    protected InputStream loadKeycloakConfigFile() {
        if (keycloakConfigFile.startsWith(GenericConstants.PROTOCOL_CLASSPATH)) {
            String classPathLocation = keycloakConfigFile.replace(GenericConstants.PROTOCOL_CLASSPATH, "");
            log.fine("Loading config from classpath on location: " + classPathLocation);
            // Try current class classloader first
            InputStream is = getClass().getClassLoader().getResourceAsStream(classPathLocation);
            if (is == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classPathLocation);
            }

            if (is != null) {
                return is;
            } else {
                throw new RuntimeException("Unable to find config from classpath: " + keycloakConfigFile);
            }
        } else {
            // Fallback to file
            try {
                log.fine("Loading config from file: " + keycloakConfigFile);
                return new FileInputStream(keycloakConfigFile);
            } catch (FileNotFoundException fnfe) {
                log.severe("Config not found on " + keycloakConfigFile);
                throw new RuntimeException(fnfe);
            }
        }
    }

    // REQUEST HANDLING

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        SecurityContext securityContext = getRequestSecurityContext(request);
        JaxrsHttpFacade facade = new JaxrsHttpFacade(request, securityContext);
        if (handlePreauth(facade)) {
            return;
        }

        KeycloakDeployment resolvedDeployment = deploymentContext.resolveDeployment(facade);

        nodesRegistrationManagement.tryRegister(resolvedDeployment);

        bearerAuthentication(facade, request, resolvedDeployment);
    }

    protected boolean handlePreauth(JaxrsHttpFacade facade) {
        PreAuthActionsHandler handler = new PreAuthActionsHandler(userSessionManagement, deploymentContext, facade);
        if (handler.handleRequest()) {
            // Send response now (if not already sent)
            if (!facade.isResponseFinished()) {
                facade.getResponse().end();
            }
            return true;
        }

        return false;
    }

    protected void bearerAuthentication(JaxrsHttpFacade facade, ContainerRequestContext request, KeycloakDeployment resolvedDeployment) {
        BearerTokenRequestAuthenticator authenticator = new BearerTokenRequestAuthenticator(resolvedDeployment);
        AuthOutcome outcome = authenticator.authenticate(facade);
        
        if (outcome == AuthOutcome.NOT_ATTEMPTED && resolvedDeployment.isEnableBasicAuth()) {
            authenticator = new BasicAuthRequestAuthenticator(resolvedDeployment);
            outcome = authenticator.authenticate(facade);
        }
        
        if (outcome == AuthOutcome.FAILED || outcome == AuthOutcome.NOT_ATTEMPTED) {
            AuthChallenge challenge = authenticator.getChallenge();
            log.fine("Authentication outcome: " + outcome);
            boolean challengeSent = challenge.challenge(facade);
            if (!challengeSent) {
                // Use some default status code
                facade.getResponse().setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
            }

            // Send response now (if not already sent)
            if (!facade.isResponseFinished()) {
                facade.getResponse().end();
            }
            return;
        } else {
            if (verifySslFailed(facade, resolvedDeployment)) {
                return;
            }
        }

        propagateSecurityContext(facade, request, resolvedDeployment, authenticator);
        handleAuthActions(facade, resolvedDeployment);
    }

    protected void propagateSecurityContext(JaxrsHttpFacade facade, ContainerRequestContext request, KeycloakDeployment resolvedDeployment, BearerTokenRequestAuthenticator bearer) {
        RefreshableKeycloakSecurityContext skSession = new RefreshableKeycloakSecurityContext(resolvedDeployment, null, bearer.getTokenString(), bearer.getToken(), null, null, null);

        // Not needed to do resteasy specifics as KeycloakSecurityContext can be always retrieved from SecurityContext by typecast SecurityContext.getUserPrincipal to KeycloakPrincipal
        // ResteasyProviderFactory.pushContext(KeycloakSecurityContext.class, skSession);

        facade.setSecurityContext(skSession);
        String principalName = AdapterUtils.getPrincipalName(resolvedDeployment, bearer.getToken());
        final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<RefreshableKeycloakSecurityContext>(principalName, skSession);
        SecurityContext anonymousSecurityContext = getRequestSecurityContext(request);
        final boolean isSecure = anonymousSecurityContext.isSecure();
        final Set<String> roles = AdapterUtils.getRolesFromSecurityContext(skSession);

        SecurityContext ctx = new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return principal;
            }

            @Override
            public boolean isUserInRole(String role) {
                return roles.contains(role);
            }

            @Override
            public boolean isSecure() {
                return isSecure;
            }

            @Override
            public String getAuthenticationScheme() {
                return "OAUTH_BEARER";
            }
        };
        request.setSecurityContext(ctx);
    }

    protected boolean verifySslFailed(JaxrsHttpFacade facade, KeycloakDeployment deployment) {
        if (!facade.getRequest().isSecure() && deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr())) {
            log.warning("SSL is required to authenticate, but request is not secured");
            facade.getResponse().sendError(403, "SSL required!");
            return true;
        }
        return false;
    }

    protected SecurityContext getRequestSecurityContext(ContainerRequestContext request) {
        return request.getSecurityContext();
    }

    protected void handleAuthActions(JaxrsHttpFacade facade, KeycloakDeployment deployment) {
        AuthenticatedActionsHandler authActionsHandler = new AuthenticatedActionsHandler(deployment, facade);
        if (authActionsHandler.handledRequest()) {
            // Send response now (if not already sent)
            if (!facade.isResponseFinished()) {
                facade.getResponse().end();
            }
        }
    }

    // We don't have any sessions to manage with pure jaxrs filter
    private static class EmptyUserSessionManagement implements UserSessionManagement {

        @Override
        public void logoutAll() {
        }

        @Override
        public void logoutHttpSessions(List<String> ids) {
        }
    }

}
