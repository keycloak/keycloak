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

package org.keycloak.quarkus.runtime.integration.resteasy;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Deque;
import java.util.Iterator;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.config.ProxyOptions;
import org.keycloak.http.FormPartValue;
import org.keycloak.http.HttpRequest;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.integration.jaxrs.EmptyMultivaluedMap;
import org.keycloak.services.FormPartValueImpl;

import io.vertx.ext.web.RoutingContext;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.multipart.FormValue;

public final class QuarkusHttpRequest implements HttpRequest {

    private static final MultivaluedMap<String, String> EMPTY_FORM_PARAM = new EmptyMultivaluedMap<>();
    private static final MultivaluedMap<String, FormPartValue> EMPTY_MULTI_MAP_MULTI_PART = new EmptyMultivaluedMap<>();

    private final ResteasyReactiveRequestContext context;

    private MultivaluedMap<String, String> decodedFormParameters;

    public <R> QuarkusHttpRequest(ResteasyReactiveRequestContext context) {
        this.context = context;
    }

    @Override
    public String getHttpMethod() {
        if (context == null) {
            return null;
        }
        return context.getMethod();
    }

    @Override
    public MultivaluedMap<String, String> getDecodedFormParameters() {
        if (context == null) {
            return null;
        }

        if (decodedFormParameters == null) {
            FormData parameters = context.getFormData();

            if (parameters == null || !parameters.iterator().hasNext()) {
                return EMPTY_FORM_PARAM;
            }

            decodedFormParameters = new QuarkusMultivaluedHashMap<>();

            for (String name : parameters) {
                Deque<FormValue> values = parameters.get(name);

                if (values == null || values.isEmpty()) {
                    continue;
                }

                for (FormValue value : values) {
                    decodedFormParameters.add(name, value.getValue());
                }
            }
        }

        return decodedFormParameters;
    }

    @Override
    public MultivaluedMap<String, FormPartValue> getMultiPartFormParameters() {
        if (context == null) {
            return null;
        }
        FormData formData = context.getFormData();

        if (formData == null) {
            return EMPTY_MULTI_MAP_MULTI_PART;
        }

        MultivaluedMap<String, FormPartValue> params = new QuarkusMultivaluedHashMap<>();

        for (String name : formData) {
            Deque<FormValue> formValues = formData.get(name);

            if (formValues != null) {
                Iterator<FormValue> iterator = formValues.iterator();

                while (iterator.hasNext()) {
                    FormValue formValue = iterator.next();

                    if (formValue.isFileItem()) {
                        try {
                            params.add(name, new FormPartValueImpl(formValue.getFileItem().getInputStream()));
                        } catch (IOException cause) {
                            throw new RuntimeException("Failed to parse multipart file parameter", cause);
                        }
                    } else {
                        params.add(name, new FormPartValueImpl(formValue.getValue()));
                    }
                }
            }
        }

        return params;
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        if (context == null) {
            return null;
        }
        return context.getHttpHeaders();
    }

    @Override
    public X509Certificate[] getClientCertificateChain() {
        Instance<RoutingContext> instances = CDI.current().select(RoutingContext.class);

        if (instances.isResolvable()) {
            RoutingContext context = instances.get();

            try {
                SSLSession sslSession = context.request().sslSession();

                if (sslSession == null) {
                    return null;
                }

                return (X509Certificate[]) sslSession.getPeerCertificates();
            } catch (SSLPeerUnverifiedException ignore) {
                // client not authenticated
            }
        }

        return null;
    }

    @Override
    public UriInfo getUri() {
        if (context == null) {
            return null;
        }
        return context.getUriInfo();
    }

    @Override
    public boolean isProxyTrusted() {
        boolean noTrustedProxies = Configuration.getOptionalKcValue(ProxyOptions.PROXY_TRUSTED_ADDRESSES).isEmpty();
        return noTrustedProxies
                || Boolean.parseBoolean(this.getHttpHeaders().getHeaderString("X-Forwarded-Trusted-Proxy"));
    }
}
