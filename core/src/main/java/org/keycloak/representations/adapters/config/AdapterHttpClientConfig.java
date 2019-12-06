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

package org.keycloak.representations.adapters.config;

/**
 * Configuration options relevant for configuring http client that can be used by adapter.
 *
 * NOTE: keep in sync with adapters/saml/core/src/main/java/org/keycloak/adapters/AdapterHttpClientConfig.java until unified.
 *
 * @author hmlnarik
 */
public interface AdapterHttpClientConfig {

    /**
     * Returns truststore filename.
     */
    String getTruststore();

    /**
     * Returns truststore password.
     */
    String getTruststorePassword();

    /**
     * Returns keystore with client keys.
     */
    String getClientKeystore();

    /**
     * Returns keystore password.
     */
    String getClientKeystorePassword();

    /**
     * Returns boolean flag whether any hostname verification is done on the server's
     * certificate, {@code true} means that verification is not done.
     * @return
     */
    boolean isAllowAnyHostname();

    /**
     * Returns boolean flag whether any trust management and hostname verification is done.
     * <p>
     * <i>NOTE</i> Disabling trust manager is a security hole, so only set this option
     * if you cannot or do not want to verify the identity of the
     * host you are communicating with.
     */
    boolean isDisableTrustManager();

    /**
     * Returns size of connection pool.
     */
    int getConnectionPoolSize();

    /**
     * Returns URL of HTTP proxy.
     */
    String getProxyUrl();

}
