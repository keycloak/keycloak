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

package org.keycloak.adapters.saml.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.Headers;

import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.spi.*;
import org.keycloak.adapters.undertow.ServletHttpFacade;
import org.keycloak.adapters.undertow.UndertowHttpFacade;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;

import io.undertow.servlet.api.DeploymentInfo;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletSamlAuthMech extends AbstractSamlAuthMech {

    private static final Logger LOG = Logger.getLogger(ServletSamlAuthMech.class);

    protected SessionIdMapper idMapper = new InMemorySessionIdMapper();
    protected SessionIdMapperUpdater idMapperUpdater = SessionIdMapperUpdater.DIRECT;

    public ServletSamlAuthMech(SamlDeploymentContext deploymentContext, UndertowUserSessionManagement sessionManagement, String errorPage) {
        super(deploymentContext, sessionManagement, errorPage);
    }

    public void addTokenStoreUpdaters(DeploymentInfo deploymentInfo) {
        deploymentInfo.addSessionListener(new IdMapperUpdaterSessionListener(idMapper));    // This takes care of HTTP sessions manipulated locally
        SessionIdMapperUpdater updater = SessionIdMapperUpdater.EXTERNAL;

        try {
            Map<String, String> initParameters = deploymentInfo.getInitParameters();
            String idMapperSessionUpdaterClasses = initParameters == null
              ? null
              : initParameters.get("keycloak.sessionIdMapperUpdater.classes");
            if (idMapperSessionUpdaterClasses == null) {
                return;
            }

            for (String clazz : idMapperSessionUpdaterClasses.split("\\s*,\\s*")) {
                if (! clazz.isEmpty()) {
                    updater = invokeAddTokenStoreUpdaterMethod(clazz, deploymentInfo, updater);
                }
            }
        } finally {
            setIdMapperUpdater(updater);
        }
    }

    private SessionIdMapperUpdater invokeAddTokenStoreUpdaterMethod(String idMapperSessionUpdaterClass, DeploymentInfo deploymentInfo,
      SessionIdMapperUpdater previousIdMapperUpdater) {
        try {
            Class<?> clazz = deploymentInfo.getClassLoader().loadClass(idMapperSessionUpdaterClass);
            Method addTokenStoreUpdatersMethod = clazz.getMethod("addTokenStoreUpdaters", DeploymentInfo.class, SessionIdMapper.class, SessionIdMapperUpdater.class);
            if (! Modifier.isStatic(addTokenStoreUpdatersMethod.getModifiers())
              || ! Modifier.isPublic(addTokenStoreUpdatersMethod.getModifiers())
              || ! SessionIdMapperUpdater.class.isAssignableFrom(addTokenStoreUpdatersMethod.getReturnType())) {
                LOG.errorv("addTokenStoreUpdaters method in class {0} has to be public static. Ignoring class.", idMapperSessionUpdaterClass);
                return previousIdMapperUpdater;
            }

            LOG.debugv("Initializing sessionIdMapperUpdater class {0}", idMapperSessionUpdaterClass);
            return (SessionIdMapperUpdater) addTokenStoreUpdatersMethod.invoke(null, deploymentInfo, idMapper, previousIdMapperUpdater);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException ex) {
            LOG.warnv(ex, "Cannot use sessionIdMapperUpdater class {0}", idMapperSessionUpdaterClass);
            return previousIdMapperUpdater;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOG.warnv(ex, "Cannot use {0}.addTokenStoreUpdaters(DeploymentInfo, SessionIdMapper) method", idMapperSessionUpdaterClass);
            return previousIdMapperUpdater;
        }
    }

    @Override
    protected SamlSessionStore getTokenStore(HttpServerExchange exchange, HttpFacade facade, SamlDeployment deployment, SecurityContext securityContext) {
        return new ServletSamlSessionStore(exchange, sessionManagement, securityContext, idMapper, idMapperUpdater, deployment);
    }

    @Override
    protected UndertowHttpFacade createFacade(HttpServerExchange exchange) {
        return new ServletHttpFacade(exchange);
    }

    @Override
    protected void redirectLogout(SamlDeployment deployment, HttpServerExchange exchange) {
        exchange.getResponseHeaders().add(Headers.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        exchange.getResponseHeaders().add(Headers.PRAGMA, "no-cache");
        exchange.getResponseHeaders().add(Headers.EXPIRES, "0");

        super.redirectLogout(deployment, exchange);
    }

    @Override
    protected Integer servePage(HttpServerExchange exchange, String location) {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        ServletRequest req = servletRequestContext.getServletRequest();
        ServletResponse resp = servletRequestContext.getServletResponse();
        RequestDispatcher disp = req.getRequestDispatcher(location);
        //make sure the login page is never cached
        exchange.getResponseHeaders().add(Headers.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        exchange.getResponseHeaders().add(Headers.PRAGMA, "no-cache");
        exchange.getResponseHeaders().add(Headers.EXPIRES, "0");


        try {
            disp.forward(req, resp);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public SessionIdMapperUpdater getIdMapperUpdater() {
        return idMapperUpdater;
    }

    protected void setIdMapperUpdater(SessionIdMapperUpdater idMapperUpdater) {
        this.idMapperUpdater = idMapperUpdater;
    }
}
