package org.keycloak.adapters.springsecurity.authentication;

import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.springsecurity.AdapterDeploymentContextBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

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

    public static final String SSO_LOGOUT_COMPLETE_PARAM = "sso_complete";
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

        if (Boolean.valueOf(request.getParameter(SSO_LOGOUT_COMPLETE_PARAM))) {
            // already logged out
            return;
        }

        try {
            handleSingleSignOut(request, response);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to redirect to SSO url!", e);
        }

    }

    protected String createRedirectUrl(HttpServletRequest request) {

        return UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString())
                .replaceQueryParam(SSO_LOGOUT_COMPLETE_PARAM, true).build().toUriString();
    }

    protected void handleSingleSignOut(HttpServletRequest request, HttpServletResponse response) throws IOException {

        KeycloakDeployment deployment = deploymentContextBean.getDeployment();
        String redirectUrl = createRedirectUrl(request);

        response.sendRedirect(deployment.getLogoutUrl().queryParam("redirect_uri", redirectUrl).build().toASCIIString());
    }
}
