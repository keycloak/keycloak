package org.keycloak.adapters.springsecurity.facade;

import org.keycloak.adapters.HttpFacade.Response;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Concrete Keycloak {@link Response response} implementation wrapping an {@link HttpServletResponse}.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
class WrappedHttpServletResponse implements Response {

    private final HttpServletResponse response;

    /**
     * Creates a new response for the given <code>HttpServletResponse</code>.
     *
     * @param response the current <code>HttpServletResponse</code> (required)
     */
    public WrappedHttpServletResponse(HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public void resetCookie(String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        if (path != null) {
            cookie.setPath(path);
        }
        response.addCookie(cookie);
    }

    @Override
    public void setCookie(String name, String value, String path, String domain, int maxAge, boolean secure, boolean httpOnly) {
        Cookie cookie = new Cookie(name, value);

        if (path != null) {
            cookie.setPath(path);
        }

        if (domain != null) {
            cookie.setDomain(domain);
        }

        cookie.setMaxAge(maxAge);
        cookie.setSecure(secure);
        cookie.setHttpOnly(httpOnly);

        response.addCookie(cookie);
    }

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
    public OutputStream getOutputStream() {
        try {
            return response.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException("Unable to return response output stream", e);
        }
    }

    @Override
    public void sendError(int code, String message) {
        try {
            response.sendError(code, message);
        } catch (IOException e) {
            throw new RuntimeException("Unable to set HTTP status", e);
        }
    }

    @Override
    public void end() {
        // TODO: do we need this?
    }
}
