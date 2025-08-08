/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance;

import jakarta.ws.rs.core.UriInfo;
import org.keycloak.constants.Oid4VciConstants;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;

import org.keycloak.protocol.oid4vc.model.CredentialRequestEncryptionMetadata;
import org.keycloak.protocol.oid4vc.model.CredentialResponseEncryptionMetadata;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.services.Urls;
import org.keycloak.urls.UrlType;
import org.keycloak.wellknown.WellKnownProvider;
import org.jboss.logging.Logger;

import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.keycloak.crypto.KeyType.RSA;
import static org.keycloak.jose.jwk.ECPublicJWK.EC;

/**
 * {@link WellKnownProvider} implementation to provide the .well-known/openid-credential-issuer endpoint, offering
 * the Credential Issuer Metadata as defined by the OID4VCI protocol
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-11.2.2}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCIssuerWellKnownProvider implements WellKnownProvider {

    public static final String VC_KEY = "vc";

    private static final Logger LOGGER = Logger.getLogger(OID4VCIssuerWellKnownProvider.class);

    protected final KeycloakSession keycloakSession;

    public static final String ATTR_ENCRYPTION_REQUIRED = "oid4vci.encryption.required";
    public static final String ATTR_REQUEST_ENCRYPTION_REQUIRED = "oid4vci.request.encryption.required";

    // Constants for compression algorithms
    public static final String DEFLATE_COMPRESSION = "DEF";
    private static final List<String> DEFAULT_COMPRESSION_ALGORITHMS = List.of(DEFLATE_COMPRESSION);

    public OID4VCIssuerWellKnownProvider(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public Object getConfig() {
        KeycloakContext context = keycloakSession.getContext();
        CredentialIssuer issuer = new CredentialIssuer()
                .setCredentialIssuer(getIssuer(context))
                .setCredentialEndpoint(getCredentialsEndpoint(context))
                .setNonceEndpoint(getNonceEndpoint(context))
                .setDeferredCredentialEndpoint(getDeferredCredentialEndpoint(context))
                .setCredentialsSupported(getSupportedCredentials(keycloakSession))
                .setAuthorizationServers(List.of(getIssuer(context)))
                .setCredentialResponseEncryption(getCredentialResponseEncryption(keycloakSession))
//                .setCredentialRequestEncryption(getCredentialRequestEncryption(keycloakSession))
                .setBatchCredentialIssuance(getBatchCredentialIssuance(keycloakSession))
                .setSignedMetadata(getSignedMetadata(keycloakSession));
        return issuer;
    }

    private static String getDeferredCredentialEndpoint(KeycloakContext context) {
        return getIssuer(context) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/deferred_credential";
    }

    private CredentialIssuer.BatchCredentialIssuance getBatchCredentialIssuance(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        String batchSize = realm.getAttribute("batch_credential_issuance.batch_size");
        if (batchSize != null) {
            try {
                return new CredentialIssuer.BatchCredentialIssuance()
                        .setBatchSize(Integer.parseInt(batchSize));
            } catch (Exception e) {
                LOGGER.warnf(e, "Failed to parse batch_credential_issuance.batch_size from realm attributes.");
            }
        }
        return null;
    }

    private String getSignedMetadata(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        return realm.getAttribute("signed_metadata");
    }

    /**
     * Returns the credential response encryption высоко for the issuer.
     * Now determines supported algorithms from available realm keys.
     *
     * @param session The Keycloak session
     * @return The credential response encryption metadata
     */
    public static CredentialResponseEncryptionMetadata getCredentialResponseEncryption(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        CredentialResponseEncryptionMetadata metadata = new CredentialResponseEncryptionMetadata();

        // Get supported algorithms from available encryption keys
        metadata.setAlgValuesSupported(getSupportedEncryptionAlgorithms(session))
                .setEncValuesSupported(getSupportedEncryptionMethods())
                .setZipValuesSupported(getSupportedCompressionMethods())
                .setEncryptionRequired(isEncryptionRequired(realm));

        return metadata;
    }

    /**
     * Returns the credential request encryption metadata for the issuer.
     * Determines supported algorithms and JWK Set from available realm keys
     */
