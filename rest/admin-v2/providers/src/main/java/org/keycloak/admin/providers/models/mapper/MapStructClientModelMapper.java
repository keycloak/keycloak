package org.keycloak.admin.providers.models.mapper;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.representations.admin.v2.ClientRepresentation;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper
public interface MapStructClientModelMapper extends ClientModelMapper {
    @Mapping(target = "displayName", source = "name")
    @Mapping(target = "appUrl", source = "baseUrl")
    @Mapping(target = "appRedirectUrls", source = "redirectUris")
    @Mapping(target = "loginFlows", source = "authenticationFlowBindingOverrides", ignore = true)
    @Mapping(target = "auth.enabled", source = "publicClient", qualifiedByName = "isPublicClient")
    @Mapping(target = "auth.method", source = "clientAuthenticatorType")
    @Mapping(target = "auth.secret", source = "secret")
    @Mapping(target = "auth.certificate", ignore = true) // no cert in the representation
    @Mapping(target = "roles", source = "rolesStream", qualifiedByName = "getRoleStrings")
    @Mapping(target = "serviceAccount.enabled", source = "serviceAccountsEnabled")
    @Mapping(target = "serviceAccount.roles", source = "rolesStream", qualifiedByName = "getServiceAccountRoles")
    @Override
    ClientRepresentation fromModel(ClientModel model);

    // we don't want to ignore nulls so that we completely overwrite the state
    @Override
    void toModel(@MappingTarget ClientModel model, ClientRepresentation rep, @Context RealmModel realm);

    @Mapping(target = "name", source = "displayName")
    @Mapping(target = "baseUrl", source = "appUrl")
    @Mapping(target = "redirectUris", source = "appRedirectUrls")
    @Mapping(target = "authenticationFlowBindingOverrides", ignore = true) // TODO: map from loginFlows
    @Mapping(target = "publicClient", source = "auth.enabled", qualifiedByName = "isPublicClient")
    @Mapping(target = "clientAuthenticatorType", source = "auth.method")
    @Mapping(target = "secret", source = "auth.secret")
    @Mapping(target = "serviceAccountsEnabled", source = "serviceAccount.enabled")
    @Override
    org.keycloak.representations.idm.ClientRepresentation mapRepresentationV2toV1(ClientRepresentation representationV2);

    @Named("isPublicClient")
    default Boolean isPublicClient(Boolean authEnabled) {
        // If auth is enabled, it's not a public client (inverted logic)
        return authEnabled != null ? !authEnabled : null;
    }

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
