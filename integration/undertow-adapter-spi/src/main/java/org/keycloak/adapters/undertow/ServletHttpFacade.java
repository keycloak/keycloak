package org.keycloak.adapters.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletHttpFacade extends UndertowHttpFacade {
    protected HttpServletRequest request;
    protected HttpServletResponse response;

    public ServletHttpFacade(HttpServerExchange exchange) {
        super(exchange);
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        request = (HttpServletRequest)servletRequestContext.getServletRequest();
    }

    protected class RequestFacade extends UndertowHttpFacade.RequestFacade {
        @Override
        public String getFirstParam(String param) {
            return request.getParameter(param);
        }

    }

    @Override
    public Request getRequest() {
        return new RequestFacade();
    }
}
