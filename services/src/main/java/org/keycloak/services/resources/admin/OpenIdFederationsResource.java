package org.keycloak.services.resources.admin;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OpenIdFederationConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.OpenIdFederationRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.storage.datastore.DefaultExportImportManager;

import java.util.List;
import java.util.stream.Collectors;

public class OpenIdFederationsResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    protected static final Logger logger = Logger.getLogger(OpenIdFederationsResource.class);

    public OpenIdFederationsResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.OPENID_FEDERATION);
    }


    /**
     * Get a list with all OpenId Federationscd .. of the realm
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get OpenId Federation List")
    public List<OpenIdFederationRepresentation> list() {
        this.auth.realm().requireViewRealm();
        return realm.getOpenIdFederations().stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
    }

    /**
     * Create a new OpenId Federation
     *
     * @param representation JSON body
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new OpenId Federation")
    public Response create(OpenIdFederationRepresentation representation) {
        this.auth.realm().requireManageRealm();

        OpenIdFederationConfig model = DefaultExportImportManager.toModel(representation);
        realm.addOpenIdFederation(model);

        representation.setInternalId(model.getInternalId());
        adminEvent.operation(OperationType.CREATE)
                .resourcePath(session.getContext().getUri(), representation.getInternalId())
                .representation(representation).success();

        return Response.created(session.getContext().getUri().getAbsolutePathBuilder()
                .path(representation.getInternalId()).build()).build();

    }

    /**
     * @param internalId
     * @return
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get an OpenId Federation")
    public OpenIdFederationRepresentation getOpenIdFederation(@PathParam("id") String internalId) {
        this.auth.realm().requireViewIdentityProviders();

        OpenIdFederationConfig config = realm.getOpenIdFederations().stream().filter(x -> internalId.equals(x.getInternalId())).findAny().orElseThrow(NotFoundException::new);
        return ModelToRepresentation.toRepresentation(config);
    }

    /**
     * Update the OpenId Federation
     *
     * @param representation
     * @return
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation(summary = "Update the OpenId Federation")
    public Response update(@PathParam("id") String internalId, OpenIdFederationRepresentation representation) {
        this.auth.realm().requireManageRealm();

        representation.setInternalId(internalId);
        OpenIdFederationConfig config = DefaultExportImportManager.toModel(representation);

        try {
            realm.updateOpenIdFederation(config);
            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(representation).success();
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("OpenId Federation with trust anchor " + representation.getTrustAnchor() + " already exists");
        }
    }

    /**
     * Delete an OpenId Federation
     *
     * @return
     */
    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Operation(summary = "Delete an OpenId Federation")
    public Response delete(@PathParam("id") String internalId) {
        this.auth.realm().requireManageRealm();

        realm.removeOpenIdFederation(internalId);

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
        return Response.noContent().build();
    }

}
