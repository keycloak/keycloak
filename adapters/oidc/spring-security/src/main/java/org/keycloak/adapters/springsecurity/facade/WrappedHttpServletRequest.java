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

import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade.Cookie;
import org.keycloak.adapters.spi.HttpFacade.Request;
import org.keycloak.adapters.spi.LogoutError;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Concrete Keycloak {@link Request request} implementation wrapping an {@link HttpServletRequest}.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
class WrappedHttpServletRequest implements Request {

    private final HttpServletRequest request;
    private InputStream inputStream;

    /**
     * Creates a new request for the given <code>HttpServletRequest</code>
     *
     * @param request the current <code>HttpServletRequest</code> (required)
     */
    public WrappedHttpServletRequest(HttpServletRequest request) {
        Assert.notNull(request, "HttpServletRequest required");
        this.request = request;
    }

    @Override
    public String getFirstParam(String param) {
        return request.getParameter(param);
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getURI() {
        StringBuffer buf = request.getRequestURL();
        if (request.getQueryString() != null) {
            buf.append('?').append(request.getQueryString());
        }
        return buf.toString();
    }

    @Override
    public String getRelativePath() {
        return request.getServletPath();
    }

    @Override
    public boolean isSecure() {
        return request.isSecure();
    }

    @Override
    public String getQueryParamValue(String param) {
        return request.getParameter(param);
    }

    @Override
    public Cookie getCookie(String cookieName) {

        javax.servlet.http.Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (javax.servlet.http.Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(cookieName)) {
                return new Cookie(cookie.getName(), cookie.getValue(), cookie.getVersion(), cookie.getDomain(), cookie.getPath());
            }
        }

        return null;
    }

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        Enumeration<String> values = request.getHeaders(name);
        List<String> array = new ArrayList<String>();

        while (values.hasMoreElements()) {
            array.add(values.nextElement());
        }

        return Collections.unmodifiableList(array);
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
            try {
                return inputStream = new BufferedInputStream(request.getInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            return request.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
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
