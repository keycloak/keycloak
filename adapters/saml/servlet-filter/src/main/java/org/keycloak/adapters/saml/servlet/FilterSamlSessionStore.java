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

package org.keycloak.adapters.saml.servlet;

import org.jboss.logging.Logger;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.SamlUtil;
import org.keycloak.adapters.servlet.FilterSessionStore;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.common.util.KeycloakUriBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FilterSamlSessionStore extends FilterSessionStore implements SamlSessionStore {
    protected static Logger log = Logger.getLogger(SamlSessionStore.class);
    protected final SessionIdMapper idMapper;
    private final SamlDeployment deployment;

    public FilterSamlSessionStore(HttpServletRequest request, HttpFacade facade, int maxBuffer, SessionIdMapper idMapper, SamlDeployment deployment) {
        super(request, facade, maxBuffer);
        this.idMapper = idMapper;
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
        HttpSession session = request.getSession(false);
        if (session == null) return;
        if (session != null) {
            if (idMapper != null) idMapper.removeSession(session.getId());
            SamlSession samlSession = (SamlSession)session.getAttribute(SamlSession.class.getName());
            if (samlSession != null) {
                session.removeAttribute(SamlSession.class.getName());
            }
            clearSavedRequest(session);
        }
     }

    @Override
    public void logoutByPrincipal(String principal) {
        SamlSession account = getAccount();
        if (account != null && account.getPrincipal().getSamlSubject().equals(principal)) {
            logoutAccount();
        }
        if (idMapper != null) {
            Set<String> sessions = idMapper.getUserSessions(principal);
            if (sessions != null) {
                List<String> ids = new LinkedList<String>();
                ids.addAll(sessions);
                for (String id : ids) {
                    idMapper.removeSession(id);
                }
            }
        }

    }

    @Override
    public void logoutBySsoId(List<String> ssoIds) {
        SamlSession account = getAccount();
        for (String ssoId : ssoIds) {
            if (account != null && account.getSessionIndex().equals(ssoId)) {
                logoutAccount();
            } else if (idMapper != null) {
                String sessionId = idMapper.getSessionFromSSO(ssoId);
                idMapper.removeSession(sessionId);
            }
        }
    }

    @Override
    public boolean isLoggedIn() {
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.debug("session was null, returning false");
            return false;
        }
        final SamlSession samlSession = SamlUtil.validateSamlSession(session.getAttribute(SamlSession.class.getName()), deployment);
        if (samlSession == null) {
            log.debug("SamlSession was not in session, returning null");
            return false;
        }
        if (idMapper != null && !idMapper.hasSession(session.getId())) {
            logoutAccount();
            return false;
        }

        needRequestRestore = restoreRequest();
        return true;
    }

    public HttpServletRequestWrapper getWrap() {
        HttpSession session = request.getSession(true);
        final SamlSession samlSession = (SamlSession)session.getAttribute(SamlSession.class.getName());
        final KeycloakAccount account = samlSession;
        return buildWrapper(session, account);
    }

    @Override
    public void saveAccount(SamlSession account) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SamlSession.class.getName(), account);
        if (idMapper != null) idMapper.map(account.getSessionIndex(),  account.getPrincipal().getSamlSubject(), session.getId());
    }

    @Override
    public SamlSession getAccount() {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        return (SamlSession)session.getAttribute(SamlSession.class.getName());
    }

    @Override
    public String getRedirectUri() {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        String redirect = (String)session.getAttribute(REDIRECT_URI);
        if (redirect == null) {
            String contextPath = request.getContextPath();
            String baseUri = KeycloakUriBuilder.fromUri(request.getRequestURL().toString()).replacePath(contextPath).build().toString();
            return SamlUtil.getRedirectTo(facade, contextPath, baseUri);
        }
        return redirect;
    }

}
