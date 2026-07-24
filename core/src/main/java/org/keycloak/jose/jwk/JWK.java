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

package org.keycloak.jose.jwk;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.common.util.PemUtils;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JWK {

    public static final String KEY_ID = "kid";

    public static final String KEY_TYPE = "kty";

    public static final String ALGORITHM = "alg";

    public static final String PUBLIC_KEY_USE = "use";

    public static final String X5C = "x5c";

    public static final String SHA1_509_THUMBPRINT = "x5t";

    public static final String SHA256_509_THUMBPRINT = "x5t#S256";

    /**
     * This duplicates {@link org.keycloak.crypto.KeyUse}, which should be used instead when possible
     */
    @Deprecated
    public enum Use {
        SIG("sig"),
        ENCRYPTION("enc"),
        JWT_SVID("jwt-svid");

        private String str;

        Use(String str) {
            this.str = str;
        }

        public String asString() {
            return str;
        }
    }

    @JsonProperty(KEY_ID)
    private String keyId;

    @JsonProperty(KEY_TYPE)
    private String keyType;

    @JsonProperty(ALGORITHM)
    private String algorithm;

    @JsonProperty(PUBLIC_KEY_USE)
    private String publicKeyUse;

    @JsonProperty(X5C)
    private String[] x509CertificateChain;

    @JsonProperty(SHA1_509_THUMBPRINT)
    private String sha1x509Thumbprint;

    @JsonProperty(SHA256_509_THUMBPRINT)
    private String sha256x509Thumbprint;

    protected Map<String, Object> otherClaims = new HashMap<String, Object>();


    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getPublicKeyUse() {
        return publicKeyUse;
    }

    public void setPublicKeyUse(String publicKeyUse) {
        this.publicKeyUse = publicKeyUse;
    }

    public String[] getX509CertificateChain() {
        return x509CertificateChain;
    }

    public void setX509CertificateChain(String[] x509CertificateChain) {
        this.x509CertificateChain = x509CertificateChain;
    }

    public String getSha1x509Thumbprint() {
        if (sha1x509Thumbprint == null && x509CertificateChain != null && x509CertificateChain.length > 0) {
            try {
                sha1x509Thumbprint = PemUtils.generateThumbprint(x509CertificateChain, "SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return sha1x509Thumbprint;
    }

    public void setSha1x509Thumbprint(String sha1x509Thumbprint) {
        this.sha1x509Thumbprint = sha1x509Thumbprint;
    }

    public String getSha256x509Thumbprint() {
        if (sha256x509Thumbprint == null && x509CertificateChain != null && x509CertificateChain.length > 0) {
            try {
                sha256x509Thumbprint = PemUtils.generateThumbprint(x509CertificateChain, "SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return sha256x509Thumbprint;
    }

    public void setSha256x509Thumbprint(String sha256x509Thumbprint) {
        this.sha256x509Thumbprint = sha256x509Thumbprint;
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(String name, Object value) {
        otherClaims.put(name, value);
    }

    /**
     * Ability to retrieve custom claims in a unified way. The subclasses (like for example OKPublicJWK) may contain the custom claims
     * as Java properties when the "JWK" class can contain the same claims inside the "otherClaims" map. This method allows to obtain the
     * claim in both ways regardless of if we have "JWK" class or some of it's subclass
     *
     * @param claimName claim name
     * @param claimType claim type
     * @return claim if present or null
     */
    @JsonIgnore
    public <T> T getOtherClaim(String claimName, Class<T> claimType) {
        Object o = getOtherClaims().get(claimName);
        return o == null ? null : claimType.cast(o);
    }

}
