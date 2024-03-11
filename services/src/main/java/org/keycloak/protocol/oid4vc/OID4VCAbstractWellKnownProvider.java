package org.keycloak.protocol.oid4vc;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.signing.VCSigningServiceProviderFactory;
import org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.SupportedCredential;
import org.keycloak.services.Urls;
import org.keycloak.urls.UrlType;
import org.keycloak.wellknown.WellKnownProvider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Super class for the OID4VC focused {@link  WellKnownProvider}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public abstract class OID4VCAbstractWellKnownProvider implements WellKnownProvider {

    private static final Logger LOGGER = Logger.getLogger(OID4VCAbstractWellKnownProvider.class);
    protected final KeycloakSession keycloakSession;
    protected final ObjectMapper objectMapper;

    protected OID4VCAbstractWellKnownProvider(KeycloakSession keycloakSession,
                                              ObjectMapper objectMapper) {
        this.keycloakSession = keycloakSession;
        this.objectMapper = objectMapper;
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