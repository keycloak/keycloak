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

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import javax.security.cert.X509Certificate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple {@link org.keycloak.adapters.OIDCHttpFacade} wrapping an {@link HttpServletRequest} and {@link HttpServletResponse}.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class SimpleHttpFacade implements OIDCHttpFacade {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    /**
     * Creates a new simple HTTP facade for the given request and response.
     *
     * @param request the current <code>HttpServletRequest</code> (required)
     * @param response the current <code>HttpServletResponse</code> (required)
     */
    public SimpleHttpFacade(HttpServletRequest request, HttpServletResponse response) {
        Assert.notNull(request, "HttpServletRequest required");
        Assert.notNull(response, "HttpServletResponse required");
        this.request = request;
        this.response = response;
    }

    @Override
    public KeycloakSecurityContext getSecurityContext() {

        SecurityContext context = SecurityContextHolder.getContext();

        if (context != null && context.getAuthentication() != null) {
            KeycloakAuthenticationToken authentication = (KeycloakAuthenticationToken) context.getAuthentication();
            return authentication.getAccount().getKeycloakSecurityContext();
        }

        return null;
    }

    @Override
    public Request getRequest() {
        return new WrappedHttpServletRequest(request);
    }

    @Override
    public Response getResponse() {
        return new WrappedHttpServletResponse(response);
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        // TODO: implement me
        return new X509Certificate[0];
    }
}
