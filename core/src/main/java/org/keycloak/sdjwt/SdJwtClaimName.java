/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.sdjwt;

/**
 * Strong typing claim name to avoid parameter mismatch.
 * 
 * Used as map key. Beware of the hashcode and equals implementation.
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class SdJwtClaimName {
    private final String claimName;

    public SdJwtClaimName(String claimName) {
        this.claimName = SdJwtUtils.requireNonEmpty(claimName, "claimName must not be empty");
    }

    public static SdJwtClaimName of(String claimName) {
        return new SdJwtClaimName(claimName);
    }

    @Override
    public String toString() {
        return claimName;
    }

    @Override
    public int hashCode() {
        return claimName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SdJwtClaimName) {
            return claimName.equals(((SdJwtClaimName) obj).claimName);
        }
        return false;
    }
}
