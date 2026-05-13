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

import java.security.cert.X509Certificate;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;

public class OID4VPIdentityProviderConfig extends IdentityProviderModel {

    public static final String REQUEST_OBJECT_LIFESPAN = "requestObjectLifespan";
    public static final String WALLET_SCHEME = "walletScheme";
    public static final String AUTHORIZATION_REQUEST_TRANSPORT = "authorizationRequestTransport";
    public static final String CLIENT_IDENTIFIER_PREFIX = "clientIdentifierPrefix";
    public static final String X509_CERTIFICATE_PEM = "x509CertificatePem";
    public static final String X509_PRIVATE_KEY_PEM = "x509PrivateKeyPem";
    public static final String SUBJECT_CLAIM_NAME = "subjectClaimName";
    public static final String TRUSTED_ISSUER_CERTIFICATE = "trustedIssuerCertificate";
    public static final String DCQL_QUERY = "dcqlQuery";
    public static final int DEFAULT_REQUEST_OBJECT_LIFESPAN = 300;
    public static final String DEFAULT_SUBJECT_CLAIM_NAME = "sub";

    public OID4VPIdentityProviderConfig() {
    }

    public OID4VPIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public int getRequestObjectLifespan() {
        String configured = getConfig().get(REQUEST_OBJECT_LIFESPAN);
        if (configured == null) {
            return DEFAULT_REQUEST_OBJECT_LIFESPAN;
        }

        try {
            int lifespan = Integer.parseInt(configured);
            return lifespan > 0 ? lifespan : DEFAULT_REQUEST_OBJECT_LIFESPAN;
        } catch (NumberFormatException nfe) {
            return DEFAULT_REQUEST_OBJECT_LIFESPAN;
        }
    }

    public String getWalletScheme() {
        String configured = getConfig().get(WALLET_SCHEME);
        return configured == null || configured.isBlank() ? OID4VPConstants.DEFAULT_WALLET_SCHEME : configured;
    }

    public AuthorizationRequestTransport getAuthorizationRequestTransport() {
        return AuthorizationRequestTransport.fromConfig(this);
    }

    public ClientIdentifierPrefix getClientIdentifierPrefix() {
        return ClientIdentifierPrefix.fromConfig(this);
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
        validateAuthorizationRequestSettings();
    }

    private void validateAuthorizationRequestSettings() {
        AuthorizationRequestTransport transport = getAuthorizationRequestTransport();
        ClientIdentifierPrefix prefix = getClientIdentifierPrefix();
        if (prefix == ClientIdentifierPrefix.REDIRECT_URI && transport != AuthorizationRequestTransport.QUERY_PARAMETERS) {
            throw new IllegalArgumentException("The redirect_uri Client Identifier Prefix cannot be used with signed request objects");
        }
        if ((prefix == ClientIdentifierPrefix.X509_SAN_DNS || prefix == ClientIdentifierPrefix.X509_HASH)
                && transport != AuthorizationRequestTransport.REQUEST_URI) {
            throw new IllegalArgumentException("Certificate-bound OID4VP Client Identifier Prefixes require a signed request object");
        }
        if (prefix == ClientIdentifierPrefix.X509_SAN_DNS) {
            X509Certificate certificate = RequestObjectSigner.parseCertificate(getX509CertificatePem());
            ClientIdentifier.resolveX509SanDnsName(certificate);
        }
    }

    public String getX509CertificatePem() {
        return getConfig().get(X509_CERTIFICATE_PEM);
    }

    public void setX509CertificatePem(String pem) {
        getConfig().put(X509_CERTIFICATE_PEM, pem);
    }

    public String getX509PrivateKeyPem() {
        return getConfig().get(X509_PRIVATE_KEY_PEM);
    }

    public void setX509PrivateKeyPem(String pem) {
        getConfig().put(X509_PRIVATE_KEY_PEM, pem);
    }

    public String getSubjectClaimName() {
        String configured = getConfig().get(SUBJECT_CLAIM_NAME);
        return configured == null || configured.isBlank() ? DEFAULT_SUBJECT_CLAIM_NAME : configured;
    }

    public String getTrustedIssuerCertificate() {
        String configured = getConfig().get(TRUSTED_ISSUER_CERTIFICATE);
        return configured == null || configured.isBlank() ? null : configured;
    }

    public String getDcqlQuery() {
        String configured = getConfig().get(DCQL_QUERY);
        return configured == null || configured.isBlank() ? null : configured;
    }
}
