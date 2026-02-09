/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.tracing;

import org.keycloak.connections.httpclient.HttpClientBuilder;

import io.opentelemetry.instrumentation.apachehttpclient.v4_3.ApacheHttpClientTelemetry;

/**
 * Get Apache HTTP client which is instrumented and used for tracing
 */
public class OTelHttpClientBuilder extends HttpClientBuilder {
    private final OTelTracingProvider provider;

    public OTelHttpClientBuilder(OTelTracingProvider provider) {
        this.provider = provider;
    }

    @Override
    protected org.apache.http.impl.client.HttpClientBuilder getApacheHttpClientBuilder() {
        return ApacheHttpClientTelemetry.builder(provider.getOpenTelemetry()).build().newHttpClientBuilder();
    }

}
