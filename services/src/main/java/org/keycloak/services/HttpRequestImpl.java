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

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.keycloak.http.FormPartValue;
import org.keycloak.http.HttpRequest;

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

            for (Map.Entry<String, List<InputPart>> entry : inputs.getFormDataMap().entrySet()) {
                for (InputPart value : entry.getValue()) {
                    MediaType valueMediaType = value.getMediaType();

                    if (TEXT_PLAIN_TYPE.isCompatible(valueMediaType)) {
                        parts.add(entry.getKey(), new FormPartValueImpl(value.getBodyAsString()));
                    } else {
                        parts.add(entry.getKey(), new FormPartValueImpl(value.getBody(InputStream.class, null)));
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
