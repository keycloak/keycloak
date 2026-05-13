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
package org.keycloak.protocol.oid4vc.presentation;

import java.security.MessageDigest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import org.keycloak.common.util.Base64Url;

// Models the OpenID4VP 1.0 Section 5.9.1 syntax: <client_id_prefix>:<orig_client_id>.
final class ClientIdentifier {

    private static final String SEPARATOR = ":";

    private final ClientIdentifierPrefix prefix;
    private final String identifier;

    private ClientIdentifier(ClientIdentifierPrefix prefix, String identifier) {
        if (prefix == null) {
            throw new IllegalArgumentException("OID4VP Client Identifier Prefix is required");
        }
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("OID4VP client identifier is required");
        }

        this.prefix = prefix;
        this.identifier = identifier;
    }

    public static ClientIdentifier redirectUri(String responseUri) {
        return new ClientIdentifier(ClientIdentifierPrefix.REDIRECT_URI, responseUri);
    }

    public static ClientIdentifier x509SanDns(String dnsName) {
        return new ClientIdentifier(ClientIdentifierPrefix.X509_SAN_DNS, dnsName);
    }

    public static ClientIdentifier x509Hash(X509Certificate certificate) {
        if (certificate == null) {
            throw new IllegalArgumentException("OID4VP x509_hash client id requires a signing certificate");
        }

        return new ClientIdentifier(ClientIdentifierPrefix.X509_HASH, sha256Thumbprint(certificate));
    }

    static ClientIdentifier resolve(ClientIdentifierPrefix prefix,
                                    String responseUri,
                                    X509Certificate signingCertificate) {
        return switch (prefix) {
            case REDIRECT_URI -> redirectUri(responseUri);
            case X509_SAN_DNS -> x509SanDns(resolveX509SanDnsName(signingCertificate));
            case X509_HASH -> x509Hash(signingCertificate);
        };
    }

    public ClientIdentifierPrefix getPrefix() {
        return prefix;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getValue() {
        return prefix.getValue() + SEPARATOR + identifier;
    }

    private static String sha256Thumbprint(X509Certificate certificate) {
        try {
            return Base64Url.encode(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException("Unable to encode OID4VP signing certificate", e);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to calculate OID4VP signing certificate thumbprint", e);
        }
    }

    static String resolveX509SanDnsName(X509Certificate signingCertificate) {
        if (signingCertificate == null) {
            throw new IllegalArgumentException("OID4VP x509_san_dns client id requires a signing certificate");
        }
        String certificateDnsName = extractDnsSubjectAlternativeName(signingCertificate);
        if (certificateDnsName != null) {
            return certificateDnsName;
        }

        throw new IllegalArgumentException("OID4VP x509_san_dns client id requires a DNS subject alternative name");
    }

    private static String extractDnsSubjectAlternativeName(X509Certificate certificate) {
        try {
            Collection<List<?>> subjectAlternativeNames = certificate.getSubjectAlternativeNames();
            if (subjectAlternativeNames == null) {
                return null;
            }
            for (List<?> subjectAlternativeName : subjectAlternativeNames) {
                // X.509 Subject Alternative Name GeneralName type 2 is dNSName (RFC 5280 Section 4.2.1.6).
                if (subjectAlternativeName.size() >= 2 && Integer.valueOf(2).equals(subjectAlternativeName.get(0))) {
                    return subjectAlternativeName.get(1).toString();
                }
            }
            return null;
        } catch (CertificateParsingException e) {
            throw new IllegalArgumentException("Unable to read OID4VP signing certificate subject alternative names", e);
        }
    }
}
