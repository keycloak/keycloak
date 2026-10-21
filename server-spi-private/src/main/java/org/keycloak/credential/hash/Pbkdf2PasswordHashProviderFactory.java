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

package org.keycloak.credential.hash;

import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

/**
 * Provider factory for SHA1 variant of the PBKDF2 password hash algorithm.
 *
 * @author <a href="mailto:me@tsudot.com">Kunal Kerkar</a>
 * @deprecated The PBKDF2 provider with SHA1 and the recommended number of 1.300.000 iterations is known to be very slow. We recommend to use the PBKDF2 variants with SHA256 or SHA512 instead.
 */
@Deprecated
public class Pbkdf2PasswordHashProviderFactory extends AbstractPbkdf2PasswordHashProviderFactory implements PasswordHashProviderFactory {

    private static final Logger LOG = Logger.getLogger(Pbkdf2PasswordHashProviderFactory.class);

    public static final String ID = "pbkdf2";

    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";

    /**
     * Hash iterations for PBKDF2-HMAC-SHA1 according to the <a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2">Password Storage Cheat Sheet</a>.
     */
    public static final int DEFAULT_ITERATIONS = 1_300_000;

    private static boolean usageWarningPrinted;

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        if (!usageWarningPrinted) {
            LOG.warnf("Detected usage of password hashing provider '%s'. The provider is no longer recommended, use 'pbkdf2-sha256' or 'pbkdf2-sha512' instead.", ID);
            usageWarningPrinted = true;
        }
        return new Pbkdf2PasswordHashProvider(ID, PBKDF2_ALGORITHM, DEFAULT_ITERATIONS, getMaxPaddingLength());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int order() {
        return -100;
    }
}
