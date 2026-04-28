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
package org.keycloak.mdoc;

import java.util.Arrays;
import java.util.List;

import org.keycloak.crypto.Algorithm;

import com.authlete.cose.constants.COSEAlgorithms;

/**
 * Mapping between Keycloak's JOSE algorithm names and the COSE algorithm identifiers used by ISO mdoc IssuerAuth.
 * OID4VCI 1.0 Appendix A.2.2 advertises mDoc credential signing algorithms as numeric COSE identifiers, while
 * Keycloak signing keys and proof validation use JOSE names.
 */
public enum MdocAlgorithm {
    RS256(Algorithm.RS256, COSEAlgorithms.RS256),
    RS384(Algorithm.RS384, COSEAlgorithms.RS384),
    RS512(Algorithm.RS512, COSEAlgorithms.RS512),
    PS256(Algorithm.PS256, COSEAlgorithms.PS256),
    PS384(Algorithm.PS384, COSEAlgorithms.PS384),
    PS512(Algorithm.PS512, COSEAlgorithms.PS512),
    ES256(Algorithm.ES256, COSEAlgorithms.ES256),
    ES384(Algorithm.ES384, COSEAlgorithms.ES384),
    ES512(Algorithm.ES512, COSEAlgorithms.ES512),
    EDDSA(Algorithm.EdDSA, COSEAlgorithms.EdDSA);

    private final String joseAlgorithm;
    private final int coseAlgorithmIdentifier;

    MdocAlgorithm(String joseAlgorithm, int coseAlgorithmIdentifier) {
        this.joseAlgorithm = joseAlgorithm;
        this.coseAlgorithmIdentifier = coseAlgorithmIdentifier;
    }

    public String getJoseAlgorithm() {
        return joseAlgorithm;
    }

    public int getCoseAlgorithmIdentifier() {
        return coseAlgorithmIdentifier;
    }

    public static List<String> getSupportedJoseAlgorithms() {
        return Arrays.stream(values())
                .map(MdocAlgorithm::getJoseAlgorithm)
                .toList();
    }

    public static MdocAlgorithm fromJoseAlgorithm(String algorithm) {
        return Arrays.stream(values())
                .filter(value -> value.getJoseAlgorithm().equals(algorithm))
                .findFirst()
                .orElseThrow(() -> new MdocException("Unsupported JOSE algorithm for mDoc: " + algorithm));
    }

}
