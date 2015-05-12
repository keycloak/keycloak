package org.keycloak.adapters.springsecurity.authentication;

import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.AdapterDeploymentContextBean;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Logs the current user out of Keycloak.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class KeycloakLogoutHandler implements LogoutHandler {

    private static final Logger log = LoggerFactory.getLogger(KeycloakLogoutHandler.class);

    private AdapterDeploymentContextBean deploymentContextBean;

    public KeycloakLogoutHandler(AdapterDeploymentContextBean deploymentContextBean) {
        Assert.notNull(deploymentContextBean);
        this.deploymentContextBean = deploymentContextBean;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        if (authentication instanceof AnonymousAuthenticationToken) {
            log.warn("Attempt to log out an anonymous authentication");
            return;
        }

        try {
            handleSingleSignOut(request, response);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to make logout admin request!", e);
        }

    }

    protected void handleSingleSignOut(HttpServletRequest request, HttpServletResponse response) throws IOException {

        KeycloakAuthenticationToken authentication = (KeycloakAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        KeycloakDeployment deployment = deploymentContextBean.getDeployment();
        RefreshableKeycloakSecurityContext session = (RefreshableKeycloakSecurityContext) authentication.getAccount().getKeycloakSecurityContext();

        try {
            session.logout(deployment);
        } catch (Exception e) {
            log.error("Unable to complete Keycloak single sign out", e);
        }
    }
}
