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

package org.keycloak.jaxrs;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.common.util.HostUtils;

import javax.security.cert.X509Certificate;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @deprecated Class is deprecated and may be removed in the future. If you want to maintain this class for Keycloak community, please
 * contact Keycloak team on keycloak-dev mailing list. You can fork it into your github repository and
 * Keycloak team will reference it from "Keycloak Extensions" page.
 */
@Deprecated
public class JaxrsHttpFacade implements OIDCHttpFacade {

    protected final ContainerRequestContext requestContext;
    protected final SecurityContext securityContext;
    protected final RequestFacade requestFacade = new RequestFacade();
    protected final ResponseFacade responseFacade = new ResponseFacade();
    protected KeycloakSecurityContext keycloakSecurityContext;
    protected boolean responseFinished;

    public JaxrsHttpFacade(ContainerRequestContext containerRequestContext, SecurityContext securityContext) {
        this.requestContext = containerRequestContext;
        this.securityContext = securityContext;
    }

    protected class RequestFacade implements OIDCHttpFacade.Request {

        private InputStream inputStream;

        @Override
        public String getFirstParam(String param) {
            throw new RuntimeException("NOT IMPLEMENTED");
        }

        @Override
        public String getMethod() {
            return requestContext.getMethod();
        }

        @Override
        public String getURI() {
            return requestContext.getUriInfo().getRequestUri().toString();
        }

        @Override
        public String getRelativePath() {
            return requestContext.getUriInfo().getPath();
        }

        @Override
        public boolean isSecure() {
            return securityContext.isSecure();
        }

        @Override
        public String getQueryParamValue(String param) {
            MultivaluedMap<String, String> queryParams = requestContext.getUriInfo().getQueryParameters();
            if (queryParams == null)
                return null;
            return queryParams.getFirst(param);
        }

        @Override
        public Cookie getCookie(String cookieName) {
            Map<String, javax.ws.rs.core.Cookie> cookies = requestContext.getCookies();
            if (cookies == null)
                return null;
            javax.ws.rs.core.Cookie cookie = cookies.get(cookieName);
            if (cookie == null)
                return null;
            return new Cookie(cookie.getName(), cookie.getValue(), cookie.getVersion(), cookie.getDomain(), cookie.getPath());
        }

        @Override
        public String getHeader(String name) {
            return requestContext.getHeaderString(name);
        }

        @Override
        public List<String> getHeaders(String name) {
            MultivaluedMap<String, String> headers = requestContext.getHeaders();
            return (headers == null) ? null : headers.get(name);
        }

        @Override
        public InputStream getInputStream() {
            return getInputStream(false);
        }

        @Override
        public InputStream getInputStream(boolean buffered) {
            if (inputStream != null) {
                return inputStream;
            }

            if (buffered) {
                return inputStream = new BufferedInputStream(requestContext.getEntityStream());
            }

            return requestContext.getEntityStream();
        }

        @Override
        public String getRemoteAddr() {
            // TODO: implement properly
            return HostUtils.getIpAddress();
        }

        @Override
        public void setError(AuthenticationError error) {
            requestContext.setProperty(AuthenticationError.class.getName(), error);
        }

        @Override
        public void setError(LogoutError error) {
            requestContext.setProperty(LogoutError.class.getName(), error);

        }
    }

    protected class ResponseFacade implements OIDCHttpFacade.Response {

        private javax.ws.rs.core.Response.ResponseBuilder responseBuilder = javax.ws.rs.core.Response.status(204);

        @Override
        public void setStatus(int status) {
            responseBuilder.status(status);
        }

        @Override
        public void addHeader(String name, String value) {
            responseBuilder.header(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            responseBuilder.header(name, value);
        }

        @Override
        public void resetCookie(String name, String path) {
            // For now doesn't need to be supported
            throw new IllegalStateException("Not supported yet");
        }

        @Override
        public void setCookie(String name, String value, String path, String domain, int maxAge, boolean secure, boolean httpOnly) {
            // For now doesn't need to be supported
            throw new IllegalStateException("Not supported yet");
        }

        @Override
        public OutputStream getOutputStream() {
            // For now doesn't need to be supported
            throw new IllegalStateException("Not supported yet");
        }

        @Override
        public void sendError(int code) {
            javax.ws.rs.core.Response response = responseBuilder.status(code).build();
            requestContext.abortWith(response);
            responseFinished = true;
        }

        @Override
        public void sendError(int code, String message) {
            javax.ws.rs.core.Response response = responseBuilder.status(code).entity(message).build();
            requestContext.abortWith(response);
            responseFinished = true;
        }

        @Override
        public void end() {
            javax.ws.rs.core.Response response = responseBuilder.build();
            requestContext.abortWith(response);
            responseFinished = true;
        }
    }

    @Override
    public KeycloakSecurityContext getSecurityContext() {
        return keycloakSecurityContext;
    }

    public void setSecurityContext(KeycloakSecurityContext securityContext) {
        this.keycloakSecurityContext = securityContext;
    }

    @Override
    public Request getRequest() {
        return requestFacade;
    }

    @Override
    public Response getResponse() {
        return responseFacade;
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        throw new IllegalStateException("Not supported yet");
    }

    public boolean isResponseFinished() {
        return responseFinished;
    }
}
