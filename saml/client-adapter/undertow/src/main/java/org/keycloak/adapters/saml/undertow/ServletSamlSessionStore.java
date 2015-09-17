package org.keycloak.adapters.saml.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpSessionImpl;
import io.undertow.servlet.util.SavedRequest;
import io.undertow.util.Sessions;
import org.jboss.logging.Logger;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;
import org.keycloak.util.KeycloakUriBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Principal;
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

    public ServletSamlSessionStore(HttpServerExchange exchange, UndertowUserSessionManagement sessionManagement,
                                   SecurityContext securityContext) {
        this.exchange = exchange;
        this.sessionManagement = sessionManagement;
        this.securityContext = securityContext;
    }

    @Override
    public void logoutAccount() {
        HttpSession session = getSession(false);
        if (session != null) {
            session.removeAttribute(SamlSession.class.getName());
            session.removeAttribute(SAML_REDIRECT_URI);
        }
    }

    @Override
    public void logoutByPrincipal(String principal) {

    }

    @Override
    public void logoutBySsoId(List<String> ssoIds) {

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
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        return req.getSession(create);
    }
}
