package org.keycloak.protocol.oid4vc.resources.admin;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserVerifiableCredentialModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oid4vc.issuance.requiredactions.CredentialOfferActionToken;
import org.keycloak.protocol.oid4vc.utils.CredentialScopeUtils;
import org.keycloak.protocol.oid4vc.utils.OID4VCUtil;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;
import org.keycloak.representations.idm.oid4vc.VerifiableCredentialOfferActionConfig;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import static org.keycloak.services.resources.admin.UserResource.verifySendEmailParams;

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

        CredentialScopeModel credentialScope = checkCredentialScope(representation.getCredentialScopeName());

        try {
            UserVerifiableCredentialModel modelToCreate = RepresentationToModel.toModel(representation, realm);
            UserVerifiableCredentialModel createdModel = session.users().addVerifiableCredential(user.getId(), modelToCreate);

            UserVerifiableCredentialRepresentation createdRep = ModelToRepresentation.toRepresentation(createdModel, credentialScope.getName());
            createdRep.setCredentialConfigurationId(credentialScope.getCredentialConfigurationId());
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
            @APIResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorRepresentation.class))),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public List<UserVerifiableCredentialRepresentation> getCredentials() {
        auth.users().requireView(user);
        checkOid4VCIEnabled();

        return session.users().getVerifiableCredentialsByUser(user.getId())
                .map(model -> ModelToRepresentation.toRepresentation(model, realm))
                .toList();
    }

    @PUT
    @Path("credentials/{credentialScopeName}")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(summary = "Update verifiable credential - refreshes user attributes snapshot and increments revision")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserVerifiableCredentialRepresentation.class))),
            @APIResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorRepresentation.class))),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public UserVerifiableCredentialRepresentation updateCredential(@PathParam("credentialScopeName") String credentialScopeName) {
        auth.users().requireManage(user);
        checkOid4VCIEnabled();

        // Resolve name to ID
        ClientScopeModel clientScope = realm.getClientScopesStream()
                .filter(s -> s.getName().equals(credentialScopeName))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Client scope not found: " + credentialScopeName));

        try {
            UserVerifiableCredentialModel updatedModel = session.users().updateVerifiableCredential(user.getId(), clientScope.getId());

            UserVerifiableCredentialRepresentation updatedRep = ModelToRepresentation.toRepresentation(updatedModel, realm);

            CredentialScopeModel credentialScope = new CredentialScopeModel(clientScope);
            updatedRep.setCredentialConfigurationId(credentialScope.getCredentialConfigurationId());

            adminEvent.operation(OperationType.UPDATE)
                    .resourcePath(session.getContext().getUri(), credentialScopeName)
                    .representation(updatedRep)
                    .success();

            return updatedRep;

        } catch (ModelException e) {
            if(e.getMessage() != null && e.getMessage().contains("concurrently modified")) {
                logger.warn(String.format("Concurrent update detected for verifiable credential '%s' for user '%s' in realm '%s'.",
                        credentialScopeName, user.getUsername(), realm.getName()));
                throw ErrorResponse.error("The verifiable credential was modified by another request. Please retry.", Response.Status.CONFLICT);
            }
            logger.warn(String.format("Verifiable credential '%s' not found for user '%s' in the realm '%s'.",
                    credentialScopeName, user.getUsername(), realm.getName()));
            throw new NotFoundException("Verifiable credential not found");
        }
    }

    @DELETE
    @Path("credentials/{credentialScopeName}")
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(summary = "Revoke verifiable credential for particular user")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void revokeCredential(@PathParam("credentialScopeName") String credentialScopeName) {
        auth.users().requireManage(user);
        checkOid4VCIEnabled();

        // Resolve name to ID
        ClientScopeModel clientScope = realm.getClientScopesStream()
                .filter(s -> s.getName().equals(credentialScopeName))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Client scope not found: " + credentialScopeName));

        boolean removed = session.users().removeVerifiableCredential(user.getId(), clientScope.getId());
        if (!removed) {
            logger.warn(String.format("Verifiable credential '%s' not found for user '%s' in the realm '%s'.",
                    credentialScopeName, user.getUsername(), realm.getName()));
            throw new NotFoundException("Verifiable credential not found");
        }

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }

    @GET
    @Path("issued-credentials")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(summary = "Get issued verifiable credentials for the user")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public List<IssuedVerifiableCredentialRepresentation> getIssuedCredentials() {
        auth.users().requireView(user);
        checkOid4VCIEnabled();

        return session.users().getIssuedVerifiableCredentialsStreamByUser(user.getId())
                .map(model -> ModelToRepresentation.toRepresentation(model, session, realm))
                .toList();
    }

    @DELETE
    @Path("issued-credentials/{id}")
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(summary = "Revoke an issued verifiable credential")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void revokeIssuedCredential(@PathParam("id") String credentialId) {
        auth.users().requireManage(user);
        checkOid4VCIEnabled();

        boolean removed = session.users().removeIssuedVerifiableCredential(credentialId);
        if (!removed) {
            logger.warn(String.format("Issued verifiable credential with ID '%s' not found for user '%s' in realm '%s'.",
                    credentialId, user.getUsername(), realm.getName()));
            throw new NotFoundException("Issued verifiable credential not found");
        }

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }

    @PUT
    @Path("credentials/send-credential-offer")
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(summary = "Send credential offer of specified verifiable credential to this user by email. An email contains a link the user can click " +
                         "to see the page with credential offer, from which he can obtain verifiable credential to his wallet.")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorRepresentation.class))),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "404", description = "Not Found"),
            @APIResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorRepresentation.class)))
    })
    public Response sendCredentialOffer(@Parameter(description = "Client id. Optional parameter. If it is set, then once user clicks on 'Continue' button from credential offer page (which is displayed to him after he clicks on the link from the email), the Base URL of this client might be displayed, which means user is guided to be redirected to that specified client application") @QueryParam("client_id") String clientId,
                                    @Parameter(description = "Redirect uri. Optional parameter. If it is set, it needs to be valid redirect URI for the client specified by 'client ID' parameter. It allows to use different URL than client base URL on the screen, which is displayed to the user after continue from credential offer page.") @QueryParam("redirect_uri") String redirectUri,
                                    @Parameter(description = "Number of seconds after which the generated token expires. If not set, the default value is realm option 'Default Admin-Initiated Action Lifespan', which defaults to 12 hours.") @QueryParam("lifespan") Integer lifespan,
                                    @Parameter(description = "Configuration of the requested credential offer. This is required parameter, but only credentialConfigurationId needs to be filled inside this offer") VerifiableCredentialOfferActionConfig credentialOfferConfig) {
        auth.users().requireManage(user);
        checkOid4VCIEnabled();

        UserResource.SendEmailParams result = verifySendEmailParams(session, realm, user, redirectUri, clientId, lifespan);

        // Additional configuration verifications
        if (credentialOfferConfig == null) {
            throw ErrorResponse.error("Credential offer configuration missing", Response.Status.BAD_REQUEST);
        }
        if (credentialOfferConfig.getCredentialConfigurationId() == null) {
            logger.warnf("Credential configuration ID was missing. KC action parameter value was: %s", credentialOfferConfig);
            throw ErrorResponse.error("Credential configuration ID was missing", Response.Status.BAD_REQUEST);
        }
        String credentialConfigId = credentialOfferConfig.getCredentialConfigurationId();
        CredentialScopeModel credScope = CredentialScopeUtils.findCredentialScopeModelByConfigurationId(
                realm, () -> session.clientScopes().getClientScopesStream(realm), credentialConfigId);
        if (credScope == null) {
            logger.warnf("Client scope was not found for credential configuration ID: %s", credentialConfigId);
            throw ErrorResponse.error("Client scope was not found for specified credential configuration ID", Response.Status.BAD_REQUEST);
        }
        if (!OID4VCUtil.hasVerifiableCredential(session, user, credScope)) {
            logger.warnf("User '%s' in the realm '%s' does not have requested credential scope '%s'", user.getUsername(), realm.getName(), credScope.getName());
            throw ErrorResponse.error("User does not have requested credential scope", Response.Status.BAD_REQUEST);
        }

        int expiration = Time.currentTime() + result.getLifespan();
        CredentialOfferActionToken token = new CredentialOfferActionToken(user.getId(), user.getEmail(), expiration, credentialOfferConfig, result.getRedirectUri(), result.getClientId());

        try {
            UriBuilder builder = LoginActionsService.actionTokenProcessor(session.getContext().getUri());
            builder.queryParam("key", token.serialize(session, realm, session.getContext().getUri()));

            String link = builder.build(realm.getName()).toString();
            String credentialDisplayName = CredentialScopeUtils.getCredentialDisplayName(session, user, credScope);

            this.session.getProvider(EmailTemplateProvider.class)
                    .setAttribute(OID4VCIConstants.EMAIL_TEMPLATE_ATTR_CREDENTIAL_SCOPE_DISPLAY_NAME, credentialDisplayName)
                    .setAttribute(Constants.IGNORE_ACCEPT_LANGUAGE_HEADER, true)
                    .setRealm(realm)
                    .setUser(user)
                    .sendVerifiableCredentialOffer(link, TimeUnit.SECONDS.toMinutes(result.getLifespan()));

            adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();

            return Response.noContent().build();
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendActionsEmail(e);
            throw ErrorResponse.error("Failed to send email for credential offer: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private void checkOid4VCIEnabled() {
        if (!Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI)) {
            throw ErrorResponse.error("Feature " + Profile.Feature.OID4VC_VCI.getKey() + " not enabled", Response.Status.BAD_REQUEST);
        }
        if (!realm.isVerifiableCredentialsEnabled()) {
            throw ErrorResponse.error("Verifiable credentials not enabled for the realm", Response.Status.BAD_REQUEST);
        }
    }

    private CredentialScopeModel checkCredentialScope(String credentialScopeName) {
        ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(realm, credentialScopeName);
        if (clientScope == null) {
            logger.warn(String.format("Client scope '%s' does not exists in the realm realm '%s'.", credentialScopeName,realm.getName()));
            throw ErrorResponse.error("Client scope does not exists", Response.Status.BAD_REQUEST);
        }
        if (!OID4VCIConstants.OID4VC_PROTOCOL.equals(clientScope.getProtocol())) {
            logger.warn(String.format("Client scope '%s' in the realm realm '%s' does not have protocol '%s'.",
                    credentialScopeName,realm.getName(), OID4VCIConstants.OID4VC_PROTOCOL));
            throw ErrorResponse.error("Client scope has incorrect protocol", Response.Status.BAD_REQUEST);
        }
        return new CredentialScopeModel(clientScope);
    }

}
