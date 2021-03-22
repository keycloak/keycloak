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
package org.keycloak.adapters.undertow;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.AuthMethodConfig;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.ServletSessionConfig;
import io.undertow.servlet.util.ImmediateInstanceHandle;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.constants.AdapterConstants;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakServletExtension implements ServletExtension {

    protected static Logger log = Logger.getLogger(KeycloakServletExtension.class);
    private final AdapterDeploymentContext deploymentContext;

    public KeycloakServletExtension() {
        this(null);
    }

    public KeycloakServletExtension(AdapterDeploymentContext deploymentContext) {
        this.deploymentContext = deploymentContext;
    }

    // todo when this DeploymentInfo method of the same name is fixed.
    public boolean isAuthenticationMechanismPresent(DeploymentInfo deploymentInfo, final String mechanismName) {
        LoginConfig loginConfig = deploymentInfo.getLoginConfig();
        if (loginConfig != null) {
            for (AuthMethodConfig method : loginConfig.getAuthMethods()) {
                if (method.getName().equalsIgnoreCase(mechanismName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static InputStream getJSONFromServletContext(ServletContext servletContext) {
        String json = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (json == null) {
            return null;
        }
        return new ByteArrayInputStream(json.getBytes());
    }

    private static InputStream getConfigInputStream(ServletContext context) {
        InputStream is = getJSONFromServletContext(context);
        if (is == null) {
            String path = context.getInitParameter("keycloak.config.file");
            if (path == null) {
                log.debug("using /WEB-INF/keycloak.json");
                is = context.getResourceAsStream("/WEB-INF/keycloak.json");
            } else {
                try {
                    is = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return is;
    }


    @Override
    @SuppressWarnings("UseSpecificCatch")
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        if (!isAuthenticationMechanismPresent(deploymentInfo, "KEYCLOAK") && deploymentContext == null) {
            log.debug("auth-method is not keycloak!");
            return;
        }
        log.debug("KeycloakServletException initialization");

        // Possible scenarios:
        // 1) The deployment has a keycloak.config.resolver specified and it exists:
        //    Outcome: adapter uses the resolver
        // 2) The deployment has a keycloak.config.resolver and isn't valid (doesn't exists, isn't a resolver, ...) :
        //    Outcome: adapter is left unconfigured
        // 3) The deployment doesn't have a keycloak.config.resolver , but has a keycloak.json (or equivalent)
        //    Outcome: adapter uses it
        // 4) The deployment doesn't have a keycloak.config.resolver nor keycloak.json (or equivalent)
        //    Outcome: adapter is left unconfigured
        AdapterDeploymentContext deploymentContext = this.deploymentContext;

        if (deploymentContext == null) {
            KeycloakConfigResolver configResolver;
            String configResolverClass = servletContext.getInitParameter("keycloak.config.resolver");
            if (configResolverClass != null) {
                try {
                    configResolver = (KeycloakConfigResolver) deploymentInfo.getClassLoader().loadClass(configResolverClass).newInstance();
                    deploymentContext = new AdapterDeploymentContext(configResolver);
                    log.info("Using " + configResolverClass + " to resolve Keycloak configuration on a per-request basis.");
                } catch (Exception ex) {
                    log.warn("The specified resolver " + configResolverClass + " could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: " + ex.getMessage());
                    deploymentContext = new AdapterDeploymentContext(new KeycloakDeployment());
                }
            } else {
                InputStream is = getConfigInputStream(servletContext);
                final KeycloakDeployment deployment;
                if (is == null) {
                    log.warn("No adapter configuration.  Keycloak is unconfigured and will deny all requests.");
                    deployment = new KeycloakDeployment();
                } else {
                    deployment = KeycloakDeploymentBuilder.build(is);
                }
                deploymentContext = new AdapterDeploymentContext(deployment);
                log.debug("Keycloak is using a per-deployment configuration.");
            }
        } else {
            deploymentContext = this.deploymentContext;
        }

        servletContext.setAttribute(AdapterDeploymentContext.class.getName(), deploymentContext);
        UndertowUserSessionManagement userSessionManagement = new UndertowUserSessionManagement();
        final NodesRegistrationManagement nodesRegistrationManagement = new NodesRegistrationManagement();
        final ServletKeycloakAuthMech mech = createAuthenticationMechanism(deploymentInfo, deploymentContext, userSessionManagement, nodesRegistrationManagement);

        UndertowAuthenticatedActionsHandler.Wrapper actions = new UndertowAuthenticatedActionsHandler.Wrapper(deploymentContext);

        // setup handlers

        deploymentInfo.addOuterHandlerChainWrapper(new ServletPreAuthActionsHandler.Wrapper(deploymentContext, userSessionManagement));
        deploymentInfo.addAuthenticationMechanism("KEYCLOAK", new AuthenticationMechanismFactory() {
            @Override
            public AuthenticationMechanism create(String s, FormParserFactory formParserFactory, Map<String, String> stringStringMap) {
                return mech;
            }
        }); // authentication
        deploymentInfo.addInnerHandlerChainWrapper(actions); // handles authenticated actions and cors.

        deploymentInfo.setIdentityManager(new IdentityManager() {
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
        });

        ServletSessionConfig cookieConfig = deploymentInfo.getServletSessionConfig();
        if (cookieConfig == null) {
            cookieConfig = new ServletSessionConfig();
        }
        if (cookieConfig.getPath() == null) {
            log.debug("Setting jsession cookie path to: " + deploymentInfo.getContextPath());
            cookieConfig.setPath(deploymentInfo.getContextPath());
            deploymentInfo.setServletSessionConfig(cookieConfig);
        }
        ChangeSessionId.turnOffChangeSessionIdOnLogin(deploymentInfo);
        deploymentInfo.addListener(new ListenerInfo(UndertowNodesRegistrationManagementWrapper.class, new InstanceFactory<UndertowNodesRegistrationManagementWrapper>() {

            @Override
            public InstanceHandle<UndertowNodesRegistrationManagementWrapper> createInstance() throws InstantiationException {
                UndertowNodesRegistrationManagementWrapper listener = new UndertowNodesRegistrationManagementWrapper(nodesRegistrationManagement);
                return new ImmediateInstanceHandle<UndertowNodesRegistrationManagementWrapper>(listener);
            }

        }));
    }

    protected ServletKeycloakAuthMech createAuthenticationMechanism(DeploymentInfo deploymentInfo, AdapterDeploymentContext deploymentContext, UndertowUserSessionManagement userSessionManagement,
                                                                    NodesRegistrationManagement nodesRegistrationManagement) {
        log.debug("creating ServletKeycloakAuthMech");
        String errorPage = getErrorPage(deploymentInfo);
        return new ServletKeycloakAuthMech(deploymentContext, userSessionManagement, nodesRegistrationManagement, deploymentInfo.getConfidentialPortManager(), errorPage);
    }

    protected String getErrorPage(DeploymentInfo deploymentInfo) {
        LoginConfig loginConfig = deploymentInfo.getLoginConfig();
        String errorPage = null;
        if (loginConfig != null) {
            errorPage = loginConfig.getErrorPage();
        }
        return errorPage;
    }

}
