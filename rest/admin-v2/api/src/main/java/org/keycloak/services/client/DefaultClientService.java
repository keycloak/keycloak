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
import org.keycloak.models.mapper.MapStructModelMapper;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.representations.admin.v2.validation.CreateClientDefault;
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
    private final ClientModelMapper mapper;
    private final JakartaValidatorProvider validator;
    private final RealmAdminResource realmAdminResource;
    private final ClientsResource clientsResource;
    private ClientResource clientResource;

    public DefaultClientService(KeycloakSession session, RealmAdminResource realmAdminResource, ClientResource clientResource) {
        this.session = session;
        this.realmAdminResource = realmAdminResource;
        this.clientResource = clientResource;

        this.clientsResource = realmAdminResource.getClients();
        this.mapper = new MapStructModelMapper().clients();
        this.validator = new HibernateValidatorProvider();
    }

    public DefaultClientService(KeycloakSession session, RealmAdminResource realmAdminResource) {
        this(session, realmAdminResource, null);
    }

    @Override
    public Optional<ClientRepresentation> getClient(RealmModel realm, String clientId, ClientProjectionOptions projectionOptions) {
        // TODO: is the access map on the representation needed
        return Optional.ofNullable(clientResource).map(ClientResource::viewClientModel).map(model -> mapper.fromModel(session, model));
    }

    @Override
    public Stream<ClientRepresentation> getClients(RealmModel realm, ClientProjectionOptions projectionOptions,
                                                   ClientSearchOptions searchOptions, ClientSortAndSliceOptions sortAndSliceOptions) {
        // TODO: is the access map on the representation needed
        return clientsResource.getClientModels(null, true, false, null, null, null).map(model -> mapper.fromModel(session, model));
    }

    @Override
    public CreateOrUpdateResult createOrUpdate(RealmModel realm, ClientRepresentation client, boolean allowUpdate) throws ServiceException {
        boolean created = false;
        ClientModel model;
        if (clientResource != null) {
            if (!allowUpdate) {
                throw new ServiceException("Client already exists", Response.Status.CONFLICT);
            }
            model = mapper.toModel(session, realm, clientResource.viewClientModel(), client);
            var rep = ModelToRepresentation.toRepresentation(model, session);
            clientResource.update(rep);
        } else {
            created = true;
            validator.validate(client, CreateClientDefault.class); // TODO improve it to avoid second validation when we know it is create and not update

            model = mapper.toModel(session, realm, client);
            var rep = ModelToRepresentation.toRepresentation(model, session);
            model = clientsResource.createClientModel(rep);
            clientResource = clientsResource.getClient(model.getId());
        }

        handleRoles(client.getRoles());
        handleServiceAccount(model, client.getServiceAccount());
        var updated = mapper.fromModel(session, model);

        return new CreateOrUpdateResult(updated, created);
    }

    @Override
    public Stream<ClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions) {
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
    protected void handleServiceAccount(ClientModel model, ClientRepresentation.ServiceAccount serviceAccount) {
        if (serviceAccount != null && serviceAccount.getEnabled() != null) {
            ClientResource.updateClientServiceAccount(session, model, serviceAccount.getEnabled());

            if (serviceAccount.getEnabled()) {
                var clientRoleResource = clientResource.getRoleContainerResource();
                var realmRoleResource = realmAdminResource.getRoleContainerResource();

                var serviceAccountUser = session.users().getServiceAccount(model);
                var serviceAccountRoleResource = realmAdminResource.users().user(clientResource.getServiceAccountUser().getId()).getRoleMappings();

                Set<String> desiredRoleNames = Optional.ofNullable(serviceAccount.getRoles()).orElse(Collections.emptySet());
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
    }

}
