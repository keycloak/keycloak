package org.keycloak.admin.mapper;

import org.keycloak.admin.api.client.ClientRepresentation;
import org.keycloak.admin.api.mapper.ApiModelMapper;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper
public interface MapStructApiModelMapper extends ApiModelMapper {
    @Mapping(target = "displayName", source = "name")
    @Mapping(target = "appUrl", source = "baseUrl")
    @Mapping(target = "appRedirectUrls", source = "redirectUris")
    @Mapping(target = "loginFlows", source = "authenticationFlowBindingOverrides", ignore = true)
    @Mapping(target = "auth", ignore = true) // TODO
    @Mapping(target = "roles", source = "rolesStream", qualifiedByName = "getRoleStrings")
    @Mapping(target = "serviceAccount.enabled", source = "serviceAccountsEnabled")
    @Mapping(target = "serviceAccount.roles", source = "rolesStream", qualifiedByName = "getServiceAccountRoles")
    ClientRepresentation fromModel(ClientModel model);

    @Named("getRoleStrings")
    default Set<String> getRoleStrings(Stream<RoleModel> stream) {
        return stream.map(RoleModel::getName).collect(Collectors.toSet());
    }

    @Named("getServiceAccountRoles")
    default Set<String> getServiceAccountRoles(Stream<RoleModel> stream) {
        return stream.filter(f -> true) //TODO check roles for SA
                .map(RoleModel::getName)
                .collect(Collectors.toSet());
    }
}
