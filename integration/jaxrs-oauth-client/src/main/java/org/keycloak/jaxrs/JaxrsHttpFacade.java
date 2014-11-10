package org.keycloak.jaxrs;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.security.cert.X509Certificate;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.util.HostUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JaxrsHttpFacade implements HttpFacade {

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

    protected class RequestFacade implements HttpFacade.Request {

        @Override
        public String getMethod() {
            return requestContext.getMethod();
        }

        @Override
        public String getURI() {
            return requestContext.getUriInfo().getRequestUri().toString();
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
            return requestContext.getEntityStream();
        }

        @Override
        public String getRemoteAddr() {
            // TODO: implement properly
            return HostUtils.getIpAddress();
        }
    }

    protected class ResponseFacade implements HttpFacade.Response {

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
