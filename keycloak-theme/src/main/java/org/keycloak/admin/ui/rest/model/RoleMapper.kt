package org.keycloak.admin.ui.rest.model

import org.keycloak.models.ClientModel
import org.keycloak.models.RoleModel
import org.mapstruct.*
import java.util.stream.Stream

@Mapper
abstract class RoleMapper {

    @Mapping(source = "name", target = "role")
    @Mapping(target = "client", ignore = true)
    abstract fun convertToRepresentation(role: RoleModel, @Context clientModel: Stream<ClientModel>): ClientRole

    @AfterMapping
    fun convert(role: RoleModel, @MappingTarget clientRole: ClientRole, @Context list: Stream<ClientModel>) {
        clientRole.client = list.filter { c -> role.containerId == c.id }.findFirst().get().clientId
    }
}