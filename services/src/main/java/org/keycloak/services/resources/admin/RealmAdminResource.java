/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services.resources.admin;

import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;

import org.keycloak.Config;
import org.keycloak.KeyPairVerifier;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.client.clienttype.ClientTypeManager;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.email.EmailAuthenticator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.exportimport.ClientDescriptionConverter;
import org.keycloak.exportimport.ClientDescriptionConverterFactory;
import org.keycloak.exportimport.ExportAdapter;
import org.keycloak.exportimport.ExportOptions;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.organization.admin.resource.OrganizationsResource;
import org.keycloak.partialimport.PartialImportResult;
import org.keycloak.partialimport.PartialImportResults;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissionManagement;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.services.util.DateUtil;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.ExportImportManager;
import org.keycloak.storage.StoreSyncEvent;
import org.keycloak.utils.GroupUtils;
import org.keycloak.utils.ProfileHelper;
import org.keycloak.utils.ReservedCharValidator;
import org.keycloak.utils.SMTPUtil;
import org.keycloak.workflow.admin.resource.WorkflowsResource;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import static org.keycloak.util.JsonSerialization.readValue;

/**
 * Base resource class for the admin REST api of one realm
 *
 * @resource Realms Admin
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class RealmAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);

    protected final AdminPermissionEvaluator auth;
    protected final RealmModel realm;
    private final AdminEventBuilder adminEvent;

    protected final KeycloakSession session;

    protected final ClientConnection connection;

    protected final HttpHeaders headers;

    public RealmAdminResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.auth = auth;
        this.realm = session.getContext().getRealm();
        this.connection = session.getContext().getConnection();
        this.adminEvent = adminEvent.resource(ResourceType.REALM);
        this.headers = session.getContext().getRequestHeaders();
    }

    /**
     * Base path for importing clients under this realm.
     *
     * @return
     */
    @Path("client-description-converter")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Base path for importing clients under this realm.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ClientRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public ClientRepresentation convertClientDescription(String description) {
        auth.clients().requireManage();

        if (realm == null) {
            throw new NotFoundException("Realm not found.");
        }

        return session.getKeycloakSessionFactory().getProviderFactoriesStream(ClientDescriptionConverter.class)
                .map(ClientDescriptionConverterFactory.class::cast)
                .filter(factory -> factory.isSupported(description))
                .map(factory -> factory.create(session).convertToInternal(description))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported format"));
    }

    /**
     * Base path for managing attack detection.
     *
     * @return
     */
    @Path("attack-detection")
    public AttackDetectionResource getAttackDetection() {
        return new AttackDetectionResource(session, auth, adminEvent);
    }

    /**
     * Base path for managing clients under this realm.
     *
     * @return
     */
    @Path("clients")
    public ClientsResource getClients() {
        return new ClientsResource(session, auth, adminEvent);
    }

    /**
     * This endpoint is deprecated. It's here just because of backwards compatibility. Use {@link #getClientScopes()} instead
     *
     * @return
     */
    @Deprecated
    @Path("client-templates")
    public ClientScopesResource getClientTemplates() {
        return getClientScopes();
    }

    /**
     * Base path for managing client scopes under this realm.
     *
     * @return
     */
    @Path("client-scopes")
    public ClientScopesResource getClientScopes() {
        return new ClientScopesResource(session, auth, adminEvent);
    }

    /**
     * Base path for managing localization under this realm.
     */
    @Path("localization")
    public RealmLocalizationResource getLocalization() {
        return new RealmLocalizationResource(session, auth);
    }

    /**
     * Get realm default client scopes.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-default-client-scopes")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Get realm default client scopes. Only name and ids are returned.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ClientScopeRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<ClientScopeRepresentation> getDefaultDefaultClientScopes() {
        return getDefaultClientScopes(true);
    }

    private Stream<ClientScopeRepresentation> getDefaultClientScopes(boolean defaultScope) {
        auth.clients().requireViewClientScopes();

        return realm.getDefaultClientScopesStream(defaultScope).map(clientScope -> {
            ClientScopeRepresentation rep = new ClientScopeRepresentation();
            rep.setId(clientScope.getId());
            rep.setName(clientScope.getName());
            rep.setProtocol(clientScope.getProtocol());
            return rep;
        });
    }

    @PUT
    @NoCache
    @Path("default-default-client-scopes/{clientScopeId}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void addDefaultDefaultClientScope(@PathParam("clientScopeId") String clientScopeId) {
        addDefaultClientScope(clientScopeId,true);
    }

    private void addDefaultClientScope(String clientScopeId, boolean defaultScope) {
        auth.clients().requireManageClientScopes();

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);
        if (clientScope == null) {
            throw new NotFoundException("Client scope not found");
        }
        realm.addDefaultClientScope(clientScope, defaultScope);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.CLIENT_SCOPE).resourcePath(session.getContext().getUri()).success();
    }

    @DELETE
    @NoCache
    @Path("default-default-client-scopes/{clientScopeId}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void removeDefaultDefaultClientScope(@PathParam("clientScopeId") String clientScopeId) {
        auth.clients().requireManageClientScopes();

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);
        if (clientScope == null) {
            throw new NotFoundException("Client scope not found");
        }
        realm.removeDefaultClientScope(clientScope);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.CLIENT_SCOPE).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Get realm optional client scopes.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-optional-client-scopes")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Get realm optional client scopes. Only name and ids are returned.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ClientScopeRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<ClientScopeRepresentation> getDefaultOptionalClientScopes() {
        return getDefaultClientScopes(false);
    }

    @PUT
    @NoCache
    @Path("default-optional-client-scopes/{clientScopeId}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void addDefaultOptionalClientScope(@PathParam("clientScopeId") String clientScopeId) {
        addDefaultClientScope(clientScopeId, false);
    }

    @DELETE
    @NoCache
    @Path("default-optional-client-scopes/{clientScopeId}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void removeDefaultOptionalClientScope(@PathParam("clientScopeId") String clientScopeId) {
        removeDefaultDefaultClientScope(clientScopeId);
    }

    /**
     * Base path for managing client initial access tokens
     *
     * @return
     */
    @Path("clients-initial-access")
    public ClientInitialAccessResource getClientInitialAccess() {
        return new ClientInitialAccessResource(session, auth, adminEvent);
    }

    @Path("client-registration-policy")
    public ClientRegistrationPolicyResource getClientRegistrationPolicy() {
        return new ClientRegistrationPolicyResource(session, auth, adminEvent);
    }

    /**
     * Base path for managing components under this realm.
     *
     * @return
     */
    @Path("components")
    public ComponentResource getComponents() {
        return new ComponentResource(session, auth, adminEvent);
    }

    /**
     * base path for managing realm-level roles of this realm
     *
     * @return
     */
    @Path("roles")
    public RoleContainerResource getRoleContainerResource() {
        return new RoleContainerResource(session, session.getContext().getUri(), realm, auth, realm, adminEvent);
    }

    /**
     * Get the top-level representation of the realm
     *
     * It will not include nested information like User and Client representations.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Get the top-level representation of the realm It will not include nested information like User and Client representations.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RealmRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public RealmRepresentation getRealm() {
        if (auth.realm().canViewRealm()) {
            return ModelToRepresentation.toRepresentation(session, realm, false);
        } else {
            auth.realm().requireViewRealmNameList();

            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realm.getName());
            rep.setDefaultLocale(realm.getDefaultLocale());
            rep.setDisplayName(realm.getDisplayName());
            rep.setDisplayNameHtml(realm.getDisplayNameHtml());
            rep.setSupportedLocales(realm.getSupportedLocalesStream().collect(Collectors.toSet()));
            rep.setBruteForceProtected(realm.isBruteForceProtected());

            if (auth.users().canView()) {
                rep.setRegistrationEmailAsUsername(realm.isRegistrationEmailAsUsername());
            }

            return rep;
        }
    }

    /**
     * Update the top-level information of the realm
     *
     * Any user, roles or client information in the representation
     * will be ignored.  This will only update top-level attributes of the realm.
     *
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Update the top-level information of the realm Any user, roles or client information in the representation will be ignored.",
            description = "This will only update top-level attributes of the realm.")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found"),
        @APIResponse(responseCode = "409", description = "Conflict"),
        @APIResponse(responseCode = "500", description = "Internal Server Error")
    })
    public Response updateRealm(final RealmRepresentation rep) {
        auth.realm().requireManageRealm();

        logger.debugf("updating realm: %s", realm.getName());

        if (Config.getAdminRealm().equals(realm.getName()) && (rep.getRealm() != null && !rep.getRealm().equals(Config.getAdminRealm()))) {
            throw ErrorResponse.error("Can't rename master realm", Status.BAD_REQUEST);
        }

        try {
            ReservedCharValidator.validate(rep.getRealm());
            ReservedCharValidator.validateLocales(rep.getSupportedLocales());
            ReservedCharValidator.validateSecurityHeaders(rep.getBrowserSecurityHeaders());
        } catch (ReservedCharValidator.ReservedCharException e) {
            logger.error(e.getMessage(), e);
            throw ErrorResponse.error(e.getMessage(), Status.BAD_REQUEST);
        }

        try {
            SMTPUtil.checkSMTPConfiguration(session, rep.getSmtpServer());
        } catch (EmailException e) {
            logger.error(e.getMessage(), e);
            throw ErrorResponse.error(e.getMessage(), Status.BAD_REQUEST);
        }

        try {
            if (!Constants.GENERATE.equals(rep.getPublicKey()) && (rep.getPrivateKey() != null && rep.getPublicKey() != null)) {
                try {
                    KeyPairVerifier.verify(rep.getPrivateKey(), rep.getPublicKey());
                } catch (VerificationException e) {
                    throw ErrorResponse.error(e.getMessage(), Status.BAD_REQUEST);
                }
            }

            if (!Constants.GENERATE.equals(rep.getPublicKey()) && (rep.getCertificate() != null)) {
                try {
                    X509Certificate cert = PemUtils.decodeCertificate(rep.getCertificate());
                    if (cert == null) {
                        throw ErrorResponse.error("Failed to decode certificate", Status.BAD_REQUEST);
                    }
                } catch (Exception e)  {
                    throw ErrorResponse.error("Failed to decode certificate", Status.BAD_REQUEST);
                }
            }

            if (rep.getAccessCodeLifespanLogin() != null && rep.getAccessCodeLifespanUserAction() != null) {
                if (rep.getAccessCodeLifespanLogin() < 1 || rep.getAccessCodeLifespanUserAction() < 1) {
                    throw ErrorResponse.error("AccessCodeLifespanLogin or AccessCodeLifespanUserAction cannot be 0", Status.BAD_REQUEST);
                }
            }

            RepresentationToModel.updateRealm(rep, realm, session);

            // Refresh periodic sync tasks for configured federationProviders
            StoreSyncEvent.fire(session, realm, false);

            // This populates the map in DefaultKeycloakContext to be used when treating the event
            session.getContext().getUri();

            adminEvent.operation(OperationType.UPDATE).representation(rep).success();

            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Realm with same name exists");
        } catch (ModelIllegalStateException e) {
            logger.error(e.getMessage(), e);
            throw ErrorResponse.error(e.getMessage(), Status.INTERNAL_SERVER_ERROR);
        } catch (ModelException e) {
            throw ErrorResponse.error(e.getMessage(), Status.BAD_REQUEST);
        } catch (org.keycloak.services.ErrorResponseException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw ErrorResponse.error("Failed to update realm", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete the realm
     *
     */
    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Delete the realm")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void deleteRealm() {
        auth.realm().requireManageRealm();

        if (Config.getAdminRealm().equals(realm.getName())) {
            throw ErrorResponse.error("Can't remove master realm", Status.BAD_REQUEST);
        }

        if (!new RealmManager(session).removeRealm(realm)) {
            throw new NotFoundException("Realm doesn't exist");
        }

        // The delete event is associated with the realm of the user executing the operation,
        // instead of the realm being deleted.
        AdminEventBuilder deleteAdminEvent = new AdminEventBuilder(auth.adminAuth().getRealm(), auth.adminAuth(), session, connection);
        deleteAdminEvent.operation(OperationType.DELETE).resource(ResourceType.REALM)
                .realm(auth.adminAuth().getRealm()).resourcePath(realm.getName()).success();
    }

    /**
     * Base path for managing users in this realm.
     *
     * @return
     */
    @Path("users")
    public UsersResource users() {
        return new UsersResource(session, auth, adminEvent);
    }

    @NoCache
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users-management-permissions")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ManagementPermissionReference.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public ManagementPermissionReference getUserMgmtPermissions() {
        ProfileHelper.requireFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        auth.realm().requireViewRealm();

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (permissions.users().isPermissionsEnabled()) {
            return toUsersMgmtRef(permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Path("users-management-permissions")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ManagementPermissionReference.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public ManagementPermissionReference setUsersManagementPermissionsEnabled(ManagementPermissionReference ref) {
        ProfileHelper.requireFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        auth.realm().requireManageRealm();

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        permissions.users().setPermissionsEnabled(ref.isEnabled());
        if (ref.isEnabled()) {
            return toUsersMgmtRef(permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }

    private ManagementPermissionReference toUsersMgmtRef(AdminPermissionManagement permissions) {
        ManagementPermissionReference ref = new ManagementPermissionReference();
        ref.setEnabled(true);
        ref.setResource(permissions.users().resource().getId());
        Map<String, String> scopes = permissions.users().getPermissions();
        ref.setScopePermissions(scopes);
        return ref;
    }

    @Path("organizations")
    public OrganizationsResource organizations() {
        return new OrganizationsResource(session, auth, adminEvent);
    }

    @Path("workflows")
    public WorkflowsResource workflows() {
        return new WorkflowsResource(session, auth);
    }

    @Path("{extension}")
    public Object extension(@PathParam("extension") String extension) {
        AdminRealmResourceProvider provider = session.getProvider(AdminRealmResourceProvider.class, extension);
        if (provider != null) {
            Object resource = provider.getResource(session, realm, auth, adminEvent);
            if (resource != null) {
                return resource;
            }
        }

        throw new NotFoundException();
    }

    @Path("authentication")
    public AuthenticationManagementResource flows() {
        return new AuthenticationManagementResource(session, auth, adminEvent);

    }

    /**
     * Path for managing all realm-level or client-level roles defined in this realm by its id.
     *
     * @return
     */
    @Path("roles-by-id")
    public RoleByIdResource rolesById() {
         return new RoleByIdResource(session, auth, adminEvent);
    }

    /**
     * Push the realm's revocation policy to any client that has an admin url associated with it.
     *
     */
    @Path("push-revocation")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Push the realm's revocation policy to any client that has an admin url associated with it.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = GlobalRequestResult.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public GlobalRequestResult pushRevocation() {
        auth.realm().requireManageRealm();

        GlobalRequestResult result = new ResourceAdminManager(session).pushRealmRevocationPolicy(realm);
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(result).success();
        return result;
    }

    /**
     * Removes all user sessions. Any client that has an admin url will also be told to invalidate any sessions
     * they have.
     *
     */
    @Path("logout-all")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Removes all user sessions.", description = "Any client that has an admin url will also be told to invalidate any sessions they have.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = GlobalRequestResult.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public GlobalRequestResult logoutAll() {
        auth.users().requireManage();

        session.sessions().removeUserSessions(realm);
        GlobalRequestResult result = new ResourceAdminManager(session).logoutAll(realm);
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(result).success();
        return result;
    }

    /**
     * Remove a specific user session. Any client that has an admin url will also be told to invalidate this
     * particular session.
     *
     * @param sessionId
     */
    @Path("sessions/{session}")
    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Remove a specific user session.", description = "Any client that has an admin url will also be told to invalidate this particular session.")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void deleteSession(@PathParam("session") String sessionId, @DefaultValue("false") @QueryParam("isOffline") boolean offline) {
        auth.users().requireManage();

        UserSessionModel userSession = offline ? session.sessions().getOfflineUserSession(realm, sessionId) : session.sessions().getUserSession(realm, sessionId);
        if (userSession == null) {
            throw new NotFoundException("Sesssion not found");
        }

        AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), connection, headers, true);

        Map<String, Object> eventRep = new HashMap<>();
        eventRep.put("offline", offline);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.USER_SESSION).resourcePath(session.getContext().getUri()).representation(eventRep).success();
    }

    /**
     * Get client session stats
     *
     * Returns a JSON map.  The key is the client id, the value is the number of sessions that currently are active
     * with that client.  Only clients that actually have a session associated with them will be in this map.
     *
     * @return
     */
    @Path("client-session-stats")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Get client session stats Returns a JSON map.",
        description = "The key is the client id, the value is the number of sessions that currently are active with that client. Only clients that actually have a session associated with them will be in this map.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<Map<String, String>> getClientSessionStats() {
        auth.realm().requireViewRealm();

        Map<String, Map<String, String>> data = new HashMap<>();
        {
            Map<String, Long> activeCount = session.sessions().getActiveClientSessionStats(realm, false);
            for (Map.Entry<String, Long> entry : activeCount.entrySet()) {
                Map<String, String> map = new HashMap<>();
                ClientModel client = realm.getClientById(entry.getKey());
                if (client == null)
                    continue;
                map.put("id", client.getId());
                map.put("clientId", client.getClientId());
                map.put("active", entry.getValue().toString());
                map.put("offline", "0");
                data.put(client.getId(), map);
            }
        }
        {
            Map<String, Long> offlineCount = session.sessions().getActiveClientSessionStats(realm, true);
            for (Map.Entry<String, Long> entry : offlineCount.entrySet()) {
                Map<String, String> map = data.get(entry.getKey());
                if (map == null) {
                    map = new HashMap<>();
                    ClientModel client = realm.getClientById(entry.getKey());
                    if (client == null)
                        continue;
                    map.put("id", client.getId());
                    map.put("clientId", client.getClientId());
                    map.put("active", "0");
                    data.put(client.getId(), map);
                }
                map.put("offline", entry.getValue().toString());
            }
        }

        return data.values().stream();
    }

    /**
     * Get the events provider configuration
     *
     * Returns JSON object with events provider configuration
     *
     * @return
     */
    @GET
    @NoCache
    @Path("events/config")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Get the events provider configuration Returns JSON object with events provider configuration")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RealmEventsConfigRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public RealmEventsConfigRepresentation getRealmEventsConfig() {
        auth.realm().requireViewEvents();

        RealmEventsConfigRepresentation config = ModelToRepresentation.toEventsConfigReprensetation(realm);
        if (config.getEnabledEventTypes() == null || config.getEnabledEventTypes().isEmpty()) {
            List<String> eventTypes = Arrays.stream(EventType.values())
                    .filter(EventType::isSaveByDefault)
                    .map(EventType::name)
                    .collect(Collectors.toList());
            config.setEnabledEventTypes(eventTypes);
        }
        return config;
    }

    /**
     * Update the events provider
     *
     * Change the events provider and/or its configuration
     *
     * @param rep
     */
    @PUT
    @Path("events/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation( description = "Update the events provider Change the events provider and/or its configuration")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public void updateRealmEventsConfig(final RealmEventsConfigRepresentation rep) {
        auth.realm().requireManageEvents();

        logger.debugf("updating realm events config: %s", realm.getName());
        new RealmManager(session).updateRealmEventsConfig(rep, realm);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.REALM)
                .resourcePath(session.getContext().getUri()).representation(rep)
                // refresh the builder to consider old and new config
                .refreshRealmEventsConfig(session)
                .success();
    }

    /**
     * Get events
     *
     * Returns all events, or filters them based on URL query parameters listed here
     *
     * @param types The types of events to return
     * @param client App or oauth client name
     * @param user User id
     * @param ipAddress IP address
     * @param dateTo To date
     * @param dateFrom From date
     * @param firstResult Paging offset
     * @param maxResults Maximum results size (defaults to 100)
     * @param direction The direction to sort events by (asc or desc)
     * @return
     */
    @Path("events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Get events Returns all events, or filters them based on URL query parameters listed here")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EventRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<EventRepresentation> getEvents(@Parameter(description = "The types of events to return") @QueryParam("type") List<String> types,
                                                 @Parameter(description = "App or oauth client name") @QueryParam("client") String client,
                                                 @Parameter(description = "User id") @QueryParam("user") String user,
                                                 @Parameter(description = "From (inclusive) date (yyyy-MM-dd) or time in Epoch timestamp millis (number of milliseconds since January 1, 1970, 00:00:00 GMT)") @QueryParam("dateFrom") String dateFrom,
                                                 @Parameter(description = "To (inclusive) date (yyyy-MM-dd) or time in Epoch timestamp millis (number of milliseconds since January 1, 1970, 00:00:00 GMT)") @QueryParam("dateTo") String dateTo,
                                                 @Parameter(description = "IP Address") @QueryParam("ipAddress") String ipAddress,
                                                 @Parameter(description = "Paging offset") @QueryParam("first") Integer firstResult,
                                                 @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults,
                                                 @Parameter(description = "The direction to sort events by (asc or desc)") @QueryParam("direction") String direction) {
        auth.realm().requireViewEvents();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery().realm(realm.getId());
        if (client != null) {
            query.client(client);
        }

        if (types != null && !types.isEmpty()) {
            EventType[] t = new EventType[types.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = EventType.valueOf(types.get(i));
            }
            query.type(t);
        }

        if (user != null) {
            query.user(user);
        }

        if(dateFrom != null) {
            try {
                query.fromDate(DateUtil.toStartOfDay(dateFrom));
            } catch (Throwable t) {
                throw new BadRequestException("Invalid value for 'dateFrom', expected format is yyyy-MM-dd or an Epoch timestamp");
            }
        }

        if(dateTo != null) {
            try {
                query.toDate(DateUtil.toEndOfDay(dateTo));
            } catch (Throwable t) {
                throw new BadRequestException("Invalid value for 'dateTo', expected format is yyyy-MM-dd or an Epoch timestamp");
            }
        }

        if (ipAddress != null) {
            query.ipAddress(ipAddress);
        }

        if (direction != null) {
            if ("asc".equals(direction)) {
                query.orderByAscTime();
            } else if ("desc".equals(direction)) {
                query.orderByDescTime();
            } else {
                throw new BadRequestException("Invalid value for sortDirection, expected value is asc or desc");
            }
        }

        if (firstResult != null) {
            query.firstResult(firstResult);
        }
        if (maxResults != null) {
            query.maxResults(maxResults);
        } else {
            query.maxResults(Constants.DEFAULT_MAX_RESULTS);
        }

        return query.getResultStream().map(ModelToRepresentation::toRepresentation);
    }

    /**
     * Get admin events
     *
     * Returns all admin events, or filters events based on URL query parameters listed here
     *
     * @param operationTypes
     * @param authRealm
     * @param authClient
     * @param authUser user id
     * @param authIpAddress
     * @param resourcePath
     * @param dateTo
     * @param dateFrom
     * @param firstResult
     * @param maxResults Maximum results size (defaults to 100)
     * @param resourceTypes
     * @param direction The direction to sort events by (asc or desc)
     * @return
     */
    @Path("admin-events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Get admin events Returns all admin events, or filters events based on URL query parameters listed here")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AdminEventRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<AdminEventRepresentation> getEvents(@QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
                                                    @Parameter(description = "user id") @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
                                                    @QueryParam("resourcePath") String resourcePath,
                                                    @Parameter(description = "From (inclusive) date (yyyy-MM-dd) or time in Epoch timestamp millis (number of milliseconds since January 1, 1970, 00:00:00 GMT)") @QueryParam("dateFrom") String dateFrom,
                                                    @Parameter(description = "To (inclusive) date (yyyy-MM-dd) or time in Epoch timestamp millis (number of milliseconds since January 1, 1970, 00:00:00 GMT)") @QueryParam("dateTo") String dateTo,
                                                    @QueryParam("first") Integer firstResult,
                                                    @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults,
                                                    @QueryParam("resourceTypes") List<String> resourceTypes,
                                                    @Parameter(description = "The direction to sort events by (asc or desc)") @QueryParam("direction") String direction) {
        auth.realm().requireViewEvents();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        AdminEventQuery query = eventStore.createAdminQuery().realm(realm.getId());;

        if (authRealm != null) {
            query.authRealm(authRealm);
        }

        if (authClient != null) {
            query.authClient(authClient);
        }

        if (authUser != null) {
            query.authUser(authUser);
        }

        if (authIpAddress != null) {
            query.authIpAddress(authIpAddress);
        }

        if (resourcePath != null) {
            query.resourcePath(resourcePath);
        }

        if (operationTypes != null && !operationTypes.isEmpty()) {
            OperationType[] t = new OperationType[operationTypes.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = OperationType.valueOf(operationTypes.get(i));
            }
            query.operation(t);
        }

        if (resourceTypes != null && !resourceTypes.isEmpty()) {
            ResourceType[] t = new ResourceType[resourceTypes.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = ResourceType.valueOf(resourceTypes.get(i));
            }
            query.resourceType(t);
        }

        if(dateFrom != null) {
            try {
                query.fromTime(DateUtil.toStartOfDay(dateFrom));
            } catch (Throwable t) {
                throw new BadRequestException("Invalid value for 'dateFrom', expected format is yyyy-MM-dd or an Epoch timestamp");
            }
        }

        if(dateTo != null) {
            try {
                query.toTime(DateUtil.toEndOfDay(dateTo));
            } catch (Throwable t) {
                throw new BadRequestException("Invalid value for 'dateTo', expected format is yyyy-MM-dd or an Epoch timestamp");
            }
        }

        if (direction != null) {
            if ("asc".equals(direction)) {
                query.orderByAscTime();
            } else if ("desc".equals(direction)) {
                query.orderByDescTime();
            } else {
                throw new BadRequestException("Invalid value for sortDirection, expected value is asc or desc");
            }
        }

        if (firstResult != null) {
            query.firstResult(firstResult);
        }
        if (maxResults != null) {
            query.maxResults(maxResults);
        } else {
            query.maxResults(Constants.DEFAULT_MAX_RESULTS);
        }

        return query.getResultStream().map(ModelToRepresentation::toRepresentation);
    }

    /**
     * Delete all events
     *
     */
    @Path("events")
    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Delete all events")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public void clearEvents() {
        auth.realm().requireManageEvents();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clear(realm);
    }

    /**
     * Delete all admin events
     *
     */
    @Path("admin-events")
    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Delete all admin events")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public void clearAdminEvents() {
        auth.realm().requireManageEvents();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clearAdmin(realm);
    }

    /**
     * Test SMTP connection with current logged in user
     *
     * @param config SMTP server configuration
     * @return
     * @throws Exception
     */
    @Path("testSMTPConnection")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Deprecated
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Test SMTP connection with current logged in user")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "500", description = "Internal Server Error")
    })
    public Response testSMTPConnection(final @Parameter(description = "SMTP server configuration") @FormParam("config") String config) throws Exception {
        Map<String, String> settings = readValue(config, new TypeReference<Map<String, String>>() {
        });
        return testSMTPConnection(settings);
    }

    @Path("testSMTPConnection")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "500", description = "Internal Server Error")
    })
    public Response testSMTPConnection(Map<String, String> settings) throws Exception {
        auth.realm().requireManageRealm();
        try {
            UserModel user = auth.adminAuth().getUser();
            if (user.getEmail() == null) {
                throw ErrorResponse.error("Logged in user does not have an e-mail.", Response.Status.INTERNAL_SERVER_ERROR);
            }
            if (ComponentRepresentation.SECRET_VALUE.equals(settings.get("password"))
                    && reuseConfiguredAuthenticationForSmtp(settings, EmailAuthenticator.AuthenticatorType.BASIC)) {
                settings.put("password", realm.getSmtpConfig().get("password"));
            }
            if (ComponentRepresentation.SECRET_VALUE.equals(settings.get("authTokenClientSecret"))
                    && reuseConfiguredAuthenticationForSmtp(settings, EmailAuthenticator.AuthenticatorType.TOKEN)) {
                settings.put("authTokenClientSecret", realm.getSmtpConfig().get("authTokenClientSecret"));
            }
            session.getProvider(EmailTemplateProvider.class).sendSmtpTestEmail(settings, user);
        } catch (Exception e) {
            logger.errorf(e, "Failed to send email \n %s", e.getCause());
            throw ErrorResponse.error("Failed to send email", Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.noContent().build();
    }

    private boolean reuseConfiguredAuthenticationForSmtp(Map<String, String> settings, EmailAuthenticator.AuthenticatorType type) {
        // just reuse the configured authentication if the same authenticator, host, port and user are passed
        return Boolean.parseBoolean(settings.get("auth")) && Boolean.parseBoolean(realm.getSmtpConfig().get("auth"))
                && Optional.ofNullable(settings.get("authType")).orElse(EmailAuthenticator.AuthenticatorType.BASIC.name()).equalsIgnoreCase(type.name())
                && realm.getSmtpConfig().getOrDefault("authType", EmailAuthenticator.AuthenticatorType.BASIC.name()).equalsIgnoreCase(type.name())
                && Objects.equals(Optional.ofNullable(settings.get("host")).orElse(""), realm.getSmtpConfig().getOrDefault("host", ""))
                && Objects.equals(Optional.ofNullable(settings.get("port")).orElse("25"), realm.getSmtpConfig().getOrDefault("port", "25"))
                && Objects.equals(Optional.ofNullable(settings.get("user")).orElse(""), realm.getSmtpConfig().getOrDefault("user", ""));
    }

    @Path("identity-provider")
    public IdentityProvidersResource getIdentityProviderResource() {
        return new IdentityProvidersResource(realm, session, this.auth, adminEvent);
    }

    /**
     * Get group hierarchy.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-groups")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Get group hierarchy.  Only name and ids are returned.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = GroupRepresentation.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<GroupRepresentation> getDefaultGroups() {
        auth.realm().requireViewRealm();

        return realm.getDefaultGroupsStream().map(ModelToRepresentation::groupToBriefRepresentation);
    }

    @PUT
    @NoCache
    @Path("default-groups/{groupId}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void addDefaultGroup(@PathParam("groupId") String groupId) {
        auth.realm().requireManageRealm();

        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }

        realm.addDefaultGroup(group);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.GROUP).resourcePath(session.getContext().getUri()).success();
    }

    @DELETE
    @NoCache
    @Path("default-groups/{groupId}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public void removeDefaultGroup(@PathParam("groupId") String groupId) {
        auth.realm().requireManageRealm();

        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }

        realm.removeDefaultGroup(group);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.GROUP).resourcePath(session.getContext().getUri()).success();
    }

    @Path("groups")
    public GroupsResource getGroups() {
        return  new GroupsResource(realm, session, this.auth, adminEvent);
    }

    @GET
    @Path("group-by-path/{path: .*}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = GroupRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "404", description = "Not Found")
    })
    public GroupRepresentation getGroupByPath(@PathParam("path") String path) {
        GroupModel found = KeycloakModelUtils.findGroupByPath(session, realm, path);
        if (found == null) {
            throw new NotFoundException("Group path does not exist");

        }
        auth.groups().requireView(found);
        GroupRepresentation groupRep = ModelToRepresentation.toRepresentation(found, true);
        return GroupUtils.populateSubGroupCount(found, groupRep);
    }

    /**
     * Partial import from a JSON file to an existing realm.
     *
     */
    @Path("partialImport")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Partial import from a JSON file to an existing realm.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PartialImportResults.class))),
        @APIResponse(responseCode = "403", description = "Forbidden"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response partialImport(InputStream requestBody) {
        auth.realm().requireManageRealm();
        try {
            return Response.ok(
                    KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session.getContext(), kcSession -> {
                        RealmModel realmClone = kcSession.realms().getRealm(realm.getId());
                        AdminEventBuilder adminEventClone = adminEvent.clone(kcSession);
                        // calling a static method to avoid using the wrong instances
                        return getPartialImportResults(requestBody, kcSession, realmClone, adminEventClone);
                    }, "Partial import in realm " + realm.getName())
            ).build();
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists(e.getLocalizedMessage());
        }
    }

    private static PartialImportResults getPartialImportResults(InputStream requestBody, KeycloakSession kcSession, RealmModel kcRealm, AdminEventBuilder adminEventClone) {
        ExportImportManager exportProvider = kcSession.getProvider(DatastoreProvider.class).getExportImportManager();
        PartialImportResults results = exportProvider.partialImportRealm(kcRealm, requestBody);
        for (PartialImportResult result : results.getResults()) {
            switch (result.getAction()) {
                case ADDED : fireCreatedEvent(result, adminEventClone); break;
                case OVERWRITTEN: fireUpdateEvent(result, adminEventClone); break;
            }
        }
        return results;
    }

    private static void fireCreatedEvent(PartialImportResult result, AdminEventBuilder adminEvent) {
        adminEvent.operation(OperationType.CREATE)
                .resourcePath(result.getResourceType().getPath(), result.getId())
                .representation(result.getRepresentation())
                .success();
    };

    private static void fireUpdateEvent(PartialImportResult result, AdminEventBuilder adminEvent) {
        adminEvent.operation(OperationType.UPDATE)
                .resourcePath(result.getResourceType().getPath(), result.getId())
                .representation(result.getRepresentation())
                .success();
    }

    /**
     * Partial export of existing realm into a JSON file.
     *
     * @param exportGroupsAndRoles
     * @param exportClients
     * @return
     */
    @Path("partial-export")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation(summary = "Partial export of existing realm into a JSON file.")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RealmRepresentation.class))),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Response partialExport(@QueryParam("exportGroupsAndRoles") Boolean exportGroupsAndRoles,
                                  @QueryParam("exportClients") Boolean exportClients) {
        auth.realm().requireManageRealm();

        boolean groupsAndRolesExported = exportGroupsAndRoles != null && exportGroupsAndRoles;
        boolean clientsExported = exportClients != null && exportClients;

        if (groupsAndRolesExported) {
            auth.groups().requireList();
        }
        if (clientsExported) {
            auth.clients().requireView();
        }

        // service accounts are exported if the clients are exported
        // this means that if clients is true but groups/roles is false the service account is exported without roles
        // the other option is just include service accounts if clientsExported && groupsAndRolesExported
        ExportOptions options = new ExportOptions(false, clientsExported, groupsAndRolesExported, clientsExported, true);

        ExportImportManager exportProvider = session.getProvider(DatastoreProvider.class).getExportImportManager();

        Response.ResponseBuilder response = Response.ok();

        exportProvider.exportRealm(realm, options, new ExportAdapter() {
            @Override
            public void setType(String mediaType) {
                response.type(mediaType);
            }
            @Override
            public void writeToOutputStream(ConsumerOfOutputStream consumer) {
                response.entity((StreamingOutput) consumer::accept);
            }
        });
        return response.build();
    }

    @Path("keys")
    public KeyResource keys() {
        return new KeyResource(realm, session, this.auth);
    }

    @GET
    @Path("credential-registrators")
    @NoCache
    @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.REALMS_ADMIN)
    @Operation()
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "OK"),
        @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Stream<String> getCredentialRegistrators(){
        auth.realm().requireViewRealm();
        return session.getContext().getRealm().getRequiredActionProvidersStream()
                .filter(RequiredActionProviderModel::isEnabled)
                .map(RequiredActionProviderModel::getProviderId)
                .filter(providerId ->  session.getProvider(RequiredActionProvider.class, providerId) instanceof CredentialRegistrator);
    }

    @Path("client-policies/policies")
    public ClientPoliciesResource getClientPoliciesResource() {
        ProfileHelper.requireFeature(Profile.Feature.CLIENT_POLICIES);
        return new ClientPoliciesResource(session, auth);
    }

    @Path("client-policies/profiles")
    public ClientProfilesResource getClientProfilesResource() {
        ProfileHelper.requireFeature(Profile.Feature.CLIENT_POLICIES);
        return new ClientProfilesResource(session, auth);
    }

    @Path("client-types")
    public ClientTypesResource getClientTypesResource() {
        ProfileHelper.requireFeature(Profile.Feature.CLIENT_TYPES);
        return new ClientTypesResource(session.getProvider(ClientTypeManager.class), realm, auth);
    }

}
