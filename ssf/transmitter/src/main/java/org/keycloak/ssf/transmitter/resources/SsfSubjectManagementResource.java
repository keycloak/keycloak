package org.keycloak.ssf.transmitter.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.ssf.transmitter.subject.SubjectManagementResult;
import org.keycloak.ssf.transmitter.subject.SubjectManagementService;
import org.keycloak.ssf.transmitter.support.SsfAuthUtil;
import org.keycloak.ssf.transmitter.support.SsfErrorRepresentation;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

/**
 * Receiver-facing subject management endpoints per SSF 1.0 §8.1.3.
 * Authenticated via the receiver's bearer token with {@code ssf.manage}
 * scope.
 *
 * <p>Privacy-by-default: unknown subjects and unsupported formats
 * return 200/204 in silent mode (the default). Verbose mode
 * (opt-in via SPI config) returns informative 400/404 status codes
 * for debugging.
 */
public class SsfSubjectManagementResource {

    private static final Logger log = Logger.getLogger(SsfSubjectManagementResource.class);

    protected final KeycloakSession session;

    protected final SubjectManagementService subjectManagementService;

    protected final boolean verbose;

    public SsfSubjectManagementResource(KeycloakSession session,
                                        SubjectManagementService subjectManagementService,
                                        boolean verbose) {
        this.session = session;
        this.subjectManagementService = subjectManagementService;
        this.verbose = verbose;
    }

    /**
     * Adds a subject to the caller's stream (SSF §8.1.3.2).
     */
    @POST
    @Path("/add")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Ssf.Tags.TRANSMITTER)
    @Operation(
            summary = "Add subject to stream",
            description = "Adds a subject to the caller's stream (SSF 1.0 §7.1.3.2). In privacy-by-default (silent) mode, unknown subjects or unsupported formats return 200 without a body."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "404", description = "Stream or subject not found (verbose mode only)")
    })
    public Response addSubject(AddSubjectRequest request) {

        if (!SsfAuthUtil.canManage()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (request == null || request.getStreamId() == null || request.getSubject() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request",
                            "stream_id and subject are required"))
                    .build();
        }

        ClientModel receiverClient = session.getContext().getClient();
        SubjectManagementResult result = subjectManagementService.addSubject(receiverClient.getId(), request);

        return toAddResponse(result);
    }

    protected Response toAddResponse(SubjectManagementResult result) {
        // 200 OK per SSF §8.1.3.2. We include an explicit empty JSON
        // entity so the response carries a Content-Type header —
        // Keycloak's DefaultSecurityHeadersProvider rejects 2xx
        // responses with no media type as a 500 internal error.
        if (result == SubjectManagementResult.OK) {
            return okEmptyJson();
        }
        if (!verbose) {
            return okEmptyJson();
        }
        return switch (result) {
            case STREAM_NOT_FOUND -> Response.status(Response.Status.NOT_FOUND)
                    .entity(new SsfErrorRepresentation("not_found", "stream not found"))
                    .build();
            case SUBJECT_NOT_FOUND -> Response.status(Response.Status.NOT_FOUND)
                    .entity(new SsfErrorRepresentation("not_found", "subject not recognized"))
                    .build();
            case FORMAT_UNSUPPORTED -> Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request", "unsupported subject format"))
                    .build();
            default -> okEmptyJson();
        };
    }

    /**
     * 200 OK with an explicit empty JSON object body. Required because
     * Keycloak's response-header filter rejects 2xx responses without
     * a media type. The empty object is a valid JSON value receivers
     * can parse without special-casing.
     */
    protected Response okEmptyJson() {
        return Response.ok("{}", MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Removes a subject from the caller's stream (SSF §8.1.3.3).
     */
    @POST
    @Path("/remove")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Ssf.Tags.TRANSMITTER)
    @Operation(
            summary = "Remove subject from stream",
            description = "Removes a subject from the caller's stream (SSF 1.0 §7.1.3.3). In privacy-by-default (silent) mode, unknown subjects or unsupported formats return 204."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "404", description = "Stream not found (verbose mode only)")
    })
    public Response removeSubject(RemoveSubjectRequest request) {

        if (!SsfAuthUtil.canManage()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (request == null || request.getStreamId() == null || request.getSubject() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request",
                            "stream_id and subject are required"))
                    .build();
        }

        ClientModel receiverClient = session.getContext().getClient();
        SubjectManagementResult result = subjectManagementService.removeSubject(receiverClient.getId(), request);

        return toRemoveResponse(result);
    }

    protected Response toRemoveResponse(SubjectManagementResult result) {
        if (result == SubjectManagementResult.OK) {
            return Response.noContent().build();
        }
        if (!verbose) {
            return Response.noContent().build();
        }
        return switch (result) {
            case STREAM_NOT_FOUND -> Response.status(Response.Status.NOT_FOUND)
                    .entity(new SsfErrorRepresentation("not_found", "stream not found"))
                    .build();
            case SUBJECT_NOT_FOUND -> Response.noContent().build();
            case FORMAT_UNSUPPORTED -> Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SsfErrorRepresentation("invalid_request", "unsupported subject format"))
                    .build();
            default -> Response.noContent().build();
        };
    }
}
