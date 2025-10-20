package org.keycloak.protocol.ssf.event.delivery.push;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.parser.SsfParsingException;
import org.keycloak.protocol.ssf.event.processor.SsfEventContext;
import org.keycloak.protocol.ssf.receiver.ReceiverModel;
import org.keycloak.protocol.ssf.spi.SsfProvider;
import org.keycloak.protocol.ssf.support.SsfFailureResponse;

import java.util.Set;

import static org.keycloak.protocol.ssf.support.SsfResponseUtil.newSharedSignalFailureResponse;
import static org.keycloak.utils.KeycloakSessionUtil.getKeycloakSession;

/**
 * Implements RFC 8935 Push-Based Security Event Token (SET) Delivery Using HTTP
 * <p>
 * https://www.rfc-editor.org/rfc/rfc8935.html
 */
public class PushEndpoint {

    protected static final Logger log = Logger.getLogger(PushEndpoint.class);

    protected final SsfProvider ssfProvider;

    public PushEndpoint(SsfProvider ssfProvider) {
        this.ssfProvider = ssfProvider;
    }

    @Path("{receiverAlias}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(APPLICATION_SECEVENT_JWT_TYPE) // some SSF providers don't set the correct content-type
    public Response ingestSecurityEventToken(@PathParam("receiverAlias") String receiverAlias, //
                                             String encodedSecurityEventToken,  //
                                             @HeaderParam(HttpHeaders.AUTHORIZATION) String authToken, //
                                             @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType //
    ) {

        KeycloakSession session = getKeycloakSession();
        KeycloakContext context = session.getContext();

        ReceiverModel receiverModel = lookupReceiverModel(receiverAlias, context);
        if (receiverModel == null) {
            throw newSharedSignalFailureResponse(Response.Status.BAD_REQUEST, SsfFailureResponse.ERROR_INVALID_REQUEST, "Invalid receiver");
        }

        checkPushAuthorizationToken(authToken, receiverModel);

        if (!Ssf.APPLICATION_SECEVENT_JWT_TYPE.equals(contentType)) {
            log.warnf("Received PUSH request with unsupported content type '%s'.", contentType);
        }

        // parse security event token
        var processingContext = ssfProvider.createSecurityEventProcessingContext(null, receiverAlias);

        // TODO validate security event token
        SecurityEventToken securityEventToken = parseSecurityEventToken(encodedSecurityEventToken, processingContext);

        if (securityEventToken == null) {
            throw newSharedSignalFailureResponse(Response.Status.BAD_REQUEST, SsfFailureResponse.ERROR_INVALID_REQUEST, "Invalid security event token");
        }
        RealmModel realm = context.getRealm();
        log.debugf("Ingest security event token. realm=%s receiverAlias=%s jti=%s", realm.getName(), receiverAlias, securityEventToken.getId());

        checkIssuer(receiverModel, securityEventToken, securityEventToken.getIssuer());

        checkAudience(receiverModel, securityEventToken, securityEventToken.getAudience());

        processingContext.setSecurityEventToken(securityEventToken);

        handleSecurityEvent(processingContext);

        if (!processingContext.isProcessedSuccessfully()) {
            // See 2.3. Failure Response https://www.rfc-editor.org/rfc/rfc8935.html#section-2.3
            return Response.serverError().type(MediaType.APPLICATION_JSON).build();
        }

        // See 2.2. Success Response https://www.rfc-editor.org/rfc/rfc8935.html#section-2.2
        return Response.accepted().type(MediaType.APPLICATION_JSON).build();
    }

    protected ReceiverModel lookupReceiverModel(String receiverAlias, KeycloakContext context) {
        return ssfProvider.receiverManager().getReceiverModel(context, receiverAlias);
    }

    protected void checkPushAuthorizationToken(String authToken, ReceiverModel receiverModel) {
        String pushAuthorizationToken = receiverModel.getPushAuthorizationToken();
        if (pushAuthorizationToken != null) {
            if (validatePushAuthToken(receiverModel, authToken, pushAuthorizationToken)) {
                throw newSharedSignalFailureResponse(Response.Status.UNAUTHORIZED, SsfFailureResponse.ERROR_AUTHENTICATION_FAILED, "Invalid auth token");
            }
        }
    }

    protected SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfEventContext processingContext) {
        try {
            return ssfProvider.parseSecurityEventToken(encodedSecurityEventToken, processingContext);
        } catch (SsfParsingException sepe) {
            // see https://www.rfc-editor.org/rfc/rfc8935.html#section-2.4
            throw newSharedSignalFailureResponse(Response.Status.BAD_REQUEST, SsfFailureResponse.ERROR_INVALID_REQUEST, sepe.getMessage());
        }
    }

    protected void handleSecurityEvent(SsfEventContext processingContext) {
        ssfProvider.processSecurityEvents(processingContext);
    }

    protected void checkIssuer(ReceiverModel receiverModel, SecurityEventToken securityEventToken, String issuer) {
        if (!receiverModel.getIssuer().equals(issuer)) {
            throw newSharedSignalFailureResponse(Response.Status.BAD_REQUEST, SsfFailureResponse.ERROR_INVALID_ISSUER, "Invalid issuer");
        }
    }

    protected void checkAudience(ReceiverModel receiverModel, SecurityEventToken securityEventToken, String[] audience) {
        if (!receiverModel.getAudience().containsAll(Set.of(audience))) {
            throw newSharedSignalFailureResponse(Response.Status.BAD_REQUEST, SsfFailureResponse.ERROR_INVALID_AUDIENCE, "Invalid audience");
        }
    }

    protected boolean validatePushAuthToken(ReceiverModel receiverModel, String authToken, String pushAuthorizationToken) {
        return !("Bearer " + pushAuthorizationToken).equals(authToken);
    }

}
