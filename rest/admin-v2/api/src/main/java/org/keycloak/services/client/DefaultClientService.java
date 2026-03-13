package org.keycloak.services.client;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
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
import org.keycloak.representations.admin.v2.validation.PutClient;
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
import org.keycloak.utils.StringUtil;
import org.keycloak.validation.ValidationUtil;
import org.keycloak.validation.jakarta.HibernateValidatorProvider;
import org.keycloak.validation.jakarta.JakartaValidatorProvider;
import org.keycloak.validation.jakarta.ValidationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import static org.keycloak.representations.admin.v2.validation.ClientSecretNotBlankValidator.isClientSecret;

/**
 * Legacy implementation of ClientService for Admin API v2 that uses Admin API v1 under hood.
 */
public class DefaultClientService implements ClientService {
    private static final ObjectMapper MAPPER = new ObjectMapperResolver().getContext(null);

    private final KeycloakSession session;
    private final JakartaValidatorProvider validator;
    private final AdminPermissionEvaluator permissions;
    private final AdminEventBuilder adminEventBuilder;

    // v1 resources
    private final RealmAdminResource realmResource;
    private final ClientsResource clientsResource;

    public DefaultClientService(@Nonnull KeycloakSession session,
                                @Nonnull RealmModel realm,
                                @Nonnull AdminPermissionEvaluator permissions,
                                @Nonnull RealmAdminResource realmResource) {
        this.session = session;
        this.permissions = permissions;
        this.validator = new HibernateValidatorProvider(new ValidationContext(session, realm));

        this.realmResource = realmResource;
        this.clientsResource = realmResource.getClients();
        this.adminEventBuilder = new AdminEventV2Builder(realm, permissions.adminAuth(), session, session.getContext().getConnection())
                .resource(ResourceType.CLIENT);
    }

    @Override
    public Optional<BaseClientRepresentation> getClient(RealmModel realm, String clientId, ClientProjectionOptions projectionOptions) throws ServiceException {
        // TODO: is the access map on the representation needed
        var clientResource = assertAndGetClientResource(realm, clientId);
        return Optional.of(clientResource)
                .map(ClientResource::viewClientModel)
                .map(model -> getMapper(model.getProtocol()).fromModel(model));
    }

    @Override
    public Stream<BaseClientRepresentation> getClients(RealmModel realm, ClientProjectionOptions projectionOptions,
                                                   ClientSearchOptions searchOptions, ClientSortAndSliceOptions sortAndSliceOptions) {
        // TODO: is the access map on the representation needed
        return clientsResource.getClientModels(null, true, false, null, null, null)
                .filter(model -> model.getProtocol() != null) // Skip clients with null protocol
                .map(model -> getMapper(model.getProtocol()).fromModel(model))
                .filter(java.util.Objects::nonNull);
    }

    @Override
    public BaseClientRepresentation createClient(RealmModel realm, BaseClientRepresentation client) throws ServiceException {
        return createOrUpdate(realm, null, client, CreateOrUpdateStrategy.ONLY_CREATE).representation();
    }

    @Override
    public CreateOrUpdateResult createOrUpdateClient(RealmModel realm, String clientId, BaseClientRepresentation client) throws ServiceException {
        if (!Objects.equals(clientId, client.getClientId())) {
            throw new ServiceException("Field 'clientId' in payload does not match the provided 'clientId'", Response.Status.BAD_REQUEST);
        }
        return createOrUpdate(realm, clientId, client, CreateOrUpdateStrategy.PUT);
    }

    private enum CreateOrUpdateStrategy {
        ONLY_CREATE,
        PUT,
        // PATCH is currently separated from PUT only due to validation running before full preparation/defaulting.
        // Once we validate the fully prepared resource, PUT and PATCH should share the same validation logic.
        PATCH
    }

