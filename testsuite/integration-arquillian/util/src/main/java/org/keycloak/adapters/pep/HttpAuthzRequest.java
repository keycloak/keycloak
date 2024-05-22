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

package org.keycloak.adapters.pep;

import java.io.InputStream;
import java.util.List;

import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.authorization.TokenPrincipal;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.adapters.spi.HttpFacade.Cookie;
import org.keycloak.representations.AccessToken;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class HttpAuthzRequest implements HttpRequest {

    private final TokenPrincipal tokenPrincipal;
    private final OIDCHttpFacade oidcFacade;

    public HttpAuthzRequest(OIDCHttpFacade oidcFacade) {
        this.oidcFacade = oidcFacade;
        tokenPrincipal = new TokenPrincipal() {
            @Override
            public String getRawToken() {
                KeycloakSecurityContext securityContext = oidcFacade.getSecurityContext();

                if (securityContext == null) {
                    return null;
                }

                return oidcFacade.getSecurityContext().getTokenString();
            }

            @Override
            public AccessToken getToken() {
                KeycloakSecurityContext securityContext = oidcFacade.getSecurityContext();

                if (securityContext == null) {
                    return null;
                }

                return securityContext.getToken();
            }
        };
    }

    @Override
    public String getRelativePath() {
        return oidcFacade.getRequest().getRelativePath();
    }

    @Override
    public String getMethod() {
        return oidcFacade.getRequest().getMethod();
    }

    @Override
    public String getURI() {
        return oidcFacade.getRequest().getURI();
    }

    @Override
    public List<String> getHeaders(String name) {
        return oidcFacade.getRequest().getHeaders(name);
    }

    @Override
    public String getFirstParam(String name) {
        String queryParamValue = oidcFacade.getRequest().getQueryParamValue(name);

        if (queryParamValue != null) {
            return queryParamValue;
        }

        return oidcFacade.getRequest().getFirstParam(name);
    }

    @Override
    public String getCookieValue(String name) {
        Cookie cookie = oidcFacade.getRequest().getCookie(name);

        if (cookie == null) {
            return null;
        }

        return cookie.getValue();
    }

    @Override
    public String getRemoteAddr() {
        return oidcFacade.getRequest().getRemoteAddr();
    }

    @Override
    public boolean isSecure() {
        return oidcFacade.getRequest().isSecure();
    }

    @Override
    public String getHeader(String name) {
        return oidcFacade.getRequest().getHeader(name);
    }

    @Override
    public InputStream getInputStream(boolean buffered) {
        return oidcFacade.getRequest().getInputStream(buffered);
    }

    @Override
    public TokenPrincipal getPrincipal() {
        return tokenPrincipal;
    }
}
