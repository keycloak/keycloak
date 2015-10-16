package org.keycloak.adapters.saml;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.realm.GenericPrincipal;
import org.jboss.logging.Logger;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.tomcat.CatalinaUserSessionManagement;
import org.keycloak.adapters.tomcat.GenericPrincipalFactory;

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
    protected final GenericPrincipalFactory principalFactory;
    private final SessionIdMapper idMapper;
    protected final Request request;
    protected final AbstractSamlAuthenticatorValve valve;
    protected final HttpFacade facade;

    public CatalinaSamlSessionStore(CatalinaUserSessionManagement sessionManagement, GenericPrincipalFactory principalFactory,
                                    SessionIdMapper idMapper, Request request, AbstractSamlAuthenticatorValve valve, HttpFacade facade) {
        this.sessionManagement = sessionManagement;
        this.principalFactory = principalFactory;
        this.idMapper = idMapper;
        this.request = request;
        this.valve = valve;
        this.facade = facade;
    }

    @Override
    public void logoutAccount() {
        Session sessionInternal = request.getSessionInternal(false);
        if (sessionInternal == null) return;
        HttpSession session = sessionInternal.getSession();
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
        sessionInternal.setPrincipal(null);
        sessionInternal.setAuthType(null);
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
        Manager sessionManager = request.getContext().getManager();
        sessionManagement.logoutHttpSessions(sessionManager, sessionIds);
    }

    @Override
    public boolean isLoggedIn() {
        Session session = request.getSessionInternal(false);
        if (session == null) return false;
        if (session == null) {
            log.debug("session was null, returning null");
            return false;
        }
        final SamlSession samlSession = (SamlSession)session.getSession().getAttribute(SamlSession.class.getName());
        if (samlSession == null) {
            log.debug("SamlSession was not in session, returning null");
            return false;
        }

        GenericPrincipal principal = (GenericPrincipal) session.getPrincipal();
        if (samlSession.getPrincipal().getName().equals(principal.getName()))
        // in clustered environment in JBossWeb, principal is not serialized or saved
        if (principal == null) {
            principal = principalFactory.createPrincipal(request.getContext().getRealm(), samlSession.getPrincipal(), samlSession.getRoles());
            session.setPrincipal(principal);
            session.setAuthType("KEYCLOAK-SAML");

        } else {
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
        idMapper.map(account.getSessionIndex(), account.getPrincipal().getSamlSubject(), session.getId());

    }

    @Override
    public SamlSession getAccount() {
        HttpSession session = getSession(true);
        return (SamlSession)session.getAttribute(SamlSession.class.getName());
    }

    @Override
    public String getRedirectUri() {
        return (String)getSession(true).getAttribute(SAML_REDIRECT_URI);
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
