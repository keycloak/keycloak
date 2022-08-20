package org.keycloak.admin.ui.rest.model

import org.eclipse.microprofile.openapi.annotations.media.Schema

data class ClientRole(
    @field:Schema(required = true) var id: String,
    @field:Schema(required = true) var role: String,
    @field:Schema(required = true) var client: String?,
    var description: String?
) {
}