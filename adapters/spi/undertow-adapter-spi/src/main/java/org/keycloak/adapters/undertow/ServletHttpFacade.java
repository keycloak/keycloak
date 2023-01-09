/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.LogoutError;

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
        response = (HttpServletResponse)servletRequestContext.getServletResponse();
    }

    protected class RequestFacade extends UndertowHttpFacade.RequestFacade {
        @Override
        public String getFirstParam(String param) {
            return request.getParameter(param);
        }

        @Override
        public void setError(AuthenticationError error) {
            request.setAttribute(AuthenticationError.class.getName(), error);

        }

        @Override
        public void setError(LogoutError error) {
            request.setAttribute(LogoutError.class.getName(), error);
        }


    }

    protected class ResponseFacade extends UndertowHttpFacade.ResponseFacade {
        // can't call sendError from a challenge.  Undertow ends up calling send error.
        /*
        @Override
        public void sendError(int code) {
            try {
                response.sendError(code);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void sendError(int code, String message) {
            try {
                response.sendError(code, message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        */

    }

    @Override
    public Response getResponse() {
        return new ResponseFacade();
    }

    @Override
    public Request getRequest() {
        return new RequestFacade();
    }
}
