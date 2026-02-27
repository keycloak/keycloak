package org.keycloak.services.client;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.events.admin.v2.AdminEventV2Builder;
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
import org.keycloak.services.PatchType;
import org.keycloak.services.ServiceException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.resources.admin.ClientsResource;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.util.ObjectMapperResolver;
import org.keycloak.validation.ValidationUtil;
import org.keycloak.validation.jakarta.HibernateValidatorProvider;
import org.keycloak.validation.jakarta.JakartaValidatorProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

/**
 * Default implementation of ClientService for Admin API v2.
 */
public class DefaultClientService implements ClientService {
    private static final ObjectMapper MAPPER = new ObjectMapperResolver().getContext(null);

    private final KeycloakSession session;
    private final JakartaValidatorProvider validator;
    private final AdminPermissionEvaluator permissions;

    // v1 resources
    private final RealmAdminResource realmResource;
    private final ClientsResource clientsResource;
    private final AdminEventBuilder adminEventBuilder;
    private ClientResource clientResource;

    public DefaultClientService(@Nonnull KeycloakSession session,
                                @Nonnull AdminPermissionEvaluator permissions,
                                @Nonnull RealmAdminResource realmResource,
                                @Nullable ClientResource clientResource) {
        this.session = session;
        this.permissions = permissions;
        this.validator = new HibernateValidatorProvider();

        this.realmResource = realmResource;
        this.clientsResource = realmResource.getClients();
        this.clientResource = clientResource;
        RealmModel realm = session.getContext().getRealm();
        this.adminEventBuilder = new AdminEventV2Builder(realm, permissions.adminAuth(), session, session.getContext().getConnection())
                .resource(ResourceType.CLIENT);
    }

    public DefaultClientService(@Nonnull KeycloakSession session,
                                @Nonnull AdminPermissionEvaluator permissions,
                                @Nonnull RealmAdminResource realmResource) {
        this(session, permissions, realmResource, null);
    }

    protected void avoidClientIdPhishing() throws ServiceException {
        if (clientResource == null && !permissions.clients().canList()) {
            // we do this to make sure somebody can't phish client IDs
            throw new ServiceException(Response.Status.FORBIDDEN);
        }
    }

    @Override
    public Optional<BaseClientRepresentation> getClient(RealmModel realm, String clientId, ClientProjectionOptions projectionOptions) throws ServiceException {
        // TODO: is the access map on the representation needed
        avoidClientIdPhishing();
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
        validateUnknownFields(client);

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

            try (var response = clientResource.update(rep)) {
                // close response and consume payload due to performance reasons
                EntityUtils.consumeQuietly((HttpEntity) response.getEntity());
            }
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

            // Validate the fully populated model (createClientModel only validates the basic model)
            ValidationUtil.validateClient(session, model, true, r -> {
                session.getTransactionManager().setRollbackOnly();
                throw new ServiceException(r.getAllErrorsAsString(), Response.Status.BAD_REQUEST);
            });
        }

        handleRoles(client.getRoles());
        if (client instanceof OIDCClientRepresentation oidcClient) {
            handleServiceAccount(model, oidcClient);
        }

        fireAdminEvent(created ? OperationType.CREATE : OperationType.UPDATE, mapper.fromModel(model));

        return new CreateOrUpdateResult(mapper.fromModel(model), created);
    }

    /**
     * Fires a v2 admin event for client operations (only enabled for testing now to avoid duplicated admin events)
     *
     * @param operationType the type of operation (CREATE, UPDATE, DELETE)
     * @param representation the v2 representation of the client
     */
    private void fireAdminEvent(OperationType operationType, BaseClientRepresentation representation) {
        if (Boolean.parseBoolean(System.getProperty("kc.admin-v2.client-service.events.enabled","false"))) {
            adminEventBuilder
                    .operation(operationType)
                    .resourcePath(session.getContext().getUri())
                    .representation(representation)
                    .success();
        }
    }

    @Override
    public BaseClientRepresentation patchClient(RealmModel realm, String clientId, PatchType patchType, JsonNode patch) throws ServiceException {
        Supplier<BaseClientRepresentation> getOriginalClient = () -> getClient(realm, clientId)
                .orElseThrow(() -> new ServiceException("Cannot find the specified client", Response.Status.NOT_FOUND));

        BaseClientRepresentation updated;
        switch (patchType) {
            case JSON_MERGE -> {
                try {
                    if (patch == null) {
                        // based on the RFC 7396 JSON Merge Patch should replace the whole entity if the patch is not an object - we can't do it
                        throw new ServiceException("Cannot replace client resource with null", Response.Status.BAD_REQUEST);
                    }
                    final ObjectReader objectReader = MAPPER.readerForUpdating(getOriginalClient.get());
                    updated = objectReader.readValue(patch);
                } catch (JsonProcessingException e) {
                    throw new ServiceException(e.getMessage(), Response.Status.BAD_REQUEST);
                } catch (IOException e) {
                    throw new ServiceException("Unknown Error Occurred", Response.Status.INTERNAL_SERVER_ERROR);
                }
            }
            default -> throw new ServiceException("Invalid patch type", Response.Status.UNSUPPORTED_MEDIA_TYPE);
        }

        return createOrUpdate(realm, updated, true).representation();
    }

    @Override
    public Stream<BaseClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteClient(RealmModel realm, String clientId) throws ServiceException {
        avoidClientIdPhishing();
        if (clientResource == null) {
            throw new ServiceException("Cannot find the specified client", Response.Status.NOT_FOUND);
        }
        clientResource.deleteClient();
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
                .forEach(roleName -> {
                    try (var response = roleResource.createRole(new RoleRepresentation(roleName, "", false))) {
                        // close response and consume payload due to performance reasons
                        EntityUtils.consumeQuietly((HttpEntity) response.getEntity());
                    }
                });

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
        var realmRoleResource = realmResource.getRoleContainerResource();

        var serviceAccountUser = session.users().getServiceAccount(model);
        var serviceAccountRoleResource = realmResource.users().user(clientResource.getServiceAccountUser().getId()).getRoleMappings();

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

    protected void validateUnknownFields(BaseClientRepresentation rep) {
        if (!rep.getAdditionalFields().isEmpty()) {
            throw new ServiceException("Payload contains unknown fields: " + rep.getAdditionalFields().keySet(), Response.Status.BAD_REQUEST);
        }
    }
}
