package org.keycloak.adapters.springsecurity.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Provides a Keycloak {@link AuthenticationEntryPoint authentication entry point}.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Default Keycloak authentication login URI
     */
    public static final String DEFAULT_LOGIN_URI = "/sso/login";

    private final static Logger log = LoggerFactory.getLogger(KeycloakAuthenticationEntryPoint.class);

    private String loginUri = DEFAULT_LOGIN_URI;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

        String contextAwareLoginUri = request.getContextPath() + loginUri;

        log.debug("Redirecting to login URI {}", contextAwareLoginUri);
        response.sendRedirect(contextAwareLoginUri);
    }

    public void setLoginUri(String loginUri) {
        Assert.notNull(loginUri, "loginUri cannot be null");
        this.loginUri = loginUri;
    }
}
