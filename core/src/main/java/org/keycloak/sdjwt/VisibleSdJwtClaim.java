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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class VisibleSdJwtClaim extends AbstractSdJwtClaim {
    private final JsonNode claimValue;

    public VisibleSdJwtClaim(SdJwtClaimName claimName, JsonNode claimValue) {
        super(claimName);
        this.claimValue = claimValue;
    }

    @Override
    public JsonNode getVisibleClaimValue(String hashAlgo) {
        return claimValue;
    }

    // Static method to create a builder instance
    public static Builder builder() {
        return new Builder();
    }

    // Static inner Builder class
    public static class Builder {
        private SdJwtClaimName claimName;
        private JsonNode claimValue;

        public Builder withClaimName(String claimName) {
            this.claimName = new SdJwtClaimName(claimName);
            return this;
        }

        public Builder withClaimValue(JsonNode claimValue) {
            this.claimValue = claimValue;
            return this;
        }

        public VisibleSdJwtClaim build() {
            claimName = Objects.requireNonNull(claimName, "claimName must not be null");
            claimValue = Objects.requireNonNull(claimValue, "claimValue must not be null");
            return new VisibleSdJwtClaim(claimName, claimValue);
        }
    }

    @Override
    public List<String> getDisclosureStrings() {
        return Collections.emptyList();
    }
}
