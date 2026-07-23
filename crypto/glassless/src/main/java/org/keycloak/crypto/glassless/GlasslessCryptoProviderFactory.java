/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.crypto.glassless;

import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.crypto.CryptoProviderFactory;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.common.crypto.FipsProvider;

public class GlasslessCryptoProviderFactory implements CryptoProviderFactory {

    @Override
    public String getName() {
        return FipsProvider.GLASSLESS.toString();
    }

    @Override
    public CryptoProvider create(FipsMode fipsMode) {
        return switch (fipsMode) {
            case NON_STRICT -> new GlasslessCryptoProvider();
            case STRICT -> new GlasslessStrictCryptoProvider();
            case DISABLED -> throw new IllegalArgumentException("The Glassless provider requires FIPS mode");
        };
    }
}
