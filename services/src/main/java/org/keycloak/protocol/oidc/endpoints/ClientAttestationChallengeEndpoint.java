package org.keycloak.protocol.oidc.endpoints;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.issuance.keybinding.CNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtCNonceHandler;
import org.keycloak.protocol.oidc.ClientAttestationChallengeResponse;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.cors.Cors;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.ProfileHelper;

/**
 * OAuth 2.0 Attestation-Based Client Authentication challenge endpoint.
 *
 * @author <a href="mailto:ogenbertrand@gmail.com">Bertrand Ogen</a>
 */
public class ClientAttestationChallengeEndpoint {

    public static final String PATH = "attestation/challenge";

    private final KeycloakSession session;
    private final RealmModel realm;
    private final ClientConnection clientConnection;

    public ClientAttestationChallengeEndpoint(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.clientConnection = session.getContext().getConnection();
    }

    public static UriBuilder challengeUrl(UriBuilder baseUriBuilder) {
        return OIDCLoginProtocolService.tokenServiceBaseUrl(baseUriBuilder)
                .path(OIDCLoginProtocolService.class, "clientAttestationChallenge");
    }

    public static String getChallengeEndpoint(KeycloakContext context) {
        return challengeUrl(context.getUri(UrlType.BACKEND).getBaseUriBuilder())
                .build(context.getRealm().getName(), OIDCLoginProtocol.LOGIN_PROTOCOL)
                .toString();
    }

    public static String buildChallenge(KeycloakSession session) {
        CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
        if (cNonceHandler == null) {
            throw new IllegalStateException("Client attestation challenge handler is not configured");
        }

        String issuer = Urls.realmIssuer(session.getContext().getUri(UrlType.FRONTEND).getBaseUri(),
                session.getContext().getRealm().getName());
        return cNonceHandler.buildCNonce(
                List.of(issuer),
                Map.of(JwtCNonceHandler.SOURCE_ENDPOINT, getChallengeEndpoint(session.getContext())));
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestChallenge() {
        ProfileHelper.requireFeature(Profile.Feature.CLIENT_AUTH_ABCA);
        Cors cors = Cors.builder().auth().allowedMethods("POST").auth()
                .exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS,
                        AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_CHALLENGE_HEADER);
        checkSsl(cors);

        String challenge = buildChallenge(session);
        ClientAttestationChallengeResponse challengeResponse = new ClientAttestationChallengeResponse();
        challengeResponse.setAttestationChallenge(challenge);

        return cors.allowAllOrigins().add(Response.ok(challengeResponse)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Pragma", "no-cache")
                .header(HttpHeaders.DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)))
                .header(AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_CHALLENGE_HEADER, challenge));
    }

    private void checkSsl(Cors cors) {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https")
                && realm.getSslRequired().isRequired(clientConnection)) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), OAuthErrorException.INVALID_REQUEST,
                    "HTTPS required", Response.Status.FORBIDDEN);
        }
    }
}
