package org.keycloak.models.mapper;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.ServiceException;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ObjectFactory;

@Mapper
public interface MapStructClientModelMapper extends ClientModelMapper {

    @Override
    @ModelToRep
    ClientRepresentation fromModel(@Context KeycloakSession session, ClientModel model);

    // we don't want to ignore nulls so that we completely overwrite the state
    @Override
    @RepToModel
    ClientModel toModel(@Context KeycloakSession session, @Context RealmModel realm, @MappingTarget ClientModel existingModel, ClientRepresentation rep) throws ServiceException;

    @Override
    @RepToModel
    ClientModel toModel(@Context KeycloakSession session, @Context RealmModel realm, ClientRepresentation rep) throws ServiceException;

    /*-------------------------------------*
     *              MAPPERS                *
     *-------------------------------------*/
    @Mapping(target = "name", source = "displayName")
    @Mapping(target = "baseUrl", source = "appUrl")
    @Mapping(target = "redirectUris", source = "appRedirectUrls")
    @Mapping(target = "authenticationFlowBindingOverrides", source = "loginFlows", ignore = true) // TODO
    @Mapping(target = "publicClient", source = "auth.enabled", qualifiedByName = "isPublicClientPrimitive")
    @Mapping(target = "clientAuthenticatorType", source = "auth.method")
    @Mapping(target = "secret", source = "auth.secret")
    @Mapping(target = "serviceAccountsEnabled", source = "serviceAccount.enabled")
    @interface RepToModel {
    }

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
    @Mapping(target = "serviceAccount.roles", source = ".", qualifiedByName = "getServiceAccountRoles")
    @interface ModelToRep {
    }

    /*-------------------------------------*
     *          HELPER METHODS             *
     *-------------------------------------*/
    @ObjectFactory
    default ClientModel createClientModel(@Context RealmModel realm, ClientRepresentation rep) {
        // dummy add/remove to obtain a detached model
        var model = realm.addClient(rep.getClientId());
        realm.removeClient(model.getId());
        return model;
    }

    @AfterMapping
    default void addRoles(@MappingTarget ClientModel model, ClientRepresentation rep, @Context RealmModel realm, @Context KeycloakSession session) {
        Optional.ofNullable(rep.getRoles())
                .orElse(Collections.emptySet())
                .stream()
                .filter(role -> model.getRole(role) == null)
                .forEach(model::addRole);

        // Service Account roles
        var serviceAccount = rep.getServiceAccount();
        if (serviceAccount != null && serviceAccount.getEnabled() && !serviceAccount.getRoles().isEmpty()) {
            new ClientManager(new RealmManager(session)).enableServiceAccount(model);
            var sa = session.users().getServiceAccount(model);

            serviceAccount.getRoles().forEach(role -> {
                var foundRole = realm.getRole(role);
                if (foundRole == null) {
                    throw new ServiceException("Cannot assign role to the service account (field 'serviceAccount.roles') as it does not exist", Response.Status.BAD_REQUEST);
                }
                sa.grantRole(foundRole);
            });
        }
    }

    @Named("isPublicClientPrimitive")
    default boolean isPublicClientPrimitive(Boolean authEnabled) {
        var result = isPublicClient(authEnabled);
        return result != null ? result : false;
    }

    @Named("isPublicClient")
    default Boolean isPublicClient(Boolean authEnabled) {
        return authEnabled != null ? !authEnabled : null;
    }

    @Named("getRoleStrings")
    default Set<String> getRoleStrings(Stream<RoleModel> stream) {
        return stream.map(RoleModel::getName).collect(Collectors.toSet());
    }

    @Named("getServiceAccountRoles")
    default Set<String> getServiceAccountRoles(@Context KeycloakSession session, ClientModel client) {
        if (client.isServiceAccountsEnabled()) {
            return session.users().getServiceAccount(client)
                    .getRoleMappingsStream()
                    .map(RoleModel::getName)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}
