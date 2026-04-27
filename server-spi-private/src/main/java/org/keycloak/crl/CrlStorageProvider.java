/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.crl;

import java.security.GeneralSecurityException;
import java.security.cert.X509CRL;
import java.util.concurrent.Callable;

import org.keycloak.provider.Provider;

/**
 * Crl Storage Provider interface
 *
 * @author rmartinc
 */
public interface CrlStorageProvider extends Provider {

    /**
     * Returns the CRL for this key from cache or loading from the loader.
     * @param key The key for the CRL
     * @param loader The loader to get if the CRL is not in cache
     * @return The X509CRL placed in the cache
     * @throws GeneralSecurityException
     */
    X509CRL get(String key, Callable<X509CRL> loader) throws GeneralSecurityException;

    /**
     * Refreshes the CRL in the cache for this key.
     * @param key The key for the CRL
     * @param loader The loader to get the CRL
     * @return true if refreshed or false if not
     * @throws GeneralSecurityException
     */
    boolean refreshCache(String key, Callable<X509CRL> loader) throws GeneralSecurityException;

}
