package org.keycloak.protocol.ssf.receiver.resources;

import java.security.MessageDigest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.event.token.SsfSecurityEventToken;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.event.parser.SecurityEventTokenParsingException;
import org.keycloak.protocol.ssf.receiver.event.processor.SsfEventContext;
import org.keycloak.protocol.ssf.receiver.registration.SsfReceiverRegistrationProviderFactory;
import org.keycloak.protocol.ssf.receiver.spi.SsfReceiverProvider;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.utils.KeycloakSessionUtil;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

public class SsfReceiversResource {

    protected static final Logger LOG = Logger.getLogger(SsfReceiversResource.class);

    /**
     * Handles legacy SSF requests, which don't send the `Content-type: application/secevent+jwt` in the request.
     * <p>
     * The endpoint is available via {@code $KC_ISSUER_URL/ssf/receivers/{receiverAlias}/push}
     *
     * @param encodedSecurityEventToken
     * @param authToken
     * @param contentType
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{receiverAlias}/push")
    // @Consumes(APPLICATION_SECEVENT_JWT_TYPE)  // some SSF providers don't set the correct content-type
    public Response invalidSecurityEventTokenRequest(@PathParam("receiverAlias") String receiverAlias,
                                                     String encodedSecurityEventToken,  //
                                                     @HeaderParam(HttpHeaders.AUTHORIZATION) String authToken, //
                                                     @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType //
    ) {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    /**
     * Handles PUSH based SET delivery via HTTP.
     * <p>
     * The endpoint is available via {@code $KC_ISSUER_URL/ssf/receivers/{receiverAlias}/push}
     *
     * @param encodedSecurityEventToken
     * @param authToken
     * @param contentType
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(Ssf.APPLICATION_SECEVENT_JWT_TYPE)
    @Path("/{receiverAlias}/push")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.SSF_PUSH)
    @Operation(summary = "SSF Push delivery endpoint for this realm.")
    @APIResponses(value = {
            @APIResponse(responseCode = "202", description = "Accepted"),
            @APIResponse(responseCode = "400", description = "Bad request"),
    })
    public Response ingestSecurityEventToken(@PathParam("receiverAlias") String receiverAlias,
                                             String encodedSecurityEventToken,  //
                                             @HeaderParam(HttpHeaders.AUTHORIZATION) String authToken, //
                                             @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType //
    ) {

        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        KeycloakContext context = session.getContext();

        SsfReceiver receiver = lookupReceiver(session, receiverAlias, context);
        if (receiver == null) {
            LOG.debugf("Ignoring security event token received for unknown receiver. receiverAlias=%s", receiverAlias);
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, "Invalid receiver");
        }

        if (!receiver.getConfig().isEnabled()) {
            LOG.debugf("Ignoring security event token received for disabled receiver. receiverAlias=%s", receiverAlias);
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, "Receiver is disabled");
        }

        checkPushAuthorizationToken(session, receiver, authToken);

        SsfReceiverProvider receiverProvider = Ssf.receiver();

        var eventContext = receiverProvider.createEventContext(null, receiver);

        SsfSecurityEventToken securityEventToken = parseSecurityEventToken(session, receiverProvider, encodedSecurityEventToken, eventContext);

        RealmModel realm = context.getRealm();
        if (securityEventToken == null) {
            LOG.debugf("Rejected invalid security event token. realm=%s receiverAlias=%s", realm.getName(), receiverAlias);
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, "Invalid security event token");
        }

        // Security Event Token is parsed and signature validated from here on
        LOG.debugf("Ingesting security event token. realm=%s receiverAlias=%s jti=%s", realm.getName(), receiverAlias, securityEventToken.getJti());

        // Security Event Token parsed
        eventContext.setSecurityEventToken(securityEventToken);

        handleEvents(session, receiverProvider, securityEventToken, eventContext);

        if (!eventContext.isProcessedSuccessfully()) {
            // See 2.3. Failure Response https://www.rfc-editor.org/rfc/rfc8935.html#section-2.3
            return Response.serverError().type(MediaType.APPLICATION_JSON).build();
        }

        // See 2.2. Success Response https://www.rfc-editor.org/rfc/rfc8935.html#section-2.2
        return Response.accepted().type(MediaType.APPLICATION_JSON).build();
    }

    protected SsfReceiver lookupReceiver(KeycloakSession session, String receiverAlias, KeycloakContext context) {
        return SsfReceiverRegistrationProviderFactory.getSsfReceiver(session, context.getRealm(), receiverAlias);
    }

    protected SsfSecurityEventToken parseSecurityEventToken(KeycloakSession session, SsfReceiverProvider receiverProvider, String encodedSecurityEventToken, SsfEventContext eventContext) {
        try {
            return receiverProvider.parseSecurityEventToken(encodedSecurityEventToken, eventContext);
        } catch (SecurityEventTokenParsingException sepe) {
            // see https://www.rfc-editor.org/rfc/rfc8935.html#section-2.4
            throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, sepe.getMessage());
        }
    }

    protected void handleEvents(KeycloakSession session, SsfReceiverProvider receiverProvider, SsfSecurityEventToken securityEventToken, SsfEventContext eventContext) {
        receiverProvider.processEvents(securityEventToken, eventContext);
    }

    protected void checkPushAuthorizationToken(KeycloakSession session, SsfReceiver receiver, String receivedAuthHeader) {

        String expectedAuthHeader = receiver.getConfig() != null ? receiver.getConfig().getPushAuthorizationHeader() : null;

        if (expectedAuthHeader != null) {
            if (!isValidPushAuthorizationHeader(receiver, receivedAuthHeader, expectedAuthHeader)) {
                throw SsfSetPushDeliveryFailureResponse.newFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_AUTHENTICATION_FAILED, "Invalid push authorization header");
            }
        }
    }

    protected boolean isValidPushAuthorizationHeader(SsfReceiver receiver, String authHeader, String expectedAuthHeader) {
        if (authHeader == null) {
            return false;
        }

        if (authHeader.startsWith("Bearer ")) {
            authHeader = authHeader.substring("Bearer ".length());
        }

        // Use constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(expectedAuthHeader.getBytes(), authHeader.getBytes());
    }
}
