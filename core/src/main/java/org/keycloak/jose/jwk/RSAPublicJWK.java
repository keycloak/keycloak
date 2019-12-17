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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.common.util.PemUtils;

import java.security.NoSuchAlgorithmException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RSAPublicJWK extends JWK {

    public static final String RSA = "RSA";
    public static final String RS256 = "RS256";

    public static final String MODULUS = "n";
    public static final String PUBLIC_EXPONENT = "e";

    @JsonProperty(MODULUS)
    private String modulus;

    @JsonProperty("e")
    private String publicExponent;

    @JsonProperty("x5c")
    private String[] x509CertificateChain;

    private String sha1x509Thumbprint;

    private String sha256x509Thumbprint;

    public String getModulus() {
        return modulus;
    }

    public void setModulus(String modulus) {
        this.modulus = modulus;
    }

    public String getPublicExponent() {
        return publicExponent;
    }

    public void setPublicExponent(String publicExponent) {
        this.publicExponent = publicExponent;
    }
    
    public String[] getX509CertificateChain() {
        return x509CertificateChain;
    }

    public void setX509CertificateChain(String[] x509CertificateChain) {
        this.x509CertificateChain = x509CertificateChain;
        if (x509CertificateChain != null && x509CertificateChain.length > 0) {
            try {
                sha1x509Thumbprint = PemUtils.generateThumbprint(x509CertificateChain, "SHA-1");
                sha256x509Thumbprint = PemUtils.generateThumbprint(x509CertificateChain, "SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @JsonProperty("x5t")
    public String getSha1x509Thumbprint() {
        return sha1x509Thumbprint;
    }

    @JsonProperty("x5t#S256")
    public String getSha256x509Thumbprint() {
        return sha256x509Thumbprint;
    }

}
