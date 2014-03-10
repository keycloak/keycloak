package org.keycloak.adapters.as7;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.LoginConfig;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.adapters.AuthChallenge;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.representations.adapters.action.AdminAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.SessionStats;
import org.keycloak.representations.adapters.action.SessionStatsAction;
import org.keycloak.representations.adapters.action.UserStats;
import org.keycloak.representations.adapters.action.UserStatsAction;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.StreamUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Web deployment whose security is managed by a remote OAuth Skeleton Key authentication server
 * <p/>
 * Redirects browser to remote authentication server if not logged in.  Also allows OAuth Bearer Token requests
 * that contain a Skeleton Key bearer tokens.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticatorValve extends FormAuthenticator implements LifecycleListener {
    private static final Logger log = Logger.getLogger(KeycloakAuthenticatorValve.class);
    protected CatalinaUserSessionManagement userSessionManagement = new CatalinaUserSessionManagement();
    protected KeycloakDeployment deployment;


    @Override
    public void start() throws LifecycleException {
        super.start();
        StandardContext standardContext = (StandardContext) context;
        standardContext.addLifecycleListener(this);
        cache = false;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getType() == Lifecycle.AFTER_START_EVENT) init();
    }

    private static InputStream getJSONFromServletContext(ServletContext servletContext) {
        String json = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (json == null) {
            return null;
        }
        log.info("**** using " + AdapterConstants.AUTH_DATA_PARAM_NAME);
        log.info(json);
        return new ByteArrayInputStream(json.getBytes());
    }
    private static InputStream getConfigInputStream(Context context) {
        InputStream is = getJSONFromServletContext(context.getServletContext());
        if (is == null) {
            String path = context.getServletContext().getInitParameter("keycloak.config.file");
            if (path == null) {
                log.info("**** using /WEB-INF/keycloak.json");
                is = context.getServletContext().getResourceAsStream("/WEB-INF/keycloak.json");
            } else {
                try {
                    is = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return is;
    }


    protected void init() {
        this.deployment = KeycloakDeploymentBuilder.build(getConfigInputStream(context));
        AuthenticatedActionsValve actions = new AuthenticatedActionsValve(deployment, getNext(), getContainer(), getController());
        setNext(actions);
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            PreAuthActionsHandler handler = new PreAuthActionsHandler(userSessionManagement, deployment, new CatalinaHttpFacade(request, response));
            if (handler.handleRequest()) {
                return;
            }
            checkKeycloakSession(request);
            super.invoke(request, response);
        } finally {
        }
    }

    @Override
    public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        CatalinaHttpFacade facade = new CatalinaHttpFacade(request, response);
        CatalinaRequestAuthenticator authenticator = new CatalinaRequestAuthenticator(deployment, this, userSessionManagement, facade, request);
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            if (facade.isEnded()) {
                return false;
            }
            return true;
        }
        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            challenge.challenge(facade);
        }
        return false;
     }

    /**
     * Checks that access token is still valid.  Will attempt refresh of token if it is not.
     *
     * @param request
     */
    protected void checkKeycloakSession(Request request) {
        if (request.getSessionInternal(false) == null || request.getSessionInternal().getPrincipal() == null) return;
        RefreshableKeycloakSecurityContext session = (RefreshableKeycloakSecurityContext)request.getSessionInternal().getNote(KeycloakSecurityContext.class.getName());
        if (session == null) return;
        // just in case session got serialized
        session.setDeployment(deployment);
        if (session.isActive()) return;

        // FYI: A refresh requires same scope, so same roles will be set.  Otherwise, refresh will fail and token will
        // not be updated
        session.refreshExpiredToken();
        if (session.isActive()) return;

        request.getSessionInternal().removeNote(KeycloakSecurityContext.class.getName());
        request.setUserPrincipal(null);
        request.setAuthType(null);
        request.getSessionInternal().setPrincipal(null);
        request.getSessionInternal().setAuthType(null);
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

}
