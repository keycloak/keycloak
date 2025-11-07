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
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.event.SecurityEventToken;
import org.keycloak.protocol.ssf.event.parser.SecurityEventTokenParsingException;
import org.keycloak.protocol.ssf.event.processor.SsfSecurityEventContext;
import org.keycloak.protocol.ssf.receiver.SsfReceiverModel;
import org.keycloak.protocol.ssf.spi.SsfProvider;
import org.keycloak.protocol.ssf.support.SsfSetPushDeliveryFailureResponse;

import java.util.Set;

import static org.keycloak.protocol.ssf.Ssf.APPLICATION_SECEVENT_JWT_TYPE;
import static org.keycloak.protocol.ssf.support.SsfSetPushDeliveryResponseUtil.newSsfSetPushDeliveryFailureResponse;
import static org.keycloak.utils.KeycloakSessionUtil.getKeycloakSession;

/**
 * Implements RFC 8935 Push-Based Security Event Token (SET) Delivery Using HTTP
 * <p>
 * https://www.rfc-editor.org/rfc/rfc8935.html
 */
public class SsfPushDeliveryEndpoint {

    protected static final Logger log = Logger.getLogger(SsfPushDeliveryEndpoint.class);

    protected final SsfProvider ssfProvider;

    public SsfPushDeliveryEndpoint(SsfProvider ssfProvider) {
        this.ssfProvider = ssfProvider;
    }

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

        SsfReceiverModel receiverModel = lookupReceiverModel(receiverAlias, context);
        if (receiverModel == null) {
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, "Invalid receiver");
        }

        checkPushAuthorizationToken(authToken, receiverModel);

        var securityEventContext = ssfProvider.createSecurityEventContext(null, receiverModel);

        SecurityEventToken securityEventToken = parseSecurityEventToken(encodedSecurityEventToken, securityEventContext);

        RealmModel realm = context.getRealm();
        if (securityEventToken == null) {
            log.debugf("Rejected invalid security event token. realm=%s receiverAlias=%s", realm.getName(), receiverAlias);
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, "Invalid security event token");
        }

        // Security Event Token is parsed and validated here
        log.debugf("Ingesting valid security event token. realm=%s receiverAlias=%s jti=%s", realm.getName(), receiverAlias, securityEventToken.getId());

        checkIssuer(receiverModel, securityEventToken, securityEventToken.getIssuer());

        checkAudience(receiverModel, securityEventToken, securityEventToken.getAudience());

        securityEventContext.setSecurityEventToken(securityEventToken);

        handleSecurityEvent(securityEventContext);

        if (!securityEventContext.isProcessedSuccessfully()) {
            // See 2.3. Failure Response https://www.rfc-editor.org/rfc/rfc8935.html#section-2.3
            return Response.serverError().type(MediaType.APPLICATION_JSON).build();
        }

        // See 2.2. Success Response https://www.rfc-editor.org/rfc/rfc8935.html#section-2.2
        return Response.accepted().type(MediaType.APPLICATION_JSON).build();
    }

    protected SsfReceiverModel lookupReceiverModel(String receiverAlias, KeycloakContext context) {
        return ssfProvider.receiverManager().getReceiverModel(context, receiverAlias);
    }

    protected SecurityEventToken parseSecurityEventToken(String encodedSecurityEventToken, SsfSecurityEventContext securityEventContext) {
        try {
            return ssfProvider.parseSecurityEventToken(encodedSecurityEventToken, securityEventContext);
        } catch (SecurityEventTokenParsingException sepe) {
            // see https://www.rfc-editor.org/rfc/rfc8935.html#section-2.4
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_REQUEST, sepe.getMessage());
        }
    }

    protected void handleSecurityEvent(SsfSecurityEventContext securityEventContext) {
        ssfProvider.processSecurityEvents(securityEventContext);
    }

    protected void checkIssuer(SsfReceiverModel receiverModel, SecurityEventToken securityEventToken, String issuer) {

        String expectedIssuer = receiverModel.getReceiverProviderConfig() != null ? receiverModel.getReceiverProviderConfig().getIssuer() : null;
        if (expectedIssuer == null) {
            expectedIssuer = receiverModel.getIssuer();
        }

        if (!isValidIssuer(receiverModel, expectedIssuer, issuer)) {
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_ISSUER, "Invalid issuer");
        }
    }

    protected void checkPushAuthorizationToken(String receivedAuthHeader, SsfReceiverModel receiverModel) {

        String expectedAuthHeader = receiverModel.getReceiverProviderConfig() != null ? receiverModel.getReceiverProviderConfig().getPushAuthorizationHeader() : null;
        if (expectedAuthHeader == null) {
            expectedAuthHeader = receiverModel.getPushAuthorizationHeader();
        }

        if (expectedAuthHeader != null) {
            if (!isValidPushAuthorizationHeader(receiverModel, receivedAuthHeader, expectedAuthHeader)) {
                throw newSsfSetPushDeliveryFailureResponse(Response.Status.UNAUTHORIZED, SsfSetPushDeliveryFailureResponse.ERROR_AUTHENTICATION_FAILED, "Invalid push authorization header");
            }
        }
    }

    protected void checkAudience(SsfReceiverModel receiverModel, SecurityEventToken securityEventToken, String[] audience) {

        var expectedAudience = receiverModel.getAudience();

        if (!isValidAudience(receiverModel, expectedAudience, audience)) {
            throw newSsfSetPushDeliveryFailureResponse(Response.Status.BAD_REQUEST, SsfSetPushDeliveryFailureResponse.ERROR_INVALID_AUDIENCE, "Invalid audience");
        }
    }

    protected boolean isValidIssuer(SsfReceiverModel receiverModel, String expectedIssuer, String issuer) {
        return expectedIssuer.equals(issuer);
    }

    protected boolean isValidAudience(SsfReceiverModel receiverModel, Set<String> expectedAudience, String[] audience) {
        return expectedAudience.containsAll(Set.of(audience));
    }

    protected boolean isValidPushAuthorizationHeader(SsfReceiverModel receiverModel, String authHeader, String expectedAuthHeader) {
        return expectedAuthHeader.equals(authHeader);
    }

}
