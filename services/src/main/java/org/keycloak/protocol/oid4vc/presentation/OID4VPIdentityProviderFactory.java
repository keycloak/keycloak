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

import java.util.List;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class OID4VPIdentityProviderFactory extends AbstractIdentityProviderFactory<OID4VPIdentityProvider> implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "oid4vp";

    @Override
    public String getName() {
        return "OpenID for Verifiable Presentations";
    }

    @Override
    public OID4VPIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new OID4VPIdentityProvider(session, new OID4VPIdentityProviderConfig(model));
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty walletScheme = new ProviderConfigProperty();
        walletScheme.setName(OID4VPIdentityProviderConfig.WALLET_SCHEME);
        walletScheme.setLabel("Wallet URL Scheme");
        walletScheme.setHelpText("Custom wallet URL scheme prefix (for example, openid4vp:// or haip-vp://).");
        walletScheme.setType(ProviderConfigProperty.STRING_TYPE);
        walletScheme.setDefaultValue(OID4VPConstants.DEFAULT_WALLET_SCHEME);

        ProviderConfigProperty authorizationRequestTransport = new ProviderConfigProperty();
        authorizationRequestTransport.setName(OID4VPIdentityProviderConfig.AUTHORIZATION_REQUEST_TRANSPORT);
        authorizationRequestTransport.setLabel("Authorization Request Transport");
        authorizationRequestTransport.setHelpText("How the OID4VP authorization request is delivered to the wallet.");
        authorizationRequestTransport.setType(ProviderConfigProperty.LIST_TYPE);
        authorizationRequestTransport.setOptions(List.of(
                AuthorizationRequestTransport.QUERY_PARAMETERS.getValue(),
                AuthorizationRequestTransport.REQUEST_URI.getValue()));
        authorizationRequestTransport.setDefaultValue(AuthorizationRequestTransport.QUERY_PARAMETERS.getValue());

        ProviderConfigProperty clientIdentifierPrefix = new ProviderConfigProperty();
        clientIdentifierPrefix.setName(OID4VPIdentityProviderConfig.CLIENT_IDENTIFIER_PREFIX);
        clientIdentifierPrefix.setLabel("Client Identifier Prefix");
        clientIdentifierPrefix.setHelpText("OID4VP verifier Client Identifier Prefix.");
        clientIdentifierPrefix.setType(ProviderConfigProperty.LIST_TYPE);
        clientIdentifierPrefix.setOptions(List.of(
                ClientIdentifierPrefix.REDIRECT_URI.getValue(),
                ClientIdentifierPrefix.X509_SAN_DNS.getValue(),
                ClientIdentifierPrefix.X509_HASH.getValue()));
        clientIdentifierPrefix.setDefaultValue(ClientIdentifierPrefix.REDIRECT_URI.getValue());

        ProviderConfigProperty x509CertificatePem = new ProviderConfigProperty();
        x509CertificatePem.setName(OID4VPIdentityProviderConfig.X509_CERTIFICATE_PEM);
        x509CertificatePem.setLabel("X.509 Certificate (PEM)");
        x509CertificatePem.setHelpText("PEM encoded verifier certificate used for certificate-bound client IDs and request object x5c.");
        x509CertificatePem.setType(ProviderConfigProperty.TEXT_TYPE);

        ProviderConfigProperty x509PrivateKeyPem = new ProviderConfigProperty();
        x509PrivateKeyPem.setName(OID4VPIdentityProviderConfig.X509_PRIVATE_KEY_PEM);
        x509PrivateKeyPem.setLabel("X.509 Private Key (PEM)");
        x509PrivateKeyPem.setHelpText("PEM encoded verifier private key used for OID4VP request object signing.");
        x509PrivateKeyPem.setType(ProviderConfigProperty.TEXT_TYPE);

        ProviderConfigProperty requestObjectLifespan = new ProviderConfigProperty();
        requestObjectLifespan.setName(OID4VPIdentityProviderConfig.REQUEST_OBJECT_LIFESPAN);
        requestObjectLifespan.setLabel("Request Object Lifespan");
        requestObjectLifespan.setHelpText("Lifetime of generated OID4VP request objects in seconds.");
        requestObjectLifespan.setType(ProviderConfigProperty.STRING_TYPE);
        requestObjectLifespan.setDefaultValue(Integer.toString(OID4VPIdentityProviderConfig.DEFAULT_REQUEST_OBJECT_LIFESPAN));

        ProviderConfigProperty subjectClaimName = new ProviderConfigProperty();
        subjectClaimName.setName(OID4VPIdentityProviderConfig.SUBJECT_CLAIM_NAME);
        subjectClaimName.setLabel("Subject Claim Name");
        subjectClaimName.setHelpText("Name of the verified credential claim to use as the brokered identity subject.");
        subjectClaimName.setType(ProviderConfigProperty.STRING_TYPE);
        subjectClaimName.setDefaultValue(OID4VPIdentityProviderConfig.DEFAULT_SUBJECT_CLAIM_NAME);

        ProviderConfigProperty trustedIssuerCertificate = new ProviderConfigProperty();
        trustedIssuerCertificate.setName(OID4VPIdentityProviderConfig.TRUSTED_ISSUER_CERTIFICATE);
        trustedIssuerCertificate.setLabel("Trusted Issuer Certificate");
        trustedIssuerCertificate.setHelpText("PEM encoded certificate used to trust SD-JWT VC issuer certificate chains.");
        trustedIssuerCertificate.setType(ProviderConfigProperty.TEXT_TYPE);

        ProviderConfigProperty dcqlQuery = new ProviderConfigProperty();
        dcqlQuery.setName(OID4VPIdentityProviderConfig.DCQL_QUERY);
        dcqlQuery.setLabel("DCQL Query");
        dcqlQuery.setHelpText("DCQL query JSON used in generated OID4VP authorization requests.");
        dcqlQuery.setType(ProviderConfigProperty.TEXT_TYPE);

        return List.of(
                walletScheme,
                authorizationRequestTransport,
                clientIdentifierPrefix,
                x509CertificatePem,
                x509PrivateKeyPem,
                requestObjectLifespan,
                subjectClaimName,
                trustedIssuerCertificate,
                dcqlQuery);
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new OID4VPIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VP);
    }
}
