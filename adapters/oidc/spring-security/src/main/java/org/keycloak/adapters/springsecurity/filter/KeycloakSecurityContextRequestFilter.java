/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.keycloak.adapters.springsecurity.token.AdapterTokenStoreFactory;
import org.keycloak.adapters.springsecurity.token.SpringSecurityAdapterTokenStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakSecurityContextRequestFilter extends GenericFilterBean implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(KeycloakSecurityContextRequestFilter.class);

    private static final String FILTER_APPLIED = KeycloakSecurityContext.class.getPackage().getName() + ".token-refreshed";
    private final AdapterTokenStoreFactory adapterTokenStoreFactory = new SpringSecurityAdapterTokenStoreFactory();

    private ApplicationContext applicationContext;
    private AdapterDeploymentContext deploymentContext;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request.getAttribute(FILTER_APPLIED) != null) {
            filterChain.doFilter(request, response);
            return;
        }

        request.setAttribute(FILTER_APPLIED, Boolean.TRUE);

        KeycloakSecurityContext keycloakSecurityContext = getKeycloakSecurityContext();

        if (keycloakSecurityContext instanceof RefreshableKeycloakSecurityContext) {
            RefreshableKeycloakSecurityContext refreshableSecurityContext = (RefreshableKeycloakSecurityContext) keycloakSecurityContext;
            KeycloakDeployment deployment = resolveDeployment(request, response);

            // just in case session got serialized
            if (refreshableSecurityContext.getDeployment()==null) {
                log.trace("Recreating missing deployment and related fields in deserialized context");
                AdapterTokenStore adapterTokenStore = adapterTokenStoreFactory.createAdapterTokenStore(deployment, (HttpServletRequest) request,
                        (HttpServletResponse) response);
                refreshableSecurityContext.setCurrentRequestInfo(deployment, adapterTokenStore);
            }

            if (!refreshableSecurityContext.isActive() || deployment.isAlwaysRefreshToken()) {
                if (refreshableSecurityContext.refreshExpiredToken(false)) {
                    request.setAttribute(KeycloakSecurityContext.class.getName(), refreshableSecurityContext);
                } else {
                    clearAuthenticationContext();
                }
            }

            request.setAttribute(KeycloakSecurityContext.class.getName(), keycloakSecurityContext);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected void initFilterBean() {
        deploymentContext = applicationContext.getBean(AdapterDeploymentContext.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private KeycloakSecurityContext getKeycloakSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof KeycloakPrincipal) {
                return KeycloakPrincipal.class.cast(principal).getKeycloakSecurityContext();
            }
        }

        return null;
    }

    private KeycloakDeployment resolveDeployment(ServletRequest servletRequest, ServletResponse servletResponse) {
        return deploymentContext.resolveDeployment(new SimpleHttpFacade(HttpServletRequest.class.cast(servletRequest), HttpServletResponse.class.cast(servletResponse)));
    }

    private void clearAuthenticationContext() {
        SecurityContextHolder.clearContext();
    }
}
