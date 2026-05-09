package org.keycloak.protocol.oid4vc.resources.admin;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserVerifiableCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

public class UserVerifiableCredentialResource {

    private static final Logger logger = Logger.getLogger(UserVerifiableCredentialResource.class);

    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final UserModel user;
    private final KeycloakSession session;
    private final RealmModel realm;

    public UserVerifiableCredentialResource(KeycloakSession session, RealmModel realm, UserModel user, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.user = user;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    @POST
    @Path("credentials")
    @Consumes({MediaType.APPLICATION_JSON})
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(summary = "Create verifiable credential for the user. Once this is successful, user will be able to issue verifiable credentials of the credential type specified type afterwards")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = UserVerifiableCredentialRepresentation.class))),
            @APIResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorRepresentation.class))),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorRepresentation.class)))
    })
    public UserVerifiableCredentialRepresentation createCredential(UserVerifiableCredentialRepresentation representation) {
        auth.users().requireManage(user);
        checkOid4VCIEnabled();

        if (representation.getCreatedDate() != null) {
            throw ErrorResponse.error("Created date not expected to be specified", Response.Status.BAD_REQUEST);
        }
        if (representation.getRevision() != null) {
            throw ErrorResponse.error("Revision not expected to be specified", Response.Status.BAD_REQUEST);
        }

        ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(realm, representation.getCredentialScopeName());
        if (clientScope == null) {
            logger.warn(String.format("Client scope '%s' does not exists in the realm realm '%s'.", representation.getCredentialScopeName(),realm.getName()));
            throw ErrorResponse.error("Client scope does not exists", Response.Status.BAD_REQUEST);
        }
        if (!OID4VCIConstants.OID4VC_PROTOCOL.equals(clientScope.getProtocol())) {
            logger.warn(String.format("Client scope '%s' in the realm realm '%s' does not have protocol '%s'.",
                    representation.getCredentialScopeName(),realm.getName(), OID4VCIConstants.OID4VC_PROTOCOL));
            throw ErrorResponse.error("Client scope has incorrect protocol", Response.Status.BAD_REQUEST);
        }

        try {
            UserVerifiableCredentialModel modelToCreate = RepresentationToModel.toModel(representation);
            UserVerifiableCredentialModel createdModel = session.users().addVerifiableCredential(user.getId(), modelToCreate);

            UserVerifiableCredentialRepresentation createdRep = ModelToRepresentation.toRepresentation(createdModel);
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(createdRep).success();
            return createdRep;
        } catch (ModelDuplicateException mde) {
            logger.warn(String.format("Verifiable credential '%s' already exists for user '%s' in the realm '%s' for credential. Details: '%s'",
                    representation.getCredentialScopeName(), user.getUsername(), realm.getName(), mde.getMessage()));
            throw ErrorResponse.exists("Verifiable credential already exists");
        } catch (ModelException mde) {
            logger.warn(String.format("Error when creating verifiable credential of type '%s' for user '%s' in the realm '%s'. Details: '%s'",
                    representation.getCredentialScopeName(), user.getUsername(), realm.getName(), mde.getMessage()), mde);
            throw ErrorResponse.error("Error when creating verifiable credential", Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @Path("credentials")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(summary = "Get verifiable credentials granted to the user")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public List<UserVerifiableCredentialRepresentation> getCredentials() {
        auth.users().requireView(user);
        checkOid4VCIEnabled();

        return session.users().getVerifiableCredentialsByUser(user.getId())
                .map(ModelToRepresentation::toRepresentation)
                .toList();
    }

    @DELETE
    @Path("credentials/{credentialScopeName}")
    @Operation(summary = "Revoke verifiable credential for particular user")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void revokeCredential(@PathParam("credentialScopeName") String credentialScopeName) {
        auth.users().requireManage(user);
        checkOid4VCIEnabled();

        boolean removed = session.users().removeVerifiableCredential(user.getId(), credentialScopeName);
        if (!removed) {
            logger.warn(String.format("Verifiable credential '%s' not found for user '%s' in the realm '%s'.",
                    credentialScopeName, user.getUsername(), realm.getName()));
            throw new NotFoundException("Verifiable credential not found");
        }

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }

    private void checkOid4VCIEnabled() {
        if (!Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI)) {
            throw ErrorResponse.error("Feature " + Profile.Feature.OID4VC_VCI.getKey() + " not enabled", Response.Status.BAD_REQUEST);
        }
        if (!realm.isVerifiableCredentialsEnabled()) {
            throw ErrorResponse.error("Verifiable credentials not enabled for the realm", Response.Status.BAD_REQUEST);
        }
    }

}
