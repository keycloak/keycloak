package org.keycloak.adapters.jetty;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiMap;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.KeycloakAccount;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OAuthRequestAuthenticator;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.enums.TokenStore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JettyRequestAuthenticator extends RequestAuthenticator {

    private static final Logger log = Logger.getLogger(JettyRequestAuthenticator.class);
    protected KeycloakJettyAuthenticator valve;
    protected Request request;
    protected KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal;

    public JettyRequestAuthenticator(KeycloakDeployment deployment,
                                     KeycloakJettyAuthenticator valve, AdapterTokenStore tokenStore,
                                     JettyHttpFacade facade,
                                     Request request) {
        super(facade, deployment, tokenStore, -1);
        this.valve = valve;
        this.request = request;
    }

    @Override
    protected OAuthRequestAuthenticator createOAuthAuthenticator() {
        return new OAuthRequestAuthenticator(this, facade, deployment, sslRedirectPort) {
            @Override
            protected void saveRequest() {
                if (deployment.getTokenStore() == TokenStore.SESSION) {
                    saveServletRequest(request, request.getSession());
                }
            }
        };
    }

    protected void saveServletRequest(HttpServletRequest request, HttpSession session) {
        // remember the current URI
        synchronized (session) {
            // But only if it is not set already, or we save every uri that leads to a login form redirect
            if (session.getAttribute(FormAuthenticator.__J_URI) == null) {
                StringBuffer buf = request.getRequestURL();
                if (request.getQueryString() != null)
                    buf.append("?").append(request.getQueryString());
                session.setAttribute(FormAuthenticator.__J_URI, buf.toString());
                session.setAttribute(FormAuthenticator.__J_METHOD, request.getMethod());

                if (MimeTypes.Type.FORM_ENCODED.is(request.getContentType()) && HttpMethod.POST.is(request.getMethod())) {
                    Request base_request = (request instanceof Request) ? (Request) request : HttpChannel
                            .getCurrentHttpChannel().getRequest();
                    base_request.extractParameters();
                    session.setAttribute(FormAuthenticator.__J_POST, new MultiMap<String>(base_request.getParameters()));
                }
            }
        }
    }


    @Override
    protected void completeOAuthAuthentication(final KeycloakPrincipal<RefreshableKeycloakSecurityContext> skp) {
        principal = skp;
        final RefreshableKeycloakSecurityContext securityContext = skp.getKeycloakSecurityContext();
        final Set<String> roles = AdapterUtils.getRolesFromSecurityContext(securityContext);
        KeycloakAccount account = new KeycloakAccount() {

            @Override
            public Principal getPrincipal() {
                return skp;
            }

            @Override
            public Set<String> getRoles() {
                return roles;
            }

            @Override
            public KeycloakSecurityContext getKeycloakSecurityContext() {
                return securityContext;
            }

        };
        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
        this.tokenStore.saveAccountInfo(account);
    }

    @Override
    protected void completeBearerAuthentication(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
        this.principal = principal;
        RefreshableKeycloakSecurityContext securityContext = principal.getKeycloakSecurityContext();
        Set<String> roles = AdapterUtils.getRolesFromSecurityContext(securityContext);
        if (log.isDebugEnabled()) {
            log.debug("Completing bearer authentication. Bearer roles: " + roles);
        }
        request.setAttribute(KeycloakSecurityContext.class.getName(), securityContext);
    }

    protected void restoreRequest() {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        synchronized (session) {
            String j_uri = (String) session.getAttribute(FormAuthenticator.__J_URI);
            if (j_uri != null) {
                // check if the request is for the same url as the original and restore
                // params if it was a post
                StringBuffer buf = request.getRequestURL();
                if (request.getQueryString() != null)
                    buf.append("?").append(request.getQueryString());

                /*
                 * if (j_uri.equals(buf.toString())) {
                 */
                MultiMap<String> j_post = (MultiMap<String>) session.getAttribute(FormAuthenticator.__J_POST);
                if (j_post != null) {
                    Request base_request = HttpChannel.getCurrentHttpChannel().getRequest();
                    base_request.setParameters(j_post);
                }
                session.removeAttribute(FormAuthenticator.__J_URI);
                session.removeAttribute(FormAuthenticator.__J_METHOD);
                session.removeAttribute(FormAuthenticator.__J_POST);
                // }
            }
        }
    }

    @Override
    protected String getHttpSessionId(boolean create) {
        HttpSession session = request.getSession(create);
        return session != null ? session.getId() : null;
    }
}
