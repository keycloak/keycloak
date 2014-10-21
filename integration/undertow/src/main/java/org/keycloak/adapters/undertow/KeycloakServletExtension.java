/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationLifecycle;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import org.keycloak.adapters.KeycloakConfigResolver;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakServletExtension implements ServletExtension {

    protected static Logger log = Logger.getLogger(KeycloakServletExtension.class);

    // todo when this DeploymentInfo method of the same name is fixed.
    public boolean isAuthenticationMechanismPresent(DeploymentInfo deploymentInfo, final String mechanismName) {
        LoginConfig loginConfig = deploymentInfo.getLoginConfig();
        if(loginConfig != null) {
            for(AuthMethodConfig method : loginConfig.getAuthMethods()) {
                if(method.getName().equalsIgnoreCase(mechanismName)) {
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
        if (!isAuthenticationMechanismPresent(deploymentInfo, "KEYCLOAK")) {
            log.debug("auth-method is not keycloak!");
            return;
        }
        log.debug("KeycloakServletException initialization");

        KeycloakConfigResolver configResolver = null;
        String configResolverClass = servletContext.getInitParameter("keycloak.config.resolver");
        if (configResolverClass != null) {
            try {
                configResolver = (KeycloakConfigResolver) deploymentInfo.getClassLoader().loadClass(configResolverClass).newInstance();
                log.info("Using " + configResolverClass + " to resolve Keycloak configuration on a per-request basis.");
            } catch (Exception ex) {
                // TODO: should it re-throw the exception?
                //
                // Reasoning: if the explicitly defined `resolver` wasn't found, it *will* behave differently
                // than the user expects!
                //
                // Counter-reasoning: the original code doesn't throws an exception if keycloak is unconfigured, ie,
                // it's already not doing what the user would generally expect, but might be OK for development environments
                // so, we probably shouldn't block the deployment of the user's application because of keycloak's configuration
                // problem
                log.warn("The specified resolver " + configResolverClass + " could NOT be loaded. Falling back to standard behavior. Reason: " + ex.getMessage());
            }
        }

        AdapterDeploymentContext deploymentContext;
        if (configResolver != null) {
            deploymentContext = new AdapterDeploymentContext(configResolver);
            log.debug("Keycloak is using a per-request configuration resolver.");
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

            deploymentInfo.addListener(new ListenerInfo(UndertowNodesRegistrationLifecycleWrapper.class, new InstanceFactory<UndertowNodesRegistrationLifecycleWrapper>() {

                // TODO: got this during a rebase, so, need to figure out what to do with it
                // one option is to require a generic keycloak.json, with no realm information
                // and use it for the node registration, but needs some testing
                @Override
                public InstanceHandle<UndertowNodesRegistrationLifecycleWrapper> createInstance() throws InstantiationException {
                    NodesRegistrationLifecycle nodesRegistration = new NodesRegistrationLifecycle(deployment);
                    UndertowNodesRegistrationLifecycleWrapper listener = new UndertowNodesRegistrationLifecycleWrapper(nodesRegistration);
                    return new ImmediateInstanceHandle<UndertowNodesRegistrationLifecycleWrapper>(listener);
                }

            }));
        }

        servletContext.setAttribute(AdapterDeploymentContext.class.getName(), deploymentContext);
        UndertowUserSessionManagement userSessionManagement = new UndertowUserSessionManagement();
        final ServletKeycloakAuthMech mech = createAuthenticationMechanism(deploymentInfo, deploymentContext, userSessionManagement);

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

        log.debug("Setting jsession cookie path to: " + deploymentInfo.getContextPath());
        ServletSessionConfig cookieConfig = new ServletSessionConfig();
        cookieConfig.setPath(deploymentInfo.getContextPath());
        deploymentInfo.setServletSessionConfig(cookieConfig);
    }

    protected ServletKeycloakAuthMech createAuthenticationMechanism(DeploymentInfo deploymentInfo, AdapterDeploymentContext deploymentContext, UndertowUserSessionManagement userSessionManagement) {
       log.debug("creating ServletKeycloakAuthMech");
       return new ServletKeycloakAuthMech(deploymentContext, userSessionManagement, deploymentInfo.getConfidentialPortManager());
    }
}
