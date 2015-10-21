package org.keycloak.adapters.saml.jetty;

import org.eclipse.jetty.server.Request;
import org.jboss.logging.Logger;
import org.keycloak.adapters.spi.AdapterSessionStore;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.jetty.spi.JettyUserSessionManagement;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;

import javax.servlet.http.HttpSession;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JettySamlSessionStore implements SamlSessionStore {
    public static final String SAML_REDIRECT_URI = "SAML_REDIRECT_URI";
    private static final Logger log = Logger.getLogger(JettySamlSessionStore.class);
    private Request request;
    protected AdapterSessionStore sessionStore;
    protected HttpFacade facade;
    protected SessionIdMapper idMapper;
    protected JettyUserSessionManagement sessionManagement;

    public JettySamlSessionStore(Request request, AdapterSessionStore sessionStore, HttpFacade facade,
                                 SessionIdMapper idMapper, JettyUserSessionManagement sessionManagement) {
        this.request = request;
        this.sessionStore = sessionStore;
        this.facade = facade;
        this.idMapper = idMapper;
        this.sessionManagement = sessionManagement;
    }

    @Override
    public void logoutAccount() {
        HttpSession session = request.getSession(false);
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
            List<String> ids = new LinkedList<String>();
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
        List<String> sessionIds = new LinkedList<String>();
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
        sessionManagement.logoutHttpSessions(sessionIds);
    }

    @Override
    public boolean isLoggedIn() {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        if (session == null) {
            log.debug("session was null, returning null");
            return false;
        }
        SamlSession samlSession = (SamlSession)session.getAttribute(SamlSession.class.getName());
        if (samlSession == null) {
            log.debug("SamlSession was not in session, returning null");
            return false;
        }

        restoreRequest();
        return true;
    }

    @Override
    public void saveAccount(SamlSession account) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SamlSession.class.getName(), account);

        idMapper.map(account.getSessionIndex(), account.getPrincipal().getSamlSubject(), session.getId());

    }

    @Override
    public SamlSession getAccount() {
        HttpSession session = request.getSession(true);
        return (SamlSession)session.getAttribute(SamlSession.class.getName());
    }

    @Override
    public String getRedirectUri() {
        return (String)request.getSession(true).getAttribute(SAML_REDIRECT_URI);
    }

    @Override
    public void saveRequest() {
        sessionStore.saveRequest();

        request.getSession(true).setAttribute(SAML_REDIRECT_URI, facade.getRequest().getURI());

    }

    @Override
    public boolean restoreRequest() {
        return sessionStore.restoreRequest();
    }

}
