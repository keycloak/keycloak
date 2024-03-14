package org.keycloak.protocol.oid4vc.issuance;

import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.model.OID4VCClient;
import org.keycloak.protocol.oid4vc.OID4VCClientRegistrationProvider;
import org.keycloak.protocol.oid4vc.OID4VCClientRegistrationProviderFactory;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.signing.VCSigningServiceProviderFactory;
import org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.SupportedCredential;
import org.keycloak.services.Urls;
import org.keycloak.urls.UrlType;
import org.keycloak.wellknown.WellKnownProvider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link  WellKnownProvider} implementation to provide the .well-known/openid-credential-issuer endpoint, offering
 * the Credential Issuer Metadata as defined by the OID4VCI protocol
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-10.2.2}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCIssuerWellKnownProvider implements WellKnownProvider {

    private static final Logger LOGGER = Logger.getLogger(OID4VCIssuerWellKnownProvider.class);
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

    public static Map<String, SupportedCredential> getSupportedCredentials(KeycloakSession keycloakSession) {
        LOGGER.debug("Get supported credentials.");
        var realm = keycloakSession.getContext().getRealm();
        List<Format> supportedFormats = realm.getComponentsStream(realm.getId(), VerifiableCredentialsSigningService.class.getName())
                .map(cm ->
                        keycloakSession
                                .getKeycloakSessionFactory()
                                .getProviderFactory(VerifiableCredentialsSigningService.class, cm.getProviderId())
                )
                .filter(pf -> pf instanceof VCSigningServiceProviderFactory)
                .map(sspf -> (VCSigningServiceProviderFactory) sspf)
                .map(VCSigningServiceProviderFactory::supportedFormat)
                .toList();

        return keycloakSession.getContext()
                .getRealm()
                .getClientsStream()
                .filter(cm -> cm.getProtocol() != null)
                .filter(cm -> cm.getProtocol().equals(OID4VCClientRegistrationProviderFactory.PROTOCOL_ID))
                .map(cm -> OID4VCClientRegistrationProvider.fromClientAttributes(cm.getClientId(), cm.getAttributes()))
                .map(OID4VCClient::getSupportedVCTypes)
                .flatMap(List::stream)
                .filter(sc -> supportedFormats.contains(sc.getFormat()))
                .distinct()
                .collect(Collectors.toMap(SupportedCredential::getId, sc -> sc, (sc1, sc2) -> sc1));

    }

    public static String getIssuer(KeycloakContext context) {
        UriInfo frontendUriInfo = context.getUri(UrlType.FRONTEND);
        return Urls.realmIssuer(frontendUriInfo.getBaseUri(),
                context.getRealm().getName());

    }

    public static String getCredentialsEndpoint(KeycloakContext context) {
        return getIssuer(context) + "/protocol/" + OID4VCLoginProtocolFactory.PROTOCOL_ID + "/" + OID4VCIssuerEndpoint.CREDENTIAL_PATH;
    }
}