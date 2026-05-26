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

package org.keycloak.broker.provider;

public class TrustMaterialRequest {

    private final String kid;
    private final String algorithm;
    private final String issuer;

    private TrustMaterialRequest(Builder builder) {
        this.kid = builder.kid;
        this.algorithm = builder.algorithm;
        this.issuer = builder.issuer;
    }

    public String getKid() {
        return kid;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getIssuer() {
        return issuer;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String kid;
        private String algorithm;
        private String issuer;

        public Builder kid(String kid) {
            this.kid = kid;
            return this;
        }

        public Builder algorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public TrustMaterialRequest build() {
            return new TrustMaterialRequest(this);
        }
    }
}
