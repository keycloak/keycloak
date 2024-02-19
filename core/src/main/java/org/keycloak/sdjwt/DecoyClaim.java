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
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class DecoyClaim extends DecoyEntry {

    private DecoyClaim(SdJwtSalt salt) {
        super(salt);
    }

    public static class Builder {
        private SdJwtSalt salt;

        public Builder withSalt(SdJwtSalt salt) {
            this.salt = salt;
            return this;
        }

        public DecoyClaim build() {
            salt = salt == null ? new SdJwtSalt(SdJwtUtils.randomSalt()) : salt;
            return new DecoyClaim(salt);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
