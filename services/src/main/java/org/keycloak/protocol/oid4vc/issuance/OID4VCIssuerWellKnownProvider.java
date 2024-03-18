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
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.OID4VCClientRegistrationProvider;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.signing.VCSigningServiceProviderFactory;
import org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.OID4VCClient;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.services.Urls;
import org.keycloak.urls.UrlType;
import org.keycloak.wellknown.WellKnownProvider;

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
     * It will take into account the configured {@link VerifiableCredentialsSigningService}'s and there supported format
     * and the credentials supported by the clients available in the session.
     */
    public static Map<String, SupportedCredentialConfiguration> getSupportedCredentials(KeycloakSession keycloakSession) {

        RealmModel realm = keycloakSession.getContext().getRealm();
        List<Format> supportedFormats = realm.getComponentsStream(realm.getId(), VerifiableCredentialsSigningService.class.getName())
                .map(cm ->
                        keycloakSession
                                .getKeycloakSessionFactory()
                                .getProviderFactory(VerifiableCredentialsSigningService.class, cm.getProviderId())
                )
                .filter(VCSigningServiceProviderFactory.class::isInstance)
                .map(VCSigningServiceProviderFactory.class::cast)
                .map(VCSigningServiceProviderFactory::supportedFormat)
                .toList();

        return keycloakSession.getContext()
                .getRealm()
                .getClientsStream()
                .filter(cm -> cm.getProtocol() != null)
                .filter(cm -> cm.getProtocol().equals(OID4VCLoginProtocolFactory.PROTOCOL_ID))
                .map(cm -> OID4VCClientRegistrationProvider.fromClientAttributes(cm.getClientId(), cm.getAttributes()))
                .map(OID4VCClient::getSupportedVCTypes)
                .flatMap(List::stream)
                .filter(sc -> supportedFormats.contains(sc.getFormat()))
                .distinct()
                .collect(Collectors.toMap(SupportedCredentialConfiguration::getId, sc -> sc, (sc1, sc2) -> sc1));

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
}