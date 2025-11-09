package org.keycloak.protocol.ssf.endpoint;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.parser.SecurityEventTokenParsingException;
import org.keycloak.protocol.ssf.event.processor.SsfSecurityEventContext;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.SsfReceiverProviderFactory;
import org.keycloak.protocol.ssf.spi.SsfProvider;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.urls.UrlType;

import java.util.Set;

import static org.keycloak.utils.KeycloakSessionUtil.getKeycloakSession;

/**
 * SsfPushDeliveryResource implements the RFC 8935 Push-Based Security Event Token (SET) Delivery Using HTTP.
 * <p>
 * See: https://www.rfc-editor.org/rfc/rfc8935.html
 */
public class SsfPushDeliveryResource {

    protected static final Logger log = Logger.getLogger(SsfPushDeliveryResource.class);

    public static final String APPLICATION_SECEVENT_JWT_TYPE = "application/secevent+jwt";

    protected final SsfProvider ssfProvider;

    public SsfPushDeliveryResource(SsfProvider ssfProvider) {
        this.ssfProvider = ssfProvider;
    }

    /**
     * Handles legacy SSF requests, which don't send the `Content-type: application/secevent+jwt` in the request.
     *
     * The endpoint is available via {@code $KC_ISSUER_URL/ssf/push/{receiverAlias}}
     *
     * @param receiverAlias
     * @param encodedSecurityEventToken
     * @param authToken
     * @param contentType
     * @return
     */
    @Path("{receiverAlias}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    // @Consumes(APPLICATION_SECEVENT_JWT_TYPE)  // some SSF providers don't set the correct content-type
    public Response invalidSecurityEventTokenRequest(@PathParam("receiverAlias") String receiverAlias, //
                                                     String encodedSecurityEventToken,  //
                                                     @HeaderParam(HttpHeaders.AUTHORIZATION) String authToken, //
                                                     @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType //
    ) {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    /**
     * Handles PUSH based SET delivery via HTTP.
     *
     * The endpoint is available via {@code $KC_ISSUER_URL/ssf/push/{receiverAlias}}
     *
     * @param receiverAlias
     * @param encodedSecurityEventToken
     * @param authToken
     * @param contentType
     * @return
     */
    @Path("{receiverAlias}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(APPLICATION_SECEVENT_JWT_TYPE)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF_PUSH)
    @Operation(summary = "SSF Push delivery endpoint for this realm.")
    @APIResponses(value = {
            @APIResponse(responseCode = "202", description = "Accepted"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
    })
    public Response ingestSecurityEventToken(@PathParam("receiverAlias") String receiverAlias, //
                                             String encodedSecurityEventToken,  //
                                             @HeaderParam(HttpHeaders.AUTHORIZATION) String authToken, //
                                             @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType //
    ) {

        KeycloakSession session = getKeycloakSession();
        KeycloakContext context = session.getContext();

        SsfReceiver receiver = lookupReceiver(session, receiverAlias, context);
        if (receiver == null) {
            log.debugf("Ignoring security event token received for unknown receiver. receiverAlias=%s", receiverAlias);
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, "Invalid receiver");
        }

        if (!receiver.getConfig().isEnabled()) {
            log.debugf("Ignoring security event token received for disabled receiver. receiverAlias=%s", receiverAlias);
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, "Receiver is disabled");
        }

        checkPushAuthorizationToken(session, receiver, authToken);

        var securityEventContext = ssfProvider.createSecurityEventContext(null, receiver);

        SecurityEventToken securityEventToken = parseSecurityEventToken(session, encodedSecurityEventToken, securityEventContext);

        RealmModel realm = context.getRealm();
        if (securityEventToken == null) {
            log.debugf("Rejected invalid security event token. realm=%s receiverAlias=%s", realm.getName(), receiverAlias);
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, "Invalid security event token");
        }

        // Security Event Token is parsed and signature validated from here on
        log.debugf("Ingesting valid security event token. realm=%s receiverAlias=%s jti=%s", realm.getName(), receiverAlias, securityEventToken.getId());

        // Perform additional validations
        checkIssuer(session, receiver, securityEventToken, securityEventToken.getIssuer());
        checkAudience(session, receiver, securityEventToken, securityEventToken.getAudience());

        // Security Event Token is valid
        securityEventContext.setSecurityEventToken(securityEventToken);

        handleSecurityEvent(session, securityEventContext);

        if (!securityEventContext.isProcessedSuccessfully()) {
            // See 2.3. Failure Response https://www.rfc-editor.org/rfc/rfc8935.html#section-2.3
            return Response.serverError().type(MediaType.APPLICATION_JSON).build();
        }

        // See 2.2. Success Response https://www.rfc-editor.org/rfc/rfc8935.html#section-2.2
        return Response.accepted().type(MediaType.APPLICATION_JSON).build();
    }

    protected SsfReceiver lookupReceiver(KeycloakSession session, String receiverAlias, KeycloakContext context) {
        return SsfReceiverProviderFactory.getSsfReceiver(session, context.getRealm(), receiverAlias);
    }

    protected SecurityEventToken parseSecurityEventToken(KeycloakSession session, String encodedSecurityEventToken, SsfSecurityEventContext securityEventContext) {
        try {
            return ssfProvider.parseSecurityEventToken(encodedSecurityEventToken, securityEventContext);
        } catch (SecurityEventTokenParsingException sepe) {
            // see https://www.rfc-editor.org/rfc/rfc8935.html#section-2.4
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, sepe.getMessage());
        }
    }

