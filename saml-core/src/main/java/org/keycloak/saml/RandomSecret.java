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

package org.keycloak.saml;

import java.security.SecureRandom;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RandomSecret {
    /**
     * <p>
     * Creates a random {@code byte[]} secret of the specified size.
     * </p>
     *
     * @param size the size of the secret to be created, in bytes.
     *
     * @return a {@code byte[]} containing the generated secret.
     */
    public static byte[] createRandomSecret(final int size) {
        SecureRandom random = new SecureRandom();
        byte[] secret = new byte[size];
        random.nextBytes(secret);
        return secret;
    }
}
