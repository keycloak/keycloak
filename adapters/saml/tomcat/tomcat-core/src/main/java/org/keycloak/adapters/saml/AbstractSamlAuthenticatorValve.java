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

package org.keycloak.adapters.saml;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.jboss.logging.Logger;

import org.keycloak.adapters.saml.config.parsers.DeploymentBuilder;
import org.keycloak.adapters.saml.config.parsers.ResourceLoader;
import org.keycloak.adapters.spi.*;
import org.keycloak.adapters.tomcat.CatalinaHttpFacade;
import org.keycloak.adapters.tomcat.CatalinaUserSessionManagement;
import org.keycloak.adapters.tomcat.PrincipalFactory;
import org.keycloak.saml.common.exceptions.ParsingException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.util.regex.Pattern;

/**
 * Keycloak authentication valve
 * 
 * @author <a href="mailto:ungarida@gmail.com">Davide Ungari</a>
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractSamlAuthenticatorValve extends FormAuthenticator implements LifecycleListener {

    public static final String TOKEN_STORE_NOTE = "TOKEN_STORE_NOTE";

	private final static Logger log = Logger.getLogger(AbstractSamlAuthenticatorValve.class);
	protected CatalinaUserSessionManagement userSessionManagement = new CatalinaUserSessionManagement();
    protected SamlDeploymentContext deploymentContext;
    protected SessionIdMapper mapper = new InMemorySessionIdMapper();
    protected SessionIdMapperUpdater idMapperUpdater = SessionIdMapperUpdater.DIRECT;

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.START_EVENT.equals(event.getType())) {
            cache = false;
        } else if (Lifecycle.AFTER_START_EVENT.equals(event.getType())) {
        	keycloakInit();
        } else if (Lifecycle.BEFORE_STOP_EVENT.equals(event.getType())) {
            beforeStop();
        }
    }

    protected void logoutInternal(Request request) {
        CatalinaHttpFacade facade = new CatalinaHttpFacade(null, request);
        SamlDeployment deployment = deploymentContext.resolveDeployment(facade);
        SamlSessionStore tokenStore = getSessionStore(request, facade, deployment);
        tokenStore.logoutAccount();
        request.setUserPrincipal(null);
    }

    @SuppressWarnings("UseSpecificCatch")
    public void keycloakInit() {
        // Possible scenarios:
        // 1) The deployment has a keycloak.config.resolver specified and it exists:
        //    Outcome: adapter uses the resolver
        // 2) The deployment has a keycloak.config.resolver and isn't valid (doesn't exist, isn't a resolver, ...) :
        //    Outcome: adapter is left unconfigured
        // 3) The deployment doesn't have a keycloak.config.resolver , but has a keycloak.json (or equivalent)
        //    Outcome: adapter uses it
        // 4) The deployment doesn't have a keycloak.config.resolver nor keycloak.json (or equivalent)
        //    Outcome: adapter is left unconfigured

        String configResolverClass = context.getServletContext().getInitParameter("keycloak.config.resolver");
        if (configResolverClass != null) {
            try {
                SamlConfigResolver configResolver = (SamlConfigResolver) context.getLoader().getClassLoader().loadClass(configResolverClass).newInstance();
                deploymentContext = new SamlDeploymentContext(configResolver);
                log.infov("Using {0} to resolve Keycloak configuration on a per-request basis.", configResolverClass);
            } catch (Exception ex) {
                log.errorv("The specified resolver {0} could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: {1}", configResolverClass, ex.getMessage());
                deploymentContext = new SamlDeploymentContext(new DefaultSamlDeployment());
            }
        } else {
            InputStream is = getConfigInputStream(context);
            final SamlDeployment deployment;
            if (is == null) {
                log.error("No adapter configuration. Keycloak is unconfigured and will deny all requests.");
                deployment = new DefaultSamlDeployment();
            } else {
                try {
                    ResourceLoader loader = new ResourceLoader() {
                        @Override
                        public InputStream getResourceAsStream(String resource) {
                            return context.getServletContext().getResourceAsStream(resource);
                        }
                    };
                    deployment = new DeploymentBuilder().build(is, loader);
                } catch (ParsingException e) {
                    throw new RuntimeException(e);
                }
            }
            deploymentContext = new SamlDeploymentContext(deployment);
            log.debug("Keycloak is using a per-deployment configuration.");
        }

        context.getServletContext().setAttribute(SamlDeploymentContext.class.getName(), deploymentContext);

        addTokenStoreUpdaters();
    }

    protected void beforeStop() {
    }

    private static InputStream getConfigFromServletContext(ServletContext servletContext) {
        String xml = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (xml == null) {
            return null;
        }
        log.trace("**** using " + AdapterConstants.AUTH_DATA_PARAM_NAME);
        return new ByteArrayInputStream(xml.getBytes());
    }

    private static InputStream getConfigInputStream(Context context) {
        InputStream is = getConfigFromServletContext(context.getServletContext());
        if (is == null) {
            String path = context.getServletContext().getInitParameter("keycloak.config.file");
            if (path == null) {
                log.trace("**** using /WEB-INF/keycloak-saml.xml");
                is = context.getServletContext().getResourceAsStream("/WEB-INF/keycloak-saml.xml");
            } else {
                try {
                    is = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    log.errorv("NOT FOUND {0}", path);
                    throw new RuntimeException(e);
                }
            }
        }
        return is;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        log.trace("*********************** SAML ************");
        CatalinaHttpFacade facade = new CatalinaHttpFacade(response, request);
        SamlDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (request.getRequestURI().substring(request.getContextPath().length()).endsWith("/saml")) {
            if (deployment != null && deployment.isConfigured()) {
                SamlSessionStore tokenStore = getSessionStore(request, facade, deployment);
                SamlAuthenticator authenticator = new CatalinaSamlEndpoint(facade, deployment, tokenStore);
                executeAuthenticator(request, response, facade, deployment, authenticator);
                return;
            }

        }

        try {
            getSessionStore(request, facade, deployment).isLoggedIn();  // sets request UserPrincipal if logged in.  we do this so that the UserPrincipal is available on unsecured, unconstrainted URLs
            super.invoke(request, response);
        } finally {
        }

    }

    protected abstract PrincipalFactory createPrincipalFactory();
    protected abstract boolean forwardToErrorPageInternal(Request request, HttpServletResponse response, Object loginConfig) throws IOException;
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:");

    protected void forwardToLogoutPage(Request request, HttpServletResponse response, SamlDeployment deployment) {
        final String location = deployment.getLogoutPage();

        try {
            //make sure the login page is never cached
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            if (location == null) {
                log.warn("Logout page not set.");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else if (PROTOCOL_PATTERN.matcher(location).find()) {
                response.sendRedirect(response.encodeRedirectURL(location));
            } else {
                RequestDispatcher disp = request.getRequestDispatcher(location);

                disp.forward(request.getRequest(), response);
            }
        } catch (ServletException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    protected boolean authenticateInternal(Request request, HttpServletResponse response, Object loginConfig) throws IOException {
        log.trace("authenticateInternal");
        CatalinaHttpFacade facade = new CatalinaHttpFacade(response, request);
        SamlDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment == null || !deployment.isConfigured()) {
            log.trace("deployment not configured");
            return false;
        }
        SamlSessionStore tokenStore = getSessionStore(request, facade, deployment);


        SamlAuthenticator authenticator = new CatalinaSamlAuthenticator(facade, deployment, tokenStore);
        return executeAuthenticator(request, response, facade, deployment, authenticator);
    }

    protected boolean executeAuthenticator(Request request, HttpServletResponse response, CatalinaHttpFacade facade, SamlDeployment deployment, SamlAuthenticator authenticator) {
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            log.trace("AUTHENTICATED");
            if (facade.isEnded()) {
                return false;
            }
            return true;
        }
        if (outcome == AuthOutcome.LOGGED_OUT) {
            logoutInternal(request);
            if (deployment.getLogoutPage() != null) {
                forwardToLogoutPage(request, response, deployment);

            }
            log.trace("Logging OUT");
            return false;
        }

        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            log.trace("challenge");
            challenge.challenge(facade);
        }
        return false;
    }

    public void keycloakSaveRequest(Request request) throws IOException {
        saveRequest(request, request.getSessionInternal(true));
    }

    public boolean keycloakRestoreRequest(Request request) {
        try {
            return restoreRequest(request, request.getSessionInternal());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected SamlSessionStore getSessionStore(Request request, HttpFacade facade, SamlDeployment resolvedDeployment) {
        SamlSessionStore store = (SamlSessionStore)request.getNote(TOKEN_STORE_NOTE);
        if (store != null) {
            return store;
        }

        store = createSessionStore(request, facade, resolvedDeployment);

        request.setNote(TOKEN_STORE_NOTE, store);
        return store;
    }

    protected SamlSessionStore createSessionStore(Request request, HttpFacade facade, SamlDeployment resolvedDeployment) {
        SamlSessionStore store;
        store = new CatalinaSamlSessionStore(userSessionManagement, createPrincipalFactory(), mapper, idMapperUpdater, request, this, facade, resolvedDeployment);
        return store;
    }

    protected void addTokenStoreUpdaters() {
        SessionIdMapperUpdater updater = getIdMapperUpdater();

        try {
            String idMapperSessionUpdaterClasses = context.getServletContext().getInitParameter("keycloak.sessionIdMapperUpdater.classes");
            if (idMapperSessionUpdaterClasses == null) {
                return;
            }

            for (String clazz : idMapperSessionUpdaterClasses.split("\\s*,\\s*")) {
                if (! clazz.isEmpty()) {
                    updater = invokeAddTokenStoreUpdaterMethod(clazz, updater);
                }
            }
        } finally {
            setIdMapperUpdater(updater);
        }
    }

    private SessionIdMapperUpdater invokeAddTokenStoreUpdaterMethod(String idMapperSessionUpdaterClass, SessionIdMapperUpdater previousIdMapperUpdater) {
        try {
            Class<?> clazz = context.getLoader().getClassLoader().loadClass(idMapperSessionUpdaterClass);
            Method addTokenStoreUpdatersMethod = clazz.getMethod("addTokenStoreUpdaters", Context.class, SessionIdMapper.class, SessionIdMapperUpdater.class);
            if (! Modifier.isStatic(addTokenStoreUpdatersMethod.getModifiers())
              || ! Modifier.isPublic(addTokenStoreUpdatersMethod.getModifiers())
              || ! SessionIdMapperUpdater.class.isAssignableFrom(addTokenStoreUpdatersMethod.getReturnType())) {
                log.errorv("addTokenStoreUpdaters method in class {0} has to be public static. Ignoring class.", idMapperSessionUpdaterClass);
                return previousIdMapperUpdater;
            }

            log.debugv("Initializing sessionIdMapperUpdater class {0}", idMapperSessionUpdaterClass);
            return (SessionIdMapperUpdater) addTokenStoreUpdatersMethod.invoke(null, context, mapper, previousIdMapperUpdater);
        } catch (ClassNotFoundException ex) {
            log.warnv(ex, "Cannot use sessionIdMapperUpdater class {0}", idMapperSessionUpdaterClass);
            return previousIdMapperUpdater;
        } catch (NoSuchMethodException ex) {
            log.warnv(ex, "Cannot use sessionIdMapperUpdater class {0}", idMapperSessionUpdaterClass);
            return previousIdMapperUpdater;
        } catch (SecurityException ex) {
            log.warnv(ex, "Cannot use sessionIdMapperUpdater class {0}", idMapperSessionUpdaterClass);
            return previousIdMapperUpdater;
        } catch (IllegalAccessException ex) {
            log.warnv(ex, "Cannot use {0}.addTokenStoreUpdaters(DeploymentInfo, SessionIdMapper) method", idMapperSessionUpdaterClass);
            return previousIdMapperUpdater;
        } catch (IllegalArgumentException ex) {
            log.warnv(ex, "Cannot use {0}.addTokenStoreUpdaters(DeploymentInfo, SessionIdMapper) method", idMapperSessionUpdaterClass);
            return previousIdMapperUpdater;
        } catch (InvocationTargetException ex) {
            log.warnv(ex, "Cannot use {0}.addTokenStoreUpdaters(DeploymentInfo, SessionIdMapper) method", idMapperSessionUpdaterClass);
            return previousIdMapperUpdater;
        }
    }

    public SessionIdMapperUpdater getIdMapperUpdater() {
        return idMapperUpdater;
    }

    public void setIdMapperUpdater(SessionIdMapperUpdater idMapperUpdater) {
        this.idMapperUpdater = idMapperUpdater;
    }
}
