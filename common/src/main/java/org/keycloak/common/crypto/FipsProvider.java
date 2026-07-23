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

package org.keycloak.common.crypto;

public enum FipsProvider {
    AUTO("auto"),
    BOUNCY_CASTLE("bouncycastle"),
    GLASSLESS("glassless");

    private static final String BOUNCY_CASTLE_FIPS_PROVIDER =
            "org/bouncycastle/jcajce/provider/BouncyCastleFipsProvider.class";

    private final String optionName;

    FipsProvider(String optionName) {
        this.optionName = optionName;
    }

    public static FipsProvider valueOfOption(String name) {
        for (FipsProvider provider : values()) {
            if (provider.optionName.equals(name)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown FIPS provider: " + name);
    }

    public FipsProvider resolve(ClassLoader classLoader) {
        if (this != AUTO) {
            return this;
        }
        return classLoader.getResource(BOUNCY_CASTLE_FIPS_PROVIDER) == null ? GLASSLESS : BOUNCY_CASTLE;
    }

    @Override
    public String toString() {
        return optionName;
    }
}
