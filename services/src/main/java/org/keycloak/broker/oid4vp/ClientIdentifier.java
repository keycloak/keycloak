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

package org.keycloak.broker.oid4vp;

import java.security.cert.X509Certificate;

import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.jose.jws.crypto.HashUtils;

/**
 * The OID4VP verifier {@code client_id}: a prefix that tells the wallet how to establish trust in the
 * request, followed by a value derived from the verifier signing certificate.
 *
 * <p>TODO add the remaining certificate based prefixes such as {@code x509_san_dns}.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-5.10">OID4VP 1.0 §5.10 — Client Identifier Prefixes</a>
 */
public enum ClientIdentifier {

    X509_HASH("x509_hash") {
        @Override
        protected String value(X509Certificate leafCertificate) {
            try {
                return Base64Url.encode(HashUtils.hash(JavaAlgorithm.SHA256, leafCertificate.getEncoded()));
            } catch (Exception e) {
                throw new IdentityBrokerException("Failed to compute x509_hash client id", e);
            }
        }
    };

    private final String prefix;

    ClientIdentifier(String prefix) {
        this.prefix = prefix;
    }

    // The prefix specific value, e.g. the certificate hash for x509_hash.
    protected abstract String value(X509Certificate leafCertificate);

    public String forCertificate(X509Certificate leafCertificate) {
        return prefix + ":" + value(leafCertificate);
    }
}