    private CreateOrUpdateResult createOrUpdate(RealmModel realm, String clientId, BaseClientRepresentation client, CreateOrUpdateStrategy strategy) throws ServiceException {
        validateUnknownFields(client);

        if (strategy == CreateOrUpdateStrategy.PUT) {
            validator.validate(client, PutClient.class);
        }

        boolean created = false;
        ClientModel model;
        ClientModelMapper mapper = getMapper(client.getProtocol());

        var clientResource = getClientResource(realm, clientId).orElse(null);
        if (clientResource != null) {
            if (strategy.equals(CreateOrUpdateStrategy.ONLY_CREATE)) {
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
            validator.validate(client, CreateClientDefault.class);

            // First, create a basic v1 representation to persist the client in the database.
            // We can't use mapper.toModel(client) directly for creation because the "detached model"
            var basicRep = new ClientRepresentation();
            basicRep.setClientId(client.getClientId());
            basicRep.setProtocol(client.getProtocol());

            // TODO: we should avoid 'instanceOf' once we stop using the v1 representation
            if (client instanceof OIDCClientRepresentation oidcClient) {
                var auth = oidcClient.getAuth();
                if (auth != null && isClientSecret(auth.getMethod())) {
                    // this makes sure that client secret is generated for "create" methods if necessary
                    basicRep.setPublicClient(false);
                    basicRep.setClientAuthenticatorType(auth.getMethod());
                    basicRep.setSecret(auth.getSecret());
                }
            }

            // Create the client in the database
            model = clientsResource.createClientModel(basicRep);
            clientResource = clientsResource.getClient(model.getId());

            // TODO: we should avoid 'instanceOf' once we stop using the v1 representation
            if (model.getSecret() != null && client instanceof OIDCClientRepresentation oidcClient) {
                // set generated secret
                oidcClient.getAuth().setSecret(model.getSecret());
            }

            mapper.toModel(client, model);

            // Validate the fully populated model (createClientModel only validates the basic model)
            ValidationUtil.validateClient(session, model, true, r -> {
                session.getTransactionManager().setRollbackOnly();
                throw new ServiceException(r.getAllErrorsAsString(), Response.Status.BAD_REQUEST);
            });
        }

        handleRoles(clientResource, client.getRoles());
        if (client instanceof OIDCClientRepresentation oidcClient) {
            handleServiceAccount(clientResource, model, oidcClient);
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
    protected void fireAdminEvent(OperationType operationType, BaseClientRepresentation representation) {
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

        return createOrUpdate(realm, clientId, updated, CreateOrUpdateStrategy.PATCH).representation();
    }

    @Override
    public Stream<BaseClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteClient(RealmModel realm, String clientId) throws ServiceException {
        var clientResource = assertAndGetClientResource(realm, clientId);
        var client = Optional.of(clientResource.viewClientModel())
                .map(c -> getMapper(c.getProtocol()).fromModel(c))
                .orElseThrow(() -> new ServiceException("Cannot map client model", Response.Status.BAD_REQUEST));

        clientResource.deleteClient();
        fireAdminEvent(OperationType.DELETE, client);
    }

    /**
     * Declaratively manage client roles - ensures the client has exactly the roles specified in 'rolesFromRep'
     * <p>
     * Reuses API v1 logic
     */
    protected void handleRoles(ClientResource clientResource, Set<String> rolesFromRep) {
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
    protected void handleServiceAccount(ClientResource clientResource, ClientModel model, OIDCClientRepresentation rep) {
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

    private ClientResource assertAndGetClientResource(RealmModel realm, String clientId) {
        var client = Optional.ofNullable(session.clients().getClientByClientId(realm, clientId));
        // handles the phishing check
        return clientsResource.getClient(client.map(ClientModel::getId).orElse(""));
    }

    protected ClientModelMapper getMapper(String protocol) {
        return Optional.ofNullable(session.getProvider(ClientModelMapper.class, protocol))
                .orElseThrow(() -> new ServiceException("Mapper not found, unsupported client protocol: " + protocol, Response.Status.BAD_REQUEST));
    }

    private Optional<ClientResource> getClientResource(RealmModel realm, String clientId) {
        if (StringUtil.isBlank(clientId)) {
            return Optional.empty();
        }
        try {
            return Optional.of(assertAndGetClientResource(realm, clientId));
        } catch (WebApplicationException e) {
            return Optional.empty();
        }
    }
}
