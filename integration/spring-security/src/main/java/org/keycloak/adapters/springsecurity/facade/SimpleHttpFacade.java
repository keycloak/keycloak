package org.keycloak.adapters.springsecurity.facade;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import javax.security.cert.X509Certificate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple {@link org.keycloak.adapters.OIDCHttpFacade} wrapping an {@link HttpServletRequest} and {@link HttpServletResponse}.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class SimpleHttpFacade implements OIDCHttpFacade {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    /**
     * Creates a new simple HTTP facade for the given request and response.
     *
     * @param request the current <code>HttpServletRequest</code> (required)
     * @param response the current <code>HttpServletResponse</code> (required)
     */
    public SimpleHttpFacade(HttpServletRequest request, HttpServletResponse response) {
        Assert.notNull(request, "HttpServletRequest required");
        Assert.notNull(response, "HttpServletResponse required");
        this.request = request;
        this.response = response;
    }

    @Override
    public KeycloakSecurityContext getSecurityContext() {

        SecurityContext context = SecurityContextHolder.getContext();

        if (context != null && context.getAuthentication() != null) {
            return (KeycloakSecurityContext) context.getAuthentication().getDetails();
        }

        return null;
    }

    @Override
    public Request getRequest() {
        return new WrappedHttpServletRequest(request);
    }

    @Override
    public Response getResponse() {
        return new WrappedHttpServletResponse(response);
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        // TODO: implement me
        return new X509Certificate[0];
    }
}
