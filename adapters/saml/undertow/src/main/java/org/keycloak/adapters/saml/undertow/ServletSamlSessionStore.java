package org.keycloak.adapters.saml.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpSessionImpl;
import io.undertow.servlet.util.SavedRequest;
import org.jboss.logging.Logger;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.dom.saml.v2.protocol.StatusType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
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


    public ServletSamlSessionStore(HttpServerExchange exchange, UndertowUserSessionManagement sessionManagement,
                                   SecurityContext securityContext,
                                   SessionIdMapper idMapper) {
        this.exchange = exchange;
        this.sessionManagement = sessionManagement;
        this.securityContext = securityContext;
        this.idMapper = idMapper;
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
                    idMapper.removeSession(session.getId());
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
                idMapper.removeSession(id);
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
                 idMapper.removeSession(sessionId);
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
            log.debug("session was null, returning null");
            return false;
        }
        final SamlSession samlSession = (SamlSession)session.getAttribute(SamlSession.class.getName());
        if (samlSession == null) {
            log.debug("SamlSession was not in session, returning null");
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
        idMapper.map(account.getSessionIndex(), account.getPrincipal().getSamlSubject(), session.getId());

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
        return (String)session.getAttribute(SAML_REDIRECT_URI);
    }

    @Override
    public void saveRequest() {
        SavedRequest.trySaveRequest(exchange);
        final ServletRequestContext sc = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpSessionImpl session = sc.getCurrentServletContext().getSession(exchange, true);
        KeycloakUriBuilder uriBuilder = KeycloakUriBuilder.fromUri(exchange.getRequestURI())
                .replaceQuery(exchange.getQueryString());
        if (!exchange.isHostIncludedInRequestURI()) uriBuilder.scheme(exchange.getRequestScheme()).host(exchange.getHostAndPort());
        String uri = uriBuilder.build().toString();

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
