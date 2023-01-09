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

package org.keycloak.adapters.springsecurity.facade;

import org.keycloak.adapters.spi.HttpFacade.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;

/**
 * Concrete Keycloak {@link Response response} implementation wrapping an {@link HttpServletResponse}.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
class WrappedHttpServletResponse implements Response {

    private static final Logger log = LoggerFactory.getLogger(WrappedHttpServletResponse.class);
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
        this.setHttpOnly(cookie, httpOnly);

        response.addCookie(cookie);
    }

    private void setHttpOnly(Cookie cookie, boolean httpOnly) {
        Method method;
        try {
            method = Cookie.class.getMethod("setHttpOnly", boolean.class);
            method.invoke(cookie, httpOnly);
        } catch (NoSuchMethodException e) {
            log.warn("Unable to set httpOnly on cookie [{}]; no such method on javax.servlet.http.Cookie", cookie.getName());
        } catch (ReflectiveOperationException e) {
            log.error("Unable to set httpOnly on cookie [{}]", cookie.getName(), e);
        }
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
    public void sendError(int code) {
        try {
            response.sendError(code);
        } catch (IOException e) {
            throw new RuntimeException("Unable to set HTTP status", e);
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
