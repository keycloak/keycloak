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

package org.keycloak.adapters.springsecurity.filter;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.UserSessionManagement;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Exposes a Keycloak adapter {@link PreAuthActionsHandler} as a Spring Security filter.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class KeycloakPreAuthActionsFilter extends GenericFilterBean implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(KeycloakPreAuthActionsFilter.class);

    private NodesRegistrationManagement nodesRegistrationManagement = new NodesRegistrationManagement();
    private ApplicationContext applicationContext;
    private AdapterDeploymentContext deploymentContext;
    private UserSessionManagement userSessionManagement;
    private PreAuthActionsHandlerFactory preAuthActionsHandlerFactory = new PreAuthActionsHandlerFactory();

    public KeycloakPreAuthActionsFilter() {
        super();
    }

    public KeycloakPreAuthActionsFilter(UserSessionManagement userSessionManagement) {
        this.userSessionManagement = userSessionManagement;
    }

    @Override
    protected void initFilterBean() throws ServletException {
        deploymentContext = applicationContext.getBean(AdapterDeploymentContext.class);
    }

    @Override
    public void destroy() {
        log.debug("Unregistering deployment");
        nodesRegistrationManagement.stop();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpFacade facade = new SimpleHttpFacade((HttpServletRequest)request, (HttpServletResponse)response);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        
        if (deployment == null) {
            return;
        }

        if (deployment.isConfigured()) {
            nodesRegistrationManagement.tryRegister(deploymentContext.resolveDeployment(facade));
        }

        PreAuthActionsHandler handler = preAuthActionsHandlerFactory.createPreAuthActionsHandler(facade);
        if (handler.handleRequest()) {
            log.debug("Pre-auth filter handled request: {}", ((HttpServletRequest) request).getRequestURI());
        } else {
            chain.doFilter(request, response);
        }
    }

    public void setUserSessionManagement(UserSessionManagement userSessionManagement) {
        this.userSessionManagement = userSessionManagement;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    void setNodesRegistrationManagement(NodesRegistrationManagement nodesRegistrationManagement) {
        this.nodesRegistrationManagement = nodesRegistrationManagement;
    }
    
    void setPreAuthActionsHandlerFactory(PreAuthActionsHandlerFactory preAuthActionsHandlerFactory) {
        this.preAuthActionsHandlerFactory = preAuthActionsHandlerFactory;
    }
    
    /**
     * Creates {@link PreAuthActionsHandler}s.
     * 
     * Package-private class to enable mocking.
     */
    class PreAuthActionsHandlerFactory {
        PreAuthActionsHandler createPreAuthActionsHandler(HttpFacade facade) {
            return new PreAuthActionsHandler(userSessionManagement, deploymentContext, facade);
        }
    }
}