//    public static CredentialRequestEncryptionMetadata getCredentialRequestEncryption(KeycloakSession session) {
//        RealmModel realm = session.getContext().getRealm();
//        CredentialRequestEncryptionMetadata metadata = new CredentialRequestEncryptionMetadata();
//
//        metadata.setJwks(getJWKSet(session))
//                .setEncValuesSupported(getSupportedEncryptionMethods())
//                .setZipValuesSupported(getSupportedCompressionMethods())
//                .setEncryptionRequired(isRequestEncryptionRequired(realm));
//
//        return metadata;
//    }


    /**
     * Returns the supported encryption algorithms from realm attributes.
     */
    public static List<String> getSupportedEncryptionAlgorithms(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        KeyManager keyManager = session.keys();

        List<String> supportedEncryptionAlgorithms = keyManager.getKeysStream(realm)
                .filter(key -> KeyUse.ENC.equals(key.getUse()))
                .map(KeyWrapper::getAlgorithm)
                .filter(algorithm -> algorithm != null && !algorithm.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // Default algorithms if none configured
        if (supportedEncryptionAlgorithms.isEmpty()) {
            boolean hasRsaKeys = keyManager.getKeysStream(realm)
                    .filter(key -> KeyUse.ENC.equals(key.getUse()))
                    .anyMatch(key -> RSA.equals(key.getType()));

            if (hasRsaKeys) {
                supportedEncryptionAlgorithms.add(JWEConstants.RSA_OAEP);
                supportedEncryptionAlgorithms.add(JWEConstants.RSA_OAEP_256);
            }
        }

        return supportedEncryptionAlgorithms;
    }

    /**
     * Returns the JWK Set for credential request encryption
     */
//    private static JSONWebKeySet getJWKSet(KeycloakSession session) {
//        RealmModel realm = session.getContext().getRealm();
//        List<JWK> jwks = session.keys().getKeysStream(realm)
//                .filter(key -> KeyUse.ENC.equals(key.getUse()))
//                .map(key -> {
//                    try {
//                        String kid = key.getKid() != null ? key.getKid() : realm.getId() + "-" + key.getProviderId();
//                        String algorithm = key.getAlgorithm() != null ? key.getAlgorithm() : JWEConstants.RSA_OAEP;
//                        if ("RSA".equals(key.getType())) {
//                            return JWKBuilder.create()
//                                    .rsa
//                                    )
//                        }
//                        // Skip non-RSA keys for now (EC not supported in this context)
//                        return null;
//                    } catch (Exception e) {
//                        LOGGER.warnf(e, "Failed to convert key to JWK");
//                        return null;
//                    }
//                })
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//        return jwks.isEmpty() ? null : new JSONWebKeySet(jwks);
//    }

    /**
     * Returns the supported compression methods.
     */
    private static List<String> getSupportedCompressionMethods() {
        // Currently only DEFLATE is widely supported
        return DEFAULT_COMPRESSION_ALGORITHMS;
    }


    /**
     * Returns the supported encryption methods from realm attributes.
     */
    private static List<String> getSupportedEncryptionMethods() {
        return List.of(JWEConstants.A256GCM);
    }

    /**
     * Returns whether encryption is required from realm attributes.
     */
    private static boolean isEncryptionRequired(RealmModel realm) {
        String required = realm.getAttribute(ATTR_ENCRYPTION_REQUIRED);
        return Boolean.parseBoolean(required);
    }

    /**
     * Returns whether request encryption is required from realm attributes.
     */
    private static boolean isRequestEncryptionRequired(RealmModel realm) {
        String required = realm.getAttribute(ATTR_REQUEST_ENCRYPTION_REQUIRED);
        return Boolean.parseBoolean(required);
    }


    /**
     * Return the supported credentials from the current session.
     * It will take into account the configured {@link CredentialBuilder}'s and their supported format
     * and the credentials supported by the clients available in the session.
     */
    public static Map<String, SupportedCredentialConfiguration> getSupportedCredentials(KeycloakSession keycloakSession) {
        List<String> globalSupportedSigningAlgorithms = getSupportedSignatureAlgorithms(keycloakSession);

        RealmModel realm = keycloakSession.getContext().getRealm();
        Map<String, SupportedCredentialConfiguration> supportedCredentialConfigurations =
                keycloakSession.clientScopes()
                        .getClientScopesByProtocol(realm, Oid4VciConstants.OID4VC_PROTOCOL)
                        .map(CredentialScopeModel::new)
                        .map(clientScope -> {
                            return SupportedCredentialConfiguration.parse(keycloakSession,
                                    clientScope,
                                    globalSupportedSigningAlgorithms
                            );
                        })
                        .collect(Collectors.toMap(SupportedCredentialConfiguration::getId, sc -> sc, (sc1, sc2) -> sc1));

        return supportedCredentialConfigurations;
    }

    public static SupportedCredentialConfiguration toSupportedCredentialConfiguration(KeycloakSession keycloakSession,
                                                                                      CredentialScopeModel credentialModel) {
        List<String> globalSupportedSigningAlgorithms = getSupportedSignatureAlgorithms(keycloakSession);
        return SupportedCredentialConfiguration.parse(keycloakSession,
                credentialModel,
                globalSupportedSigningAlgorithms);
    }

    /**
     * Return the url of the issuer.
     */
    public static String getIssuer(KeycloakContext context) {
        UriInfo frontendUriInfo = context.getUri(UrlType.FRONTEND);
        return Urls.realmIssuer(frontendUriInfo.getBaseUri(),
                context.getRealm().getName());
    }

    /**
     * Return the nonce endpoint address
     */
    public static String getNonceEndpoint(KeycloakContext context) {
        return getIssuer(context) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/" +
                OID4VCIssuerEndpoint.NONCE_PATH;
    }

    /**
     * Return the credentials endpoint address
     */
    public static String getCredentialsEndpoint(KeycloakContext context) {
        return getIssuer(context) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/" + OID4VCIssuerEndpoint.CREDENTIAL_PATH;
    }

    public static List<String> getSupportedSignatureAlgorithms(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        KeyManager keyManager = session.keys();

        return keyManager.getKeysStream(realm)
                .filter(key -> KeyUse.SIG.equals(key.getUse()))
                .map(KeyWrapper::getAlgorithm)
                .filter(algorithm -> algorithm != null && !algorithm.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

}
