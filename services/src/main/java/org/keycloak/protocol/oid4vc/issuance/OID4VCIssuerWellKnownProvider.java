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
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.OID4VCClientRegistrationProvider;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilderFactory;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSigner;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSignerFactory;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.OID4VCClient;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.services.Urls;
import org.keycloak.urls.UrlType;
import org.keycloak.wellknown.WellKnownProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link  WellKnownProvider} implementation to provide the .well-known/openid-credential-issuer endpoint, offering
 * the Credential Issuer Metadata as defined by the OID4VCI protocol
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-11.2.2}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCIssuerWellKnownProvider implements WellKnownProvider {

    private final KeycloakSession keycloakSession;

    public OID4VCIssuerWellKnownProvider(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public Object getConfig() {
        return new CredentialIssuer()
                .setCredentialIssuer(getIssuer(keycloakSession.getContext()))
                .setCredentialEndpoint(getCredentialsEndpoint(keycloakSession.getContext()))
                .setCredentialsSupported(getSupportedCredentials(keycloakSession))
                .setAuthorizationServers(List.of(getIssuer(keycloakSession.getContext())));

    }

    /**
     * Return the supported credentials from the current session.
     * It will take into account the configured {@link CredentialBuilder}'s and there supported format
     * and the credentials supported by the clients available in the session.
     */
    public static Map<String, SupportedCredentialConfiguration> getSupportedCredentials(KeycloakSession keycloakSession) {

        RealmModel realm = keycloakSession.getContext().getRealm();
        List<String> supportedFormats = getSupportedFormats(keycloakSession);

        // Retrieve signature algorithms
        List<String> supportedAlgorithms = getSupportedSignatureAlgorithms(keycloakSession);

        // Retrieving attributes from client definition.
        // This will be removed when token production is migrated.
        Map<String, SupportedCredentialConfiguration> clientAttributes = keycloakSession.getContext()
                .getRealm()
                .getClientsStream()
                .filter(cm -> cm.getProtocol() != null)
                .filter(cm -> cm.getProtocol().equals(OID4VCLoginProtocolFactory.PROTOCOL_ID))
                .map(cm -> OID4VCClientRegistrationProvider.fromClientAttributes(cm.getClientId(), cm.getAttributes()))
                .map(OID4VCClient::getSupportedVCTypes)
                .flatMap(List::stream)
                .filter(sc -> supportedFormats.contains(sc.getFormat()))
                .distinct()
                .peek(sc -> sc.setCredentialSigningAlgValuesSupported(supportedAlgorithms))
                .collect(Collectors.toMap(SupportedCredentialConfiguration::getId, sc -> sc, (sc1, sc2) -> sc1));


        // Retrieving attributes from the realm
        Map<String, SupportedCredentialConfiguration> realmAttr = fromRealmAttributes(realm.getAttributes())
                .stream()
                .filter(sc -> supportedFormats.contains(sc.getFormat()))
                .distinct()
                .peek(sc -> sc.setCredentialSigningAlgValuesSupported(supportedAlgorithms))
                .collect(Collectors.toMap(SupportedCredentialConfiguration::getId, sc -> sc, (sc1, sc2) -> sc1));

        // Aggregating attributes. Having realm attributes take preference.
        Map<String, SupportedCredentialConfiguration> aggregatedAttr = new HashMap<>(clientAttributes);
        aggregatedAttr.putAll(realmAttr);
        return aggregatedAttr;
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
     * Return the credentials endpoint address
     */
    public static String getCredentialsEndpoint(KeycloakContext context) {
        return getIssuer(context) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/" + OID4VCIssuerEndpoint.CREDENTIAL_PATH;
    }

    public static final String VC_KEY = "vc";

    public static List<SupportedCredentialConfiguration> fromRealmAttributes(Map<String, String> realmAttributes) {

        Set<String> supportedCredentialIds = new HashSet<>();
        Map<String, String> attributes = new HashMap<>();
        realmAttributes.forEach((entryKey, value) -> {
            if (!entryKey.startsWith(VC_KEY)) {
                return;
            }
            String key = entryKey.substring((VC_KEY + ".").length());
            supportedCredentialIds.add(key.split("\\.")[0]);
            attributes.put(key, value);
        });

        return supportedCredentialIds
                .stream()
                .map(id -> SupportedCredentialConfiguration.fromDotNotation(id, attributes))
                .toList();
    }

    /**
     * Returns credential formats supported.
     * <p></p>
     * Supported credential formats are identified on the criterion of a joint availability
     * of a credential builder (as a configured component) AND a credential signer.
     */
    public static List<String> getSupportedFormats(KeycloakSession keycloakSession) {
        RealmModel realm = keycloakSession.getContext().getRealm();
        KeycloakSessionFactory keycloakSessionFactory = keycloakSession.getKeycloakSessionFactory();

        List<String> supportedFormatsByBuilders = realm
                .getComponentsStream(realm.getId(), CredentialBuilder.class.getName())
                .map(cm -> keycloakSessionFactory.getProviderFactory(CredentialBuilder.class, cm.getProviderId()))
                .filter(CredentialBuilderFactory.class::isInstance)
                .map(CredentialBuilderFactory.class::cast)
                .map(CredentialBuilderFactory::getSupportedFormat)
                .toList();

        List<String> supportedFormatsBySigners = keycloakSession
                .getKeycloakSessionFactory()
                .getProviderFactoriesStream(CredentialSigner.class)
                .filter(CredentialSignerFactory.class::isInstance)
                .map(CredentialSignerFactory.class::cast)
                .map(CredentialSignerFactory::getSupportedFormat)
                .toList();

        // Supported formats must have a builder AND a signer
        List<String> supportedFormats = new ArrayList<>(supportedFormatsByBuilders);
        supportedFormats.retainAll(supportedFormatsBySigners);

        return supportedFormats;
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
