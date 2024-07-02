/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * A class representing the trusted issuer configuration in the assertion grant client configuration.
 */
public class CrossDomainTrust {
    // serializable
    private String issuer;
    private String audience;

    private String certificate;

    // computed
    private X509Certificate decodedCertificate;
    private PublicKey publicKey;

    // general config
    public static String REALM_CROSS_DOMAIN_TRUST_ATTRIBUTE = "crossDomainTrust";

    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getIssuer() { return this.issuer; }

    public void setAudience(String audience) { this.audience = audience; }
    public String getAudience() { return audience; }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public void setDecodedCertificate(X509Certificate decodedCertificate) { this.decodedCertificate = decodedCertificate; }
    public X509Certificate getDecodedCertificate() { return decodedCertificate; }

    public void setPublicKey(PublicKey publicKey) { this.publicKey = publicKey; }
    public PublicKey getPublicKey() { return publicKey; }
}
