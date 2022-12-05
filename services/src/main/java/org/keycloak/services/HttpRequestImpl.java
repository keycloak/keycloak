/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services;

import org.keycloak.http.HttpRequest;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.security.cert.X509Certificate;

public class HttpRequestImpl implements HttpRequest {

    private org.jboss.resteasy.spi.HttpRequest delegate;

    public HttpRequestImpl(org.jboss.resteasy.spi.HttpRequest delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getHttpMethod() {
        if (delegate == null) {
            return null;
        }
        return delegate.getHttpMethod();
    }

    @Override
    public MultivaluedMap<String, String> getDecodedFormParameters() {
        if (delegate == null) {
            return null;
        }
        return delegate.getDecodedFormParameters();
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        if (delegate == null) {
            return null;
        }
        return delegate.getHttpHeaders();
    }

    @Override
    public X509Certificate[] getClientCertificateChain() {
        if (delegate == null) {
            return null;
        }
        return (X509Certificate[]) delegate.getAttribute("javax.servlet.request.X509Certificate");
    }

    @Override
    public UriInfo getUri() {
        if (delegate == null) {
            return null;
        }
        return delegate.getUri();
    }
}
