package org.keycloak.admin.ui.rest

import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.keycloak.admin.ui.rest.model.ClientRole
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator
import java.util.stream.Collectors
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path("/")
open class EffectiveRoleMappingResource(
    private var realm: RealmModel,
    private var auth: AdminPermissionEvaluator,
) : RoleMappingResource(realm, auth) {
    @Context
    var session: KeycloakSession? = null

    @GET
    @Path("/clientScopes/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List all effective roles for this client scope",
        description = "This endpoint returns all the client role mapping for a specific client scope"
    )
    @APIResponse(
        responseCode = "200",
        description = "",
        content = [Content(
            schema = Schema(
                type = SchemaType.ARRAY,
                implementation = ClientRole::class
            )
        )]
    )
    fun listCompositeClientScopeRoleMappings(
        @PathParam("id") id: String,
    ): List<ClientRole> {
        val scopeContainer = realm.getClientScopeById(id) ?: throw NotFoundException("Could not find client scope")
        auth.clients().requireView(scopeContainer)

        return mapping(scopeContainer::hasScope).collect(Collectors.toList())
    }

    @GET
    @Path("/clients/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List all effective roles for this client",
        description = "This endpoint returns all the client role mapping for a specific client"
    )
    @APIResponse(
        responseCode = "200",
        description = "",
        content = [Content(
            schema = Schema(
                type = SchemaType.ARRAY,
                implementation = ClientRole::class
            )
        )]
    )
    fun listCompositeClientsRoleMappings(
        @PathParam("id") id: String,
    ): List<ClientRole> {
        val scopeContainer = realm.getClientById(id) ?: throw NotFoundException("Could not find client")
        auth.clients().requireView(scopeContainer)

        return mapping(scopeContainer::hasScope).collect(Collectors.toList())
    }

    @GET
    @Path("/groups/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List all effective roles for this group",
        description = "This endpoint returns all the client role mapping for a specific group"
    )
    @APIResponse(
        responseCode = "200",
        description = "",
        content = [Content(
            schema = Schema(
                type = SchemaType.ARRAY,
                implementation = ClientRole::class
            )
        )]
    )
    fun listCompositeGroupsRoleMappings(
        @PathParam("id") id: String,
    ): List<ClientRole> {
        val scopeContainer = realm.getGroupById(id) ?: throw NotFoundException("Could not find group")

        return mapping(scopeContainer::hasDirectRole).collect(Collectors.toList())
    }

    @GET
    @Path("/users/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List all effective roles for this users",
        description = "This endpoint returns all the client role mapping for a specific users"
    )
    @APIResponse(
        responseCode = "200",
        description = "",
        content = [Content(
            schema = Schema(
                type = SchemaType.ARRAY,
                implementation = ClientRole::class
            )
        )]
    )
    fun listCompositeUsersRoleMappings(
        @PathParam("id") id: String,
    ): List<ClientRole> {
        val user = session?.users()?.getUserById(realm, id)
            ?: if (auth.users().canQuery()) throw NotFoundException("User not found") else throw ForbiddenException()
        auth.users().requireView(user)

        return mapping(user::hasDirectRole).collect(Collectors.toList())
    }

    @GET
    @Path("/roles/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List all effective roles for this realm role",
        description = "This endpoint returns all the client role mapping for a specific realm role"
    )
    @APIResponse(
        responseCode = "200",
        description = "",
        content = [Content(
            schema = Schema(
                type = SchemaType.ARRAY,
                implementation = ClientRole::class
            )
        )]
    )
    fun listCompositeRealmRoleMappings(
    ): List<ClientRole> {
        return mapping { true }.collect(Collectors.toList())
    }

}