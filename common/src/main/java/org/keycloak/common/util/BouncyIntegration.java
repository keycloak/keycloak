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

import java.lang.reflect.Constructor;
import java.security.Provider;
import java.security.Security;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BouncyIntegration {

    private static final Logger log = Logger.getLogger(BouncyIntegration.class);

    private static final String[] providerClassNames = {
            "org.bouncycastle.jce.provider.BouncyCastleProvider",
            "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider"
    };

    public static final String PROVIDER = loadProvider();

    private static String loadProvider() {
        for (String providerClassName : providerClassNames) {
            try {
                Class<?> providerClass = Class.forName(providerClassName, true, BouncyIntegration.class.getClassLoader());
                Constructor<Provider> constructor = (Constructor<Provider>) providerClass.getConstructor();
                Provider provider = constructor.newInstance();

                if (Security.getProvider(provider.getName()) == null) {
                    Security.addProvider(provider);
                    log.debugv("Loaded {0} security provider", providerClassName);
                } else {
                    log.debugv("Security provider {0} already loaded", providerClassName);
                }

                return provider.getName();
            } catch (Exception e) {
                log.debugv("Failed to load {0}", e, providerClassName);
            }
        }

        throw new RuntimeException("Failed to load required security provider: BouncyCastleProvider or BouncyCastleFipsProvider");
    }

}
