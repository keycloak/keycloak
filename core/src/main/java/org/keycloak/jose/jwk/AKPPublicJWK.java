/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose.jwk;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.keycloak.crypto.KeyType;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AKPPublicJWK extends JWK {

    public static final String AKP = KeyType.AKP;

    public static final String PUB = "pub";

    @JsonProperty(PUB)
    private String pub;

    public String getPub() {
        return pub;
    }

    public void setPub(String pub) {
        this.pub = pub;
    }

    @JsonIgnore
    @Override
    public <T> T getOtherClaim(String claimName, Class<T> claimType) {
        Object claim = null;
        switch (claimName) {
            case ALGORITHM:
                claim = getAlgorithm();
                break;
            case PUB:
                claim = getPub();
                break;
        }
        if (claim != null) {
            return claimType.cast(claim);
        } else {
            return super.getOtherClaim(claimName, claimType);
        }
    }
}
