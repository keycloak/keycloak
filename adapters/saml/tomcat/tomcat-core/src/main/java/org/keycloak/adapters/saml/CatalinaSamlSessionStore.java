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

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.realm.GenericPrincipal;
import org.jboss.logging.Logger;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapperUpdater;
import org.keycloak.adapters.tomcat.CatalinaUserSessionManagement;
import org.keycloak.adapters.tomcat.PrincipalFactory;
import org.keycloak.common.util.KeycloakUriBuilder;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CatalinaSamlSessionStore implements SamlSessionStore {
    protected static Logger log = Logger.getLogger(SamlSessionStore.class);
    public static final String SAML_REDIRECT_URI = "SAML_REDIRECT_URI";

    private final CatalinaUserSessionManagement sessionManagement;
    protected final PrincipalFactory principalFactory;
    private final SessionIdMapper idMapper;
    private final SessionIdMapperUpdater idMapperUpdater;
    protected final Request request;
    protected final AbstractSamlAuthenticatorValve valve;
    protected final HttpFacade facade;
    protected final SamlDeployment deployment;

    public CatalinaSamlSessionStore(CatalinaUserSessionManagement sessionManagement, PrincipalFactory principalFactory,
                                    SessionIdMapper idMapper, SessionIdMapperUpdater idMapperUpdater,
                                    Request request, AbstractSamlAuthenticatorValve valve, HttpFacade facade,
                                    SamlDeployment deployment) {
        this.sessionManagement = sessionManagement;
        this.principalFactory = principalFactory;
        this.idMapper = idMapper;
        this.idMapperUpdater = idMapperUpdater;
        this.request = request;
        this.valve = valve;
        this.facade = facade;
        this.deployment = deployment;
    }

    @Override
    public void setCurrentAction(CurrentAction action) {
        if (action == CurrentAction.NONE && request.getSession(false) == null) return;
        request.getSession().setAttribute(CURRENT_ACTION, action);
    }

    @Override
    public boolean isLoggingIn() {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        CurrentAction action = (CurrentAction)session.getAttribute(CURRENT_ACTION);
        return action == CurrentAction.LOGGING_IN;
    }

    @Override
    public boolean isLoggingOut() {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        CurrentAction action = (CurrentAction)session.getAttribute(CURRENT_ACTION);
        return action == CurrentAction.LOGGING_OUT;
    }

    @Override
    public void logoutAccount() {
        Session sessionInternal = request.getSessionInternal(false);
        if (sessionInternal == null) return;
        HttpSession session = sessionInternal.getSession();
        List<String> ids = new LinkedList<String>();
        if (session != null) {
            SamlSession samlSession = (SamlSession)session.getAttribute(SamlSession.class.getName());
            if (samlSession != null) {
                if (samlSession.getSessionIndex() != null) {
                    ids.add(session.getId());
                    idMapperUpdater.removeSession(idMapper, session.getId());
                }
                session.removeAttribute(SamlSession.class.getName());
            }
            session.removeAttribute(SAML_REDIRECT_URI);
        }
        sessionInternal.setPrincipal(null);
        sessionInternal.setAuthType(null);
        logoutSessionIds(ids);
    }

    @Override
    public void logoutByPrincipal(String principal) {
        Set<String> sessions = idMapper.getUserSessions(principal);
        if (sessions != null) {
            List<String> ids = new LinkedList<String>();
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
        List<String> sessionIds = new LinkedList<String>();
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
        Manager sessionManager = request.getContext().getManager();
        sessionManagement.logoutHttpSessions(sessionManager, sessionIds);
    }

    @Override
    public boolean isLoggedIn() {
        Session session = request.getSessionInternal(false);
        if (session == null) {
            log.debug("session was null, returning null");
            return false;
        }
        final SamlSession samlSession = SamlUtil.validateSamlSession(session.getSession().getAttribute(SamlSession.class.getName()), deployment);
        if (samlSession == null) {
            return false;
        }

        GenericPrincipal principal = (GenericPrincipal) session.getPrincipal();
        // in clustered environment in JBossWeb, principal is not serialized or saved
        if (principal == null) {
            principal = principalFactory.createPrincipal(request.getContext().getRealm(), samlSession.getPrincipal(), samlSession.getRoles());
            session.setPrincipal(principal);
            session.setAuthType("KEYCLOAK-SAML");

        }
        else if (samlSession.getPrincipal().getName().equals(principal.getName())){
            if (!principal.getUserPrincipal().getName().equals(samlSession.getPrincipal().getName())) {
                throw new RuntimeException("Unknown State");
            }
            log.debug("************principal already in");
            if (log.isDebugEnabled()) {
                for (String role : principal.getRoles()) {
                    log.debug("principal role: " + role);
                }
            }

        }
        request.setUserPrincipal(principal);
        request.setAuthType("KEYCLOAK-SAML");
        restoreRequest();
        return true;
    }

    @Override
    public void saveAccount(SamlSession account) {
        Session session = request.getSessionInternal(true);
        session.getSession().setAttribute(SamlSession.class.getName(), account);
        GenericPrincipal principal = (GenericPrincipal) session.getPrincipal();
        // in clustered environment in JBossWeb, principal is not serialized or saved
        if (principal == null) {
            principal = principalFactory.createPrincipal(request.getContext().getRealm(), account.getPrincipal(), account.getRoles());
            session.setPrincipal(principal);
            session.setAuthType("KEYCLOAK-SAML");

        }
        request.setUserPrincipal(principal);
        request.setAuthType("KEYCLOAK-SAML");
        String newId = changeSessionId(session);
        idMapperUpdater.map(idMapper, account.getSessionIndex(), account.getPrincipal().getSamlSubject(), newId);

    }

    protected String changeSessionId(Session session) {
        return session.getId();
    }

    @Override
    public SamlSession getAccount() {
        HttpSession session = getSession(true);
        return (SamlSession)session.getAttribute(SamlSession.class.getName());
    }

    @Override
    public String getRedirectUri() {
        String redirect = (String)getSession(true).getAttribute(SAML_REDIRECT_URI);
        if (redirect == null) {
            String contextPath = request.getContextPath();
            String baseUri = KeycloakUriBuilder.fromUri(request.getRequestURL().toString()).replacePath(contextPath).build().toString();
            return SamlUtil.getRedirectTo(facade, contextPath, baseUri);
        }
        return redirect;
    }

    @Override
    public void saveRequest() {
        try {
            valve.keycloakSaveRequest(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getSession(true).setAttribute(SAML_REDIRECT_URI, facade.getRequest().getURI());

    }

    @Override
    public boolean restoreRequest() {
        getSession(true).removeAttribute(SAML_REDIRECT_URI);
        return valve.keycloakRestoreRequest(request);
    }

    protected HttpSession getSession(boolean create) {
        Session session = request.getSessionInternal(create);
        if (session == null) return null;
        return session.getSession();
    }
}
