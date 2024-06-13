/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.adapters.authorization.integration.elytron;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.keycloak.adapters.authorization.TokenPrincipal;
import org.keycloak.adapters.authorization.spi.HttpRequest;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ServletHttpRequest implements HttpRequest {

    private final HttpServletRequest request;
    private final TokenPrincipal tokenPrincipal;
    private InputStream inputStream;

    public ServletHttpRequest(HttpServletRequest request, TokenPrincipal tokenPrincipal) {
        this.request = request;
        this.tokenPrincipal = tokenPrincipal;
    }

    @Override
    public String getRelativePath() {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String servletPath = uri.substring(uri.indexOf(contextPath) + contextPath.length());

        if ("".equals(servletPath)) {
            servletPath = "/";
        }

        return servletPath;
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getURI() {
        return request.getRequestURI();
    }

    @Override
    public List<String> getHeaders(String name) {
        return Collections.list(request.getHeaders(name));
    }

    @Override
    public String getFirstParam(String name) {
        Map<String, String[]> parameters = request.getParameterMap();
        String[] values = parameters.get(name);

        if (values == null || values.length == 0) {
            return null;
        }

        return values[0];
    }

    @Override
    public String getCookieValue(String name) {
        Cookie[] cookies = request.getCookies();

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }

        return null;
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    @Override
    public boolean isSecure() {
        return request.isSecure();
    }

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
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
    public TokenPrincipal getPrincipal() {
        return tokenPrincipal;
    }
}
