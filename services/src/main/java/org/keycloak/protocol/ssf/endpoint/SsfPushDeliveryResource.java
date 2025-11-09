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
import org.keycloak.urls.UrlType;

import java.util.Set;

import static org.keycloak.protocol.ssf.Ssf.APPLICATION_SECEVENT_JWT_TYPE;
import static org.keycloak.protocol.ssf.endpoint.SsfSetPushDeliveryResponseUtil.newSsfSetPushDeliveryFailureResponse;
import static org.keycloak.utils.KeycloakSessionUtil.getKeycloakSession;

/**
 * Implements RFC 8935 Push-Based Security Event Token (SET) Delivery Using HTTP
 * <p>
 * https://www.rfc-editor.org/rfc/rfc8935.html
 */
public class SsfPushDeliveryResource {

    protected static final Logger log = Logger.getLogger(SsfPushDeliveryResource.class);

    protected final SsfProvider ssfProvider;

    public SsfPushDeliveryResource(SsfProvider ssfProvider) {
        this.ssfProvider = ssfProvider;
    }

    /**
     *
     *
     * $ISSUER/ssf/push/{receiverAlias}
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
     * $ISSUER/ssf/push/{receiverAlias}
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
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, "Invalid receiver");
        }

        if (!receiver.getReceiverProviderConfig().isEnabled()) {
            log.debugf("Ignoring security event token received for disabled receiver. receiverAlias=%s", receiverAlias);
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, "Receiver is disabled");
        }

        checkPushAuthorizationToken(session, receiver, authToken);

        var securityEventContext = ssfProvider.createSecurityEventContext(null, receiver);

        SecurityEventToken securityEventToken = parseSecurityEventToken(session, encodedSecurityEventToken, securityEventContext);

        RealmModel realm = context.getRealm();
        if (securityEventToken == null) {
            log.debugf("Rejected invalid security event token. realm=%s receiverAlias=%s", realm.getName(), receiverAlias);
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, "Invalid security event token");
        }

        // Security Event Token is parsed and validated here
        log.debugf("Ingesting valid security event token. realm=%s receiverAlias=%s jti=%s", realm.getName(), receiverAlias, securityEventToken.getId());

        checkIssuer(session, receiver, securityEventToken, securityEventToken.getIssuer());

        checkAudience(session, receiver, securityEventToken, securityEventToken.getAudience());

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
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, sepe.getMessage());
        }
    }

    protected void handleSecurityEvent(KeycloakSession session, SsfSecurityEventContext securityEventContext) {
        ssfProvider.processSecurityEvents(securityEventContext);
    }

    protected void checkIssuer(KeycloakSession session, SsfReceiver receiver, SecurityEventToken securityEventToken, String issuer) {

        String expectedIssuer = receiver.getReceiverProviderConfig() != null ? receiver.getReceiverProviderConfig().getIssuer() : null;

        if (!isValidIssuer(receiver, expectedIssuer, issuer)) {
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_ISSUER, "Invalid issuer");
        }
    }

    protected void checkPushAuthorizationToken(KeycloakSession session, SsfReceiver receiver, String receivedAuthHeader) {

        String expectedAuthHeader = receiver.getReceiverProviderConfig() != null ? receiver.getReceiverProviderConfig().getPushAuthorizationHeader() : null;

        if (expectedAuthHeader != null) {
            if (!isValidPushAuthorizationHeader(receiver, receivedAuthHeader, expectedAuthHeader)) {
                throw newSsfSetPushDeliveryFailureResponse(Response.Status.UNAUTHORIZED, SsfSetPushDeliveryFailureResponse.ERROR_AUTHENTICATION_FAILED, "Invalid push authorization header");
            }
        }
    }

    protected void checkAudience(KeycloakSession session, SsfReceiver receiver, SecurityEventToken securityEventToken, String[] audience) {

        Set<String> expectedAudience = receiver.getReceiverProviderConfig() != null && receiver.getReceiverProviderConfig().getStreamAudience() != null ? receiver.getReceiverProviderConfig().streamAudience() : null;

        if (expectedAudience == null) {
            // No expected audience configured for receiver, fallback to realm issuer is no audience is set
            String fallbackAudience = getFallbackAudience(session);
            expectedAudience = Set.of(fallbackAudience);
        }

        if (!isValidAudience(receiver, expectedAudience, audience)) {
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_AUDIENCE, "Invalid audience");
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
