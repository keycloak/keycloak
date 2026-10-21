/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.credential.hash;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractPbkdf2PasswordHashProviderFactory implements PasswordHashProviderFactory {

    public static final String MAX_PADDING_LENGTH_PROPERTY = "max-padding-length";

    // Minimum password length before password is encoded. If the provided password is shorter than the configured count of characters by this option,
    // then the padding with '\0' character would be used. By default, it is 0, so no padding used.
    // This can be used as for example in fips mode (BCFIPS), the pbkdf2 function does not allow less than 14 characters (112 bits).
    // Regarding backwards compatibility, there is no issue with adding this option against already existing DB of passwords as password value without padding can be verified
    // against the password with padding as it produces same encoded value.
    private int maxPaddingLength = 0;

    @Override
    public void init(Config.Scope config) {
        this.maxPaddingLength = config.getInt(MAX_PADDING_LENGTH_PROPERTY, 0);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    public int getMaxPaddingLength() {
        return maxPaddingLength;
    }

    public void setMaxPaddingLength(int maxPaddingLength) {
        this.maxPaddingLength = maxPaddingLength;
    }
}
