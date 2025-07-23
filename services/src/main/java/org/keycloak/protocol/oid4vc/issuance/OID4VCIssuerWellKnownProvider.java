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
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.services.Urls;
import org.keycloak.urls.UrlType;
import org.keycloak.wellknown.WellKnownProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link  WellKnownProvider} implementation to provide the .well-known/openid-credential-issuer endpoint, offering
 * the Credential Issuer Metadata as defined by the OID4VCI protocol
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-11.2.2}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCIssuerWellKnownProvider implements WellKnownProvider {

    public static final String VC_KEY = "vc";

    private static final Logger LOGGER = Logger.getLogger(OID4VCIssuerWellKnownProvider.class);

    protected final KeycloakSession keycloakSession;

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
                .setBatchCredentialIssuance(getBatchCredentialIssuance(keycloakSession))
                .setSignedMetadata(getSignedMetadata(keycloakSession));
        return issuer;
    }

    private static String getDeferredCredentialEndpoint(KeycloakContext context) {
        return getIssuer(context) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/deferred_credential";
    }

    private CredentialIssuer.CredentialResponseEncryption getCredentialResponseEncryption(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        String algs = realm.getAttribute("credential_response_encryption.alg_values_supported");
        String encs = realm.getAttribute("credential_response_encryption.enc_values_supported");
        String required = realm.getAttribute("credential_response_encryption.encryption_required");
        if (algs != null && encs != null && required != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return new CredentialIssuer.CredentialResponseEncryption()
                        .setAlgValuesSupported(mapper.readValue(algs, new TypeReference<List<String>>() {
                        }))
                        .setEncValuesSupported(mapper.readValue(encs, new TypeReference<List<String>>() {
                        }))
                        .setEncryptionRequired(Boolean.parseBoolean(required));
            } catch (Exception e) {
                LOGGER.warnf(e, "Failed to parse credential_response_encryption fields from realm attributes.");
            }
        }
        return null;
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
     * Return the supported credentials from the current session.
     * It will take into account the configured {@link CredentialBuilder}'s and there supported format
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

        // Aggregating attributes. Having realm attributes take preference.
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
