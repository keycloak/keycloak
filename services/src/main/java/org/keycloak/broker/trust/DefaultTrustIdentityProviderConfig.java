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

package org.keycloak.broker.trust;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.keycloak.broker.provider.X509TrustMaterial;
import org.keycloak.common.util.PemUtils;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.util.Strings;

import static org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantConfig.PUBLIC_KEY_SIGNATURE_VERIFIER;
import static org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantConfig.PUBLIC_KEY_SIGNATURE_VERIFIER_KEY_ID;
import static org.keycloak.broker.oidc.OIDCIdentityProviderConfig.JWKS_URL;
import static org.keycloak.broker.oidc.OIDCIdentityProviderConfig.USE_JWKS_URL;
import static org.keycloak.common.util.UriUtils.checkUrl;

public class DefaultTrustIdentityProviderConfig extends IdentityProviderModel {

    public static final String TRUSTED_JWKS_URL = JWKS_URL;
    public static final String TRUSTED_JWKS = PUBLIC_KEY_SIGNATURE_VERIFIER;
    public static final String TRUSTED_JWKS_KEY_ID = PUBLIC_KEY_SIGNATURE_VERIFIER_KEY_ID;
    public static final String USE_X509 = "useX509";
    public static final String TRUSTED_CERTIFICATES = "trustedCertificates";
    public static final String ATTESTATION_EXTENDED_KEY_USAGES = "attestationExtendedKeyUsages";
    public static final String CERTIFICATE_REVOCATION_ENABLED = "certificateRevocationEnabled";

    public DefaultTrustIdentityProviderConfig() {
    }

    public DefaultTrustIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    @Override
    public Boolean isHideOnLogin() {
        return true;
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
        boolean hasTrustedJwksUrl = !Strings.isEmpty(getTrustedJwksUrl());
        boolean hasTrustedJwks = !Strings.isEmpty(getTrustedJwks());
        boolean hasTrustedCertificates = !Strings.isEmpty(getTrustedCertificates());
        if (!isUseX509() && hasTrustedCertificates) {
            throw new IllegalArgumentException(
                    "'Use X.509 attestation trust' must be enabled when trusted X.509 certificates are configured");
        }
        if (!isUseX509() && isUseJwksUrl()) {
            if (!hasTrustedJwksUrl) {
                throw new IllegalArgumentException("JWKS URL is required when 'Use JWKS URL' is enabled");
            }
        } else if (!isUseX509() && !hasTrustedJwks) {
            throw new IllegalArgumentException("Validating public key is required when 'Use JWKS URL' is disabled");
        }
        if (!isUseX509() && hasTrustedJwksUrl && isUseJwksUrl()) {
            checkUrl(realm.getSslRequired(), getTrustedJwksUrl(), TRUSTED_JWKS_URL);
        }
        if (isUseX509()) {
            if (!hasTrustedCertificates) {
                throw new IllegalArgumentException("Trusted X.509 certificates are required when X.509 trust is enabled");
            }
            if (getAttestationExtendedKeyUsages().isEmpty()) {
                throw new IllegalArgumentException("At least one attestation extended key usage OID is required for X.509 trust");
            }
            try {
                X509Certificate[] certificates = PemUtils.decodeCertificates(getTrustedCertificates());
                if (certificates.length == 0) {
                    throw new IllegalArgumentException("At least one trusted X.509 certificate is required");
                }
                new X509TrustMaterial(new LinkedHashSet<>(Arrays.asList(certificates)),
                        getAttestationExtendedKeyUsages(), isCertificateRevocationEnabled());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "Trusted X.509 certificates must be a valid PEM bundle of self-signed CA roots", e);
            }
        }
    }

    public boolean isUseJwksUrl() {
        String useJwksUrl = getConfig().get(USE_JWKS_URL);
        return useJwksUrl == null ? !Strings.isEmpty(getTrustedJwksUrl())
                : Boolean.parseBoolean(useJwksUrl);
    }

    public String getTrustedJwksUrl() {
        return getConfig().get(TRUSTED_JWKS_URL);
    }

    public void setTrustedJwksUrl(String trustedJwksUrl) {
        if (trustedJwksUrl == null) {
            getConfig().remove(TRUSTED_JWKS_URL);
        } else {
            getConfig().put(TRUSTED_JWKS_URL, trustedJwksUrl);
        }
    }

    public String getTrustedJwks() {
        return getConfig().get(TRUSTED_JWKS);
    }

    public void setTrustedJwks(String trustedJwks) {
        if (trustedJwks == null) {
            getConfig().remove(TRUSTED_JWKS);
        } else {
            getConfig().put(TRUSTED_JWKS, trustedJwks);
        }
    }

    public String getTrustedJwksKeyId() {
        return getConfig().get(TRUSTED_JWKS_KEY_ID);
    }

    public String getTrustedCertificates() {
        return getConfig().get(TRUSTED_CERTIFICATES);
    }

    public boolean isUseX509() {
        return Boolean.parseBoolean(getConfig().getOrDefault(USE_X509, Boolean.FALSE.toString()));
    }

    public void setTrustedCertificates(String trustedCertificates) {
        if (trustedCertificates == null) {
            getConfig().remove(TRUSTED_CERTIFICATES);
        } else {
            getConfig().put(TRUSTED_CERTIFICATES, trustedCertificates);
        }
    }

    public List<String> getAttestationExtendedKeyUsages() {
        String configured = getConfig().get(ATTESTATION_EXTENDED_KEY_USAGES);
        if (Strings.isEmpty(configured)) {
            return List.of();
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    public boolean isCertificateRevocationEnabled() {
        String configured = getConfig().get(CERTIFICATE_REVOCATION_ENABLED);
        return configured == null || !Boolean.FALSE.toString().equalsIgnoreCase(configured.trim());
    }
}
