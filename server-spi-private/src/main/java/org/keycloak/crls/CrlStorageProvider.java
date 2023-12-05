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
package org.keycloak.crls;

import org.keycloak.provider.Provider;

import javax.security.auth.x500.X500Principal;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Crl Storage Provider interface
 *
 * @author Joshua Smith
 * @author Scott Tustison
 */
public interface CrlStorageProvider extends Provider {

    /**
     * Given a list of urls, find and load the one that matches the given X500Principal
     * @param urls
     * @param issuer
     * @return
     * @throws GeneralSecurityException
     */
    CrlEntry get(List<String> urls, X500Principal issuer) throws GeneralSecurityException;

    /**
     * Load the CRL from the given URL
     * @param url
     * @return
     * @throws GeneralSecurityException
     */
    void refreshCache(String url) throws GeneralSecurityException;

}
