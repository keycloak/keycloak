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

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakSecurityContextRequestFilter extends GenericFilterBean implements ApplicationContextAware {

    private static final String FILTER_APPLIED = KeycloakSecurityContext.class.getPackage().getName() + ".token-refreshed";

    private ApplicationContext applicationContext;
    private AdapterDeploymentContext deploymentContext;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request.getAttribute(FILTER_APPLIED) != null) {
            filterChain.doFilter(request, response);
            return;
        }

        request.setAttribute(FILTER_APPLIED, Boolean.TRUE);

        KeycloakSecurityContext keycloakSecurityContext = getKeycloakPrincipal();

        if (keycloakSecurityContext instanceof RefreshableKeycloakSecurityContext) {
            RefreshableKeycloakSecurityContext refreshableSecurityContext = (RefreshableKeycloakSecurityContext) keycloakSecurityContext;

            if (refreshableSecurityContext.isActive()) {
                KeycloakDeployment deployment = resolveDeployment(request, response);

                if (deployment.isAlwaysRefreshToken()) {
                    if (refreshableSecurityContext.refreshExpiredToken(false)) {
                        request.setAttribute(KeycloakSecurityContext.class.getName(), refreshableSecurityContext);
                    } else {
                        clearAuthenticationContext();
                    }
                }
            } else {
                clearAuthenticationContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected void initFilterBean() throws ServletException {
        deploymentContext = applicationContext.getBean(AdapterDeploymentContext.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private KeycloakSecurityContext getKeycloakPrincipal() {
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
