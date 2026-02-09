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

package org.keycloak.services.resteasy;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map.Entry;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Providers;

import org.keycloak.http.FormPartValue;
import org.keycloak.http.HttpRequest;
import org.keycloak.services.FormPartValueImpl;

import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;

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
        MediaType mediaType = getHttpHeaders().getMediaType();
        if (mediaType == null || !mediaType.isCompatible(MediaType.valueOf("application/x-www-form-urlencoded"))) {
            return new MultivaluedHashMap<>();
        }
        return delegate.getDecodedFormParameters();
    }

    @Override
    public MultivaluedMap<String, FormPartValue> getMultiPartFormParameters() {
        try {
            MediaType mediaType = getHttpHeaders().getMediaType();

            if (!MULTIPART_FORM_DATA_TYPE.isCompatible(mediaType) || !mediaType.getParameters().containsKey("boundary")) {
                return new MultivaluedHashMap<>();
            }

            Providers providers = ResteasyContext.getContextData(Providers.class);
            MessageBodyReader<MultipartFormDataInput> multiPartProvider = providers.getMessageBodyReader(
                    MultipartFormDataInput.class, null, null, MULTIPART_FORM_DATA_TYPE);
            MultipartFormDataInput inputs = multiPartProvider
                    .readFrom(null, null, null, mediaType, getHttpHeaders().getRequestHeaders(),
                            delegate.getInputStream());
            MultivaluedHashMap<String, FormPartValue> parts = new MultivaluedHashMap<>();

            for (Entry<String, Collection<FormValue>> entry : inputs.getValues().entrySet()) {
                for (FormValue value : entry.getValue()) {
                    if (!value.isFileItem()) {
                        parts.add(entry.getKey(), new FormPartValueImpl(value.getValue()));
                    } else {
                        parts.add(entry.getKey(), new FormPartValueImpl(value.getFileItem().getInputStream()));
                    }
                }
            }

            return parts;
        } catch (IOException cause) {
            throw new RuntimeException("Failed to parse multi part request", cause);
        }
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
        return (X509Certificate[]) delegate.getAttribute("jakarta.servlet.request.X509Certificate");
    }

    @Override
    public UriInfo getUri() {
        if (delegate == null) {
            return null;
        }
        return delegate.getUri();
    }

    @Override
    public boolean isProxyTrusted() {
        return true;
    }
}
