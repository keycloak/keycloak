package org.keycloak.adapters.as7;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.HttpFacade;

import javax.security.cert.X509Certificate;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CatalinaHttpFacade implements HttpFacade {
    protected org.apache.catalina.connector.Request request;
    protected HttpServletResponse response;
    protected RequestFacade requestFacade = new RequestFacade();
    protected ResponseFacade responseFacade = new ResponseFacade();

    protected class RequestFacade implements Request {
        @Override
        public String getURI() {
            StringBuffer buf = request.getRequestURL();
            if (request.getQueryString() != null) {
                buf.append('?').append(request.getQueryString());
            }
            return buf.toString();
        }

        @Override
        public boolean isSecure() {
            return request.isSecure();
        }

        @Override
        public String getQueryParamValue(String paramName) {
            return request.getParameter(paramName);
        }

        @Override
        public Cookie getCookie(String cookieName) {
            if (request.getCookies() == null) return null;
            javax.servlet.http.Cookie cookie = null;
            for (javax.servlet.http.Cookie c : request.getCookies()) {
                if (c.getName().equals(cookieName)) {
                    cookie = c;
                    break;
                }
            }
            if (cookie == null) return null;
            return new Cookie(cookie.getName(), cookie.getValue(), cookie.getVersion(), cookie.getDomain(), cookie.getPath());
        }

        @Override
        public List<String> getHeaders(String name) {
            Enumeration<String> headers = request.getHeaders(name);
            if (headers == null) return null;
            List<String> list = new ArrayList<String>();
            while (headers.hasMoreElements()) {
                list.add(headers.nextElement());
            }
            return list;
        }

        @Override
        public InputStream getInputStream() {
            try {
                return request.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getMethod() {
            return request.getMethod();
        }

        @Override
        public String getHeader(String name) {
            return request.getHeader(name);
        }

        @Override
        public String getRemoteAddr() {
            return request.getRemoteAddr();
        }
    }

    protected class ResponseFacade implements Response {
        protected boolean ended;

        @Override
        public void setStatus(int status) {
            response.setStatus(status);
        }

        @Override
        public void addHeader(String name, String value) {
            response.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            response.setHeader(name, value);
        }

        @Override
        public void resetCookie(String name, String path) {
            setCookie(name, "", null, path, 0, false, false);
        }

        @Override
        public void setCookie(String name, String value, String path, String domain, int maxAge, boolean secure, boolean httpOnly) {
            javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie(name, value);
            if (domain != null) cookie.setDomain(domain);
            if (path != null) cookie.setPath(path);
            if (secure) cookie.setSecure(true);
            if (httpOnly) cookie.setHttpOnly(httpOnly);
            cookie.setMaxAge(maxAge);
            response.addCookie(cookie);
        }

        @Override
        public OutputStream getOutputStream() {
            try {
                return response.getOutputStream();
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

        @Override
        public void end() {
            ended = true;
        }

        public boolean isEnded() {
            return ended;
        }
    }

    public CatalinaHttpFacade(org.apache.catalina.connector.Request request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
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
    public KeycloakSecurityContext getSecurityContext() {
        return (KeycloakSecurityContext)request.getAttribute(KeycloakSecurityContext.class.getName());
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        throw new IllegalStateException("Not supported yet");
    }

    public boolean isEnded() {
        return responseFacade.isEnded();
    }
}
