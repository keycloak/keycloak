package org.keycloak.admin.ui.rest

import org.keycloak.admin.ui.rest.model.ClientRole
import org.keycloak.admin.ui.rest.model.RoleMapper
import org.keycloak.models.RealmModel
import org.keycloak.models.RoleModel
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator
import org.mapstruct.factory.Mappers
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

abstract class RoleMappingResource(
    private var realm: RealmModel,
    private var auth: AdminPermissionEvaluator,
) {

    fun mapping(
        predicate: Predicate<RoleModel?>,
    ): Stream<ClientRole> {
        val mapper = Mappers.getMapper(RoleMapper::class.java)
        return realm.clientsStream
            .flatMap { c -> c.rolesStream }
            .filter(predicate)
            .filter(auth.roles()::canMapClientScope)

            .map { r -> mapper.convertToRepresentation(r, realm.clientsStream) }
    }

    fun mapping(
        predicate: Predicate<RoleModel?>,
        first: Long,
        max: Long,
        search: String
    ): List<ClientRole> {
        return mapping(predicate)
            .filter { r -> r.client!!.contains(search, true) || r.role.contains(search, true) }

            .skip(if (search.isBlank()) first else 0)
            .limit(max)
            .collect(Collectors.toList()) ?: Collections.emptyList()
    }

}