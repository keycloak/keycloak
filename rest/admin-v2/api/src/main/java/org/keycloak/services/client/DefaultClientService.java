package org.keycloak.services.client;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.validation.CreateClientDefault;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.ServiceException;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.resources.admin.ClientsResource;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.validation.jakarta.HibernateValidatorProvider;
import org.keycloak.validation.jakarta.JakartaValidatorProvider;

// TODO
public class DefaultClientService implements ClientService {
    private final KeycloakSession session;
    private final JakartaValidatorProvider validator;
    private final RealmAdminResource realmAdminResource;
    private final ClientsResource clientsResource;
    private ClientResource clientResource;

    public DefaultClientService(KeycloakSession session, RealmAdminResource realmAdminResource, ClientResource clientResource) {
        this.session = session;
        this.realmAdminResource = realmAdminResource;
        this.clientResource = clientResource;

        this.clientsResource = realmAdminResource.getClients();
        this.validator = new HibernateValidatorProvider();
    }

    public DefaultClientService(KeycloakSession session, RealmAdminResource realmAdminResource) {
        this(session, realmAdminResource, null);
    }

    @Override
    public Optional<BaseClientRepresentation> getClient(RealmModel realm, String clientId, ClientProjectionOptions projectionOptions) {
        // TODO: is the access map on the representation needed
        return Optional.ofNullable(clientResource).map(ClientResource::viewClientModel)
                .map(model -> session.getProvider(ClientModelMapper.class, model.getProtocol()).fromModel(model));
    }

    @Override
    public Stream<BaseClientRepresentation> getClients(RealmModel realm, ClientProjectionOptions projectionOptions,
                                                   ClientSearchOptions searchOptions, ClientSortAndSliceOptions sortAndSliceOptions) {
        // TODO: is the access map on the representation needed
        return clientsResource.getClientModels(null, true, false, null, null, null)
                .filter(model -> model.getProtocol() != null) // Skip clients with null protocol
                .map(model -> session.getProvider(ClientModelMapper.class, model.getProtocol()).fromModel(model))
                .filter(java.util.Objects::nonNull);
    }

    @Override
    public CreateOrUpdateResult createOrUpdate(RealmModel realm, BaseClientRepresentation client, boolean allowUpdate) throws ServiceException {
        boolean created = false;
        ClientModel model;
        ClientModelMapper mapper = session.getProvider(ClientModelMapper.class, client.getProtocol());

        if (mapper == null) {
            throw new ServiceException("Mapper not found, unsupported client protocol: " + client.getProtocol(), Response.Status.BAD_REQUEST);
        }

        if (clientResource != null) {
            if (!allowUpdate) {
                throw new ServiceException("Client already exists", Response.Status.CONFLICT);
            }
            model = mapper.toModel(client, clientResource.viewClientModel());
            var rep = ModelToRepresentation.toRepresentation(model, session);
            clientResource.update(rep);
        } else {
            created = true;
            validator.validate(client, CreateClientDefault.class); // TODO improve it to avoid second validation when we know it is create and not update

            // First, create a basic v1 representation to persist the client in the database.
            // We can't use mapper.toModel(client) directly for creation because the "detached model"
            var basicRep = new ClientRepresentation();
            basicRep.setClientId(client.getClientId());
            basicRep.setProtocol(client.getProtocol());

            // Create the client in the database
            model = clientsResource.createClientModel(basicRep);
            clientResource = clientsResource.getClient(model.getId());

            mapper.toModel(client, model);
        }

        handleRoles(client.getRoles());
        if (client instanceof OIDCClientRepresentation oidcClient) {
            handleServiceAccount(model, oidcClient);
        }
        var updated = mapper.fromModel(model);

        return new CreateOrUpdateResult(updated, created);
    }

    @Override
    public Stream<BaseClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Declaratively manage client roles - ensures the client has exactly the roles specified in 'rolesFromRep'
     * <p>
     * Reuses API v1 logic
     */
    protected void handleRoles(Set<String> rolesFromRep) {
        var roleResource = clientResource.getRoleContainerResource();

        Set<String> desiredRoleNames = Optional.ofNullable(rolesFromRep)
                .orElse(Collections.emptySet());

        Set<String> currentRoleNames = roleResource.getRoles(null, null, null, false)
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());

        // Add missing roles (in desiredRoleNames but not in currentRoleNames)
        desiredRoleNames.stream()
                .filter(roleName -> !currentRoleNames.contains(roleName))
                .forEach(roleName -> roleResource.createRole(new RoleRepresentation(roleName, "", false)));

        // Remove extra roles (in currentRoleNames but not in desiredRoleNames)
        currentRoleNames.stream()
                .filter(role -> !desiredRoleNames.contains(role))
                .forEach(roleResource::deleteRole);
    }

    /**
     * Declaratively manage service account - enables/disables it and ensures it has exactly the roles specified (realm and client roles)
     * <p>
     * Reuses API v1 logic
     */
    protected void handleServiceAccount(ClientModel model, OIDCClientRepresentation rep) {
        boolean serviceAccountEnabled = rep.getLoginFlows().contains(OIDCClientRepresentation.Flow.SERVICE_ACCOUNT);

        ClientResource.updateClientServiceAccount(session, model, serviceAccountEnabled);

        if (!serviceAccountEnabled) {
            return;
        }

        var clientRoleResource = clientResource.getRoleContainerResource();
        var realmRoleResource = realmAdminResource.getRoleContainerResource();

        var serviceAccountUser = session.users().getServiceAccount(model);
        var serviceAccountRoleResource = realmAdminResource.users().user(clientResource.getServiceAccountUser().getId()).getRoleMappings();

        Set<String> desiredRoleNames = Optional.ofNullable(rep.getServiceAccountRoles()).orElse(Collections.emptySet());
        Set<RoleModel> currentRoles = serviceAccountUser.getRoleMappingsStream().collect(Collectors.toSet());
        Set<String> currentRoleNames = currentRoles.stream().map(RoleModel::getName).collect(Collectors.toSet());

        // Get missing roles (in desired but not in current)
        List<RoleRepresentation> missingRoles = desiredRoleNames.stream()
                .filter(roleName -> !currentRoleNames.contains(roleName))
                .map(roleName -> {
                    try {
                        return clientRoleResource.getRole(roleName); // client role
                    } catch (NotFoundException e) {
                        try {
                            return realmRoleResource.getRole(roleName); // realm role
                        } catch (NotFoundException e2) {
                            throw new ServiceException("Cannot assign role to the service account (field 'serviceAccount.roles') as it does not exist", Response.Status.BAD_REQUEST);
                        }
                    }
                })
                .toList();

        // Add missing roles (in desired but not in current)
        if (!missingRoles.isEmpty()) {
            serviceAccountRoleResource.addRealmRoleMappings(missingRoles);
        }

        // Get extra roles (in current but not in desired)
        List<RoleRepresentation> extraRoles = currentRoles.stream()
                .filter(role -> !desiredRoleNames.contains(role.getName()))
                .map(ModelToRepresentation::toRepresentation)
                .toList();

        // Remove extra roles (in current but not in desired)
        if (!extraRoles.isEmpty()) {
            try {
                serviceAccountRoleResource.deleteRealmRoleMappings(extraRoles);
            } catch (NotFoundException e) {
                throw new ServiceException("Cannot unassign role from the service account (field 'serviceAccount.roles') as it does not exist", Response.Status.BAD_REQUEST);
            }
        }
    }

}
