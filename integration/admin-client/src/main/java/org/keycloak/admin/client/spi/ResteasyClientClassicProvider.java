/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.admin.client.spi;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import org.keycloak.admin.client.ClientBuilderWrapper;
import org.keycloak.admin.client.JacksonProvider;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

/**
 * An implementation of {@link ResteasyClientProvider} based on RESTEasy classic.
 */
public class ResteasyClientClassicProvider implements ResteasyClientProvider {

    @Override
    public Client newRestEasyClient(Object customJacksonProvider, SSLContext sslContext, boolean disableTrustManager) {
        ClientBuilder clientBuilder = ClientBuilderWrapper.create(sslContext, disableTrustManager);

        if (customJacksonProvider != null) {
            clientBuilder.register(customJacksonProvider, 100);
        } else {
            clientBuilder.register(JacksonProvider.class, 100);
        }

        return clientBuilder.build();
    }

    @Override
    public <R> R targetProxy(WebTarget client, Class<R> targetClass) {
        return ResteasyWebTarget.class.cast(client).proxy(targetClass);
    }
}
