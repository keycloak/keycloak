package org.keycloak.testframework.realm;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.util.ApiUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserSupplier implements Supplier<ManagedUser, InjectUser> {

    private static final String USER_UUID_KEY = "userUuid";

    @Override
    public Class<InjectUser> getAnnotationClass() {
        return InjectUser.class;
    }

    @Override
    public Class<ManagedUser> getValueType() {
        return ManagedUser.class;
    }

    @Override
    public ManagedUser getValue(InstanceContext<ManagedUser, InjectUser> instanceContext) {
        ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class, instanceContext.getAnnotation().realmRef());

        UserConfig config = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        UserRepresentation userRepresentation = config.configure(UserConfigBuilder.create()).build();

        if (userRepresentation.getUsername() == null) {
            String username = SupplierHelpers.createName(instanceContext);
            userRepresentation.setUsername(username);
        }

        try (Response response = realm.admin().users().create(userRepresentation)) {
            if (Status.CONFLICT.equals(Status.fromStatusCode(response.getStatus()))) {
                throw new IllegalStateException("User already exist with username: " + userRepresentation.getUsername());
            }
            String userUuid = ApiUtil.handleCreatedResponse(response);

            instanceContext.addNote(USER_UUID_KEY, userUuid);

            UserResource userResource = realm.admin().users().get(userUuid);
            userRepresentation.setId(userUuid);

            // let's assign the client roles with as much optimization as possible
            Map<String, List<String>> clientRoles = userRepresentation.getClientRoles();
            if (clientRoles == null || clientRoles.isEmpty()) {
                return new ManagedUser(userRepresentation, userResource);
            }
            // replace map keys (instead of the clientId we need the client's uuid)
            ClientsResource clientsResource = realm.admin().clients();
            Map<String, List<String>> clientRolesUuid = clientRoles.entrySet().stream().collect(Collectors.toMap(
                    entry -> clientsResource.findByClientId(entry.getKey()).stream()
                            .findFirst()
                            .orElseThrow()
                            .getId(),
                    Map.Entry::getValue
            ));
            // for each client uuid, get the RoleScopeResource
            RoleMappingResource roleMappingResource = userResource.roles();
            Map<String, RoleScopeResource> roleScopeResources = clientRolesUuid.keySet().stream().collect(Collectors.toMap(
                    key -> key,
                    roleMappingResource::clientLevel
            ));
            // replace the string described roles with actual role representations
            Map<String, List<RoleRepresentation>> clientRolesRepresentations = clientRolesUuid.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> roleScopeResources.get(entry.getKey()).listAvailable().stream()
                            .filter(role -> entry.getValue().contains(role.getName())).toList()
            ));
            // assign user the client roles
            clientRolesRepresentations.forEach((key, value) -> roleScopeResources.get(key).add(value));

            return new ManagedUser(userRepresentation, userResource);
        }
    }

    @Override
    public boolean compatible(InstanceContext<ManagedUser, InjectUser> a, RequestedInstance<ManagedUser, InjectUser> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config());
    }

    @Override
    public void close(InstanceContext<ManagedUser, InjectUser> instanceContext) {
        instanceContext.getValue().admin().remove();
    }

}
