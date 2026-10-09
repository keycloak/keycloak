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
import jakarta.ws.rs.client.WebTarget;

/**
 * An SPI for using the JAX-RS Client API regardless of the underlying stack.
 */
public interface ResteasyClientProvider {

    /**
     * Creates a new {@link Client}.
     *
     * @param messageHandler a {@link jakarta.ws.rs.ext.MessageBodyReader} and/or {@link jakarta.ws.rs.ext.MessageBodyWriter} instance.
     * @param sslContext an optional {@link SSLContext}
     * @param disableTrustManager if the client should not validate the server certificates when using TLS
     * @return
     */
    Client newRestEasyClient(Object messageHandler, SSLContext sslContext, boolean disableTrustManager);

    /**
     * Creates a implementation-specific proxy for a given {@code targetClass}.
     *
     * @param target the {@link WebTarget} instance
     * @param targetClass the JAX-RS client resource class
     * @return an instance of {@code targetClass}
     */
    <R> R targetProxy(WebTarget target, Class<R> targetClass);
}
