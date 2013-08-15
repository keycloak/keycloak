package org.keycloak.services.resources.flows;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.JspRequestParameters;

public class PageFlows {

    private static final Logger log = Logger.getLogger(PageFlows.class);

    private HttpRequest request;

    PageFlows(HttpRequest request) {
        this.request = request;
    }

    public Response forwardToSecurityFailure(String message) {
        log.error(message);

        request.setAttribute(JspRequestParameters.KEYCLOAK_SECURITY_FAILURE_MESSAGE, message);

        request.forward(Pages.SECURITY_FAILURE);
        return null;
    }

}