    protected void handleSecurityEvent(KeycloakSession session, SsfSecurityEventContext securityEventContext) {
        ssfProvider.processSecurityEvents(securityEventContext);
    }

    protected void checkIssuer(KeycloakSession session, SsfReceiver receiver, SecurityEventToken securityEventToken, String issuer) {

        String expectedIssuer = receiver.getConfig() != null ? receiver.getConfig().getIssuer() : null;

        if (!isValidIssuer(receiver, expectedIssuer, issuer)) {
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_ISSUER, "Invalid issuer");
        }
    }

    protected void checkPushAuthorizationToken(KeycloakSession session, SsfReceiver receiver, String receivedAuthHeader) {

        String expectedAuthHeader = receiver.getConfig() != null ? receiver.getConfig().getPushAuthorizationHeader() : null;

        if (expectedAuthHeader != null) {
            if (!isValidPushAuthorizationHeader(receiver, receivedAuthHeader, expectedAuthHeader)) {
                throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_AUTHENTICATION_FAILED, "Invalid push authorization header");
            }
        }
    }

    protected void checkAudience(KeycloakSession session, SsfReceiver receiver, SecurityEventToken securityEventToken, String[] audience) {

        Set<String> expectedAudience = receiver.getConfig() != null && receiver.getConfig().getStreamAudience() != null ? receiver.getConfig().streamAudience() : null;

        if (expectedAudience == null) {
            // No expected audience configured for receiver, fallback to realm issuer is no audience is set
            String fallbackAudience = getFallbackAudience(session);
            expectedAudience = Set.of(fallbackAudience);
        }

        if (!isValidAudience(receiver, expectedAudience, audience)) {
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_AUDIENCE, "Invalid audience");
        }
    }

    protected String getFallbackAudience(KeycloakSession session) {
        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        return Urls.realmIssuer(frontendUriInfo.getBaseUri(), session.getContext().getRealm().getName());
    }

    protected boolean isValidIssuer(SsfReceiver receiver, String expectedIssuer, String issuer) {
        return expectedIssuer.equals(issuer);
    }

    protected boolean isValidAudience(SsfReceiver receiver, Set<String> expectedAudience, String[] audience) {
        return expectedAudience.containsAll(Set.of(audience));
    }

    protected boolean isValidPushAuthorizationHeader(SsfReceiver receiver, String authHeader, String expectedAuthHeader) {
        return expectedAuthHeader.equals(authHeader);
    }

}
