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
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpSessionImpl;
import org.jboss.logging.Logger;

import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.SamlUtil;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapperUpdater;
import org.keycloak.adapters.undertow.ChangeSessionId;
import org.keycloak.adapters.undertow.SavedRequest;
import org.keycloak.adapters.undertow.ServletHttpFacade;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Session store manipulation methods per single HTTP exchange.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletSamlSessionStore implements SamlSessionStore {
    protected static Logger log = Logger.getLogger(SamlSessionStore.class);
    public static final String SAML_REDIRECT_URI = "SAML_REDIRECT_URI";

    private final HttpServerExchange exchange;
    private final UndertowUserSessionManagement sessionManagement;
    private final SecurityContext securityContext;
    private final SessionIdMapper idMapper;
    private final SessionIdMapperUpdater idMapperUpdater;
    protected final SamlDeployment deployment;


    public ServletSamlSessionStore(HttpServerExchange exchange, UndertowUserSessionManagement sessionManagement,
                                   SecurityContext securityContext,
                                   SessionIdMapper idMapper, SessionIdMapperUpdater idMapperUpdater,
                                   SamlDeployment deployment) {
        this.exchange = exchange;
        this.sessionManagement = sessionManagement;
        this.securityContext = securityContext;
        this.idMapper = idMapper;
        this.deployment = deployment;
        this.idMapperUpdater = idMapperUpdater;
    }

    @Override
    public void setCurrentAction(CurrentAction action) {
        if (action == CurrentAction.NONE && getRequest().getSession(false) == null) return;
        getRequest().getSession().setAttribute(CURRENT_ACTION, action);
    }

    @Override
    public boolean isLoggingIn() {
        HttpSession session = getRequest().getSession(false);
        if (session == null) return false;
        CurrentAction action = (CurrentAction)session.getAttribute(CURRENT_ACTION);
        return action == CurrentAction.LOGGING_IN;
    }

    @Override
    public boolean isLoggingOut() {
        HttpSession session = getRequest().getSession(false);
        if (session == null) return false;
        CurrentAction action = (CurrentAction)session.getAttribute(CURRENT_ACTION);
        return action == CurrentAction.LOGGING_OUT;
    }

    @Override
    public void logoutAccount() {
        HttpSession session = getSession(false);
        if (session != null) {
            SamlSession samlSession = (SamlSession)session.getAttribute(SamlSession.class.getName());
            if (samlSession != null) {
                if (samlSession.getSessionIndex() != null) {
                    idMapperUpdater.removeSession(idMapper, session.getId());
                }
                session.removeAttribute(SamlSession.class.getName());
            }
            session.removeAttribute(SAML_REDIRECT_URI);
        }
    }

    @Override
    public void logoutByPrincipal(String principal) {
        Set<String> sessions = idMapper.getUserSessions(principal);
        if (sessions != null) {
            List<String> ids = new LinkedList<>();
            ids.addAll(sessions);
            logoutSessionIds(ids);
            for (String id : ids) {
                idMapperUpdater.removeSession(idMapper, id);
            }
        }

    }

    @Override
    public void logoutBySsoId(List<String> ssoIds) {
        if (ssoIds == null) return;
        List<String> sessionIds = new LinkedList<>();
        for (String id : ssoIds) {
             String sessionId = idMapper.getSessionFromSSO(id);
             if (sessionId != null) {
                 sessionIds.add(sessionId);
                 idMapperUpdater.removeSession(idMapper, sessionId);
             }

        }
        logoutSessionIds(sessionIds);
    }

    protected void logoutSessionIds(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) return;
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        SessionManager sessionManager = servletRequestContext.getDeployment().getSessionManager();
        sessionManagement.logoutHttpSessions(sessionManager, sessionIds);
    }

    @Override
    public boolean isLoggedIn() {
        HttpSession session = getSession(false);
        if (session == null) {
            log.debug("Session was not found");
            return false;
        }

        if (! idMapper.hasSession(session.getId()) && ! idMapperUpdater.refreshMapping(idMapper, session.getId())) {
            log.debugf("Session %s has expired on some other node", session.getId());
            session.removeAttribute(SamlSession.class.getName());
            return false;
        }

        final SamlSession samlSession = SamlUtil.validateSamlSession(session.getAttribute(SamlSession.class.getName()), deployment);
        if (samlSession == null) {
            return false;
        }

        Account undertowAccount = new Account() {
            @Override
            public Principal getPrincipal() {
                return samlSession.getPrincipal();
            }

            @Override
            public Set<String> getRoles() {
                return samlSession.getRoles();
            }
        };
        securityContext.authenticationComplete(undertowAccount, "KEYCLOAK-SAML", false);
        restoreRequest();
        return true;
    }

    @Override
    public void saveAccount(SamlSession account) {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpSession session = getSession(true);
        session.setAttribute(SamlSession.class.getName(), account);
        sessionManagement.login(servletRequestContext.getDeployment().getSessionManager());
        String sessionId = changeSessionId(session);
        idMapperUpdater.map(idMapper, account.getSessionIndex(), account.getPrincipal().getSamlSubject(), sessionId);
    }

    protected String changeSessionId(HttpSession session) {
        if (!deployment.turnOffChangeSessionIdOnLogin()) return ChangeSessionId.changeSessionId(exchange, false);
        else return session.getId();
    }

    @Override
    public SamlSession getAccount() {
        HttpSession session = getSession(true);
        return (SamlSession)session.getAttribute(SamlSession.class.getName());
    }

    @Override
    public String getRedirectUri() {
        final ServletRequestContext sc = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpSessionImpl session = sc.getCurrentServletContext().getSession(exchange, true);
        String redirect = (String)session.getAttribute(SAML_REDIRECT_URI);
        if (redirect == null) {
            ServletHttpFacade facade = new ServletHttpFacade(exchange);
            HttpServletRequest req = (HttpServletRequest)sc.getServletRequest();
            String contextPath = req.getContextPath();
            String baseUri = KeycloakUriBuilder.fromUri(req.getRequestURL().toString()).replacePath(contextPath).build().toString();
            return SamlUtil.getRedirectTo(facade, contextPath, baseUri);
        }
        return redirect;
    }

    @Override
    public void saveRequest() {
        SavedRequest.trySaveRequest(exchange);
        final ServletRequestContext sc = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpSessionImpl session = sc.getCurrentServletContext().getSession(exchange, true);
        KeycloakUriBuilder uriBuilder = KeycloakUriBuilder.fromUri(exchange.getRequestURI())
                .replaceQuery(exchange.getQueryString());
        if (!exchange.isHostIncludedInRequestURI()) uriBuilder.scheme(exchange.getRequestScheme()).host(exchange.getHostAndPort());
        String uri = uriBuilder.buildAsString();

        session.setAttribute(SAML_REDIRECT_URI, uri);

    }

    @Override
    public boolean restoreRequest() {
        HttpSession session = getSession(false);
        if (session == null) return false;
        SavedRequest.tryRestoreRequest(exchange, session);
        session.removeAttribute(SAML_REDIRECT_URI);
        return false;
    }

    protected HttpSession getSession(boolean create) {
        HttpServletRequest req = getRequest();
        return req.getSession(create);
    }

    private HttpServletResponse getResponse() {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        return (HttpServletResponse)servletRequestContext.getServletResponse();

    }

    private HttpServletRequest getRequest() {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        return (HttpServletRequest) servletRequestContext.getServletRequest();
    }
}
