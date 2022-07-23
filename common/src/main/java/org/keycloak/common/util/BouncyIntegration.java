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

package org.keycloak.common.util;

import org.jboss.logging.Logger;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoConstants;

import java.security.Provider;
import java.security.Security;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BouncyIntegration {

    private static final Logger log = Logger.getLogger(BouncyIntegration.class);

    public static final String PROVIDER = loadProvider();

    private static String loadProvider() {
        Provider provider = CryptoIntegration.getProvider().getBouncyCastleProvider();
        if (provider == null) {
            throw new RuntimeException("Failed to load required security provider: BouncyCastleProvider or BouncyCastleFipsProvider");
        }
        if (Security.getProvider(provider.getName()) == null) {
            Security.addProvider(provider);
            log.debugv("Loaded {0} security provider", provider.getClass().getName());
        } else {
            log.debugv("Security provider {0} already loaded", provider.getClass().getName());
        }
        return provider.getName();
    }

}
