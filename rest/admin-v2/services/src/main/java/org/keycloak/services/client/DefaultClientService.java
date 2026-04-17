package org.keycloak.services.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.validation.groups.Default;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.events.admin.v2.AdminEventV2Builder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.validation.CreateClient;
import org.keycloak.representations.admin.v2.validation.PatchClient;
import org.keycloak.representations.admin.v2.validation.PutClient;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.PatchType;
import org.keycloak.services.RolesService;
import org.keycloak.services.ServiceException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientRegisteredContext;
import org.keycloak.services.clientpolicy.context.AdminClientUnregisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdatedContext;
import org.keycloak.services.clientpolicy.context.AdminClientViewContext;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.RoleContainerResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.util.ObjectMapperResolver;
import org.keycloak.validation.ValidationUtil;
import org.keycloak.validation.jakarta.HibernateValidatorProvider;
import org.keycloak.validation.jakarta.JakartaValidatorProvider;
import org.keycloak.validation.jakarta.ValidationContext;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import static org.keycloak.representations.admin.v2.validators.ClientSecretNotBlankValidator.isClientSecret;
import static org.keycloak.utils.StringUtil.isBlank;

/**
 * Default client service for Admin Client API v2
 */
public class DefaultClientService implements ClientService {
    private static final ObjectMapper MAPPER = new ObjectMapperResolver().getContext(null);

    private final KeycloakSession session;
    private final AdminPermissionEvaluator permissions;
    private final RealmAdminResource realmResource;
    private final AdminEventBuilder adminEventBuilder;
    private final JakartaValidatorProvider validator;
    private final RolesService rolesService;

    public DefaultClientService(@Nonnull KeycloakSession session,
                                @Nonnull RealmModel realm,
                                @Nonnull AdminPermissionEvaluator permissions,
                                @Nonnull RealmAdminResource realmResource) {
        this.session = session;
        this.permissions = permissions;
        this.realmResource = realmResource;
        this.adminEventBuilder = new AdminEventV2Builder(realm, permissions.adminAuth(), session, session.getContext().getConnection()).resource(ResourceType.CLIENT);
        this.validator = new HibernateValidatorProvider(new ValidationContext(session, realm));
        this.rolesService = new RolesService(session, realm, permissions, adminEventBuilder);
    }

    protected Optional<ClientModel> avoidClientIdPhishing(ClientModel client) throws ServiceException {
        if (client == null && !permissions.clients().canList()) {
            // we do this to make sure somebody can't phish client IDs
            throw new ServiceException(Response.Status.FORBIDDEN);
        }
        return Optional.ofNullable(client);
    }

    @Override
    public Optional<BaseClientRepresentation> getClient(@Nonnull RealmModel realm,
                                                        @Nonnull String clientId,
                                                        ClientProjectionOptions projectionOptions) throws ServiceException {
        ClientModel client = avoidClientIdPhishing(realm.getClientByClientId(clientId)).orElseThrow(() -> new ServiceException("Could not find client", Response.Status.NOT_FOUND));
        permissions.clients().requireView(client);
        
        try {
            session.clientPolicy().triggerOnEvent(new AdminClientViewContext(client, permissions.adminAuth()));
            return Optional.ofNullable(getMapper(client.getProtocol()).fromModel(client));
        } catch (ClientPolicyException e) {
            throw new ServiceException(e.getErrorDetail(), Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public Stream<BaseClientRepresentation> getClients(@Nonnull RealmModel realm,
                                                       ClientProjectionOptions projectionOptions,
                                                       ClientSearchOptions searchOptions,
                                                       ClientSortAndSliceOptions sortAndSliceOptions) {
        permissions.clients().requireList();

        // When FGAP is enabled, authorization filtering is applied at the JPA layer (via PartialEvaluator predicates), so we trust the DB results.
        // When disabled, we fall back to in-memory filtering by VIEW_CLIENTS role.
        boolean canView = AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm) || permissions.clients().canView();
        try {
            return realm.getClientsStream()
                    .filter(client -> canView || permissions.clients().canView(client))
                    .filter(client -> client.getProtocol() != null)
                    .map(client -> getMapper(client.getProtocol()).fromModel(client))
                    .filter(java.util.Objects::nonNull);
        } catch (ModelException e) {
            throw new ServiceException(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public BaseClientRepresentation createClient(RealmModel realm, BaseClientRepresentation client) throws ServiceException {
        return createOrUpdate(realm, null, client, CreateOrUpdateStrategy.ONLY_CREATE).representation();
    }

    @Override
    public CreateOrUpdateResult createOrUpdateClient(RealmModel realm, String clientId, BaseClientRepresentation client) throws ServiceException {
        return createOrUpdate(realm, clientId, client, CreateOrUpdateStrategy.PUT);
    }

    @Override
    public void deleteClient(RealmModel realm, String clientId) throws ServiceException {
        ClientModel client = avoidClientIdPhishing(realm.getClientByClientId(clientId))
                .orElseThrow(() -> new ServiceException("Could not find client", Response.Status.NOT_FOUND));

        permissions.clients().requireManage(client);
        try {
            AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, client.getId());
            session.clientPolicy().triggerOnEvent(new AdminClientUnregisterContext(client, permissions.adminAuth()));
        } catch (ModelValidationException e) {
            throw new ServiceException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (ClientPolicyException e) {
            throw new ServiceException(e.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        var clientRepresentation = Optional.ofNullable(getMapper(client.getProtocol()).fromModel(client))
                .orElseThrow(() -> new ServiceException("Cannot map client model", Response.Status.BAD_REQUEST));

        if (new ClientManager(new RealmManager(session)).removeClient(realm, client)) {
            fireAdminEvent(OperationType.DELETE, clientRepresentation);
        } else {
            throw new ServiceException("Could not delete client", Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public BaseClientRepresentation patchClient(RealmModel realm, String clientId, PatchType patchType, InputStream patch) throws ServiceException {
        Supplier<BaseClientRepresentation> getOriginalClient = () -> getClient(realm, clientId)
                .orElseThrow(() -> new ServiceException("Cannot find the specified client", Response.Status.NOT_FOUND));

        BaseClientRepresentation updated;
        switch (patchType) {
            case JSON_MERGE -> {
                try (JsonParser parser = MAPPER.getFactory().createParser(patch)) {
                    final ObjectReader objectReader = MAPPER.readerForUpdating(getOriginalClient.get());
                    JsonToken nextToken = parser.nextToken();
                    if (nextToken != JsonToken.START_OBJECT) {
                        throw new ServiceException("Cannot replace client resource with non-object", Response.Status.BAD_REQUEST);
                    }
                    updated = objectReader.readValue(parser);
                    if (parser.nextToken() != null) {
                        throw new ServiceException("Patch contains additional content", Response.Status.BAD_REQUEST);
                    }
                } catch (JsonMappingException e) {
                    var invalidFields = e.getPath().stream().map(JsonMappingException.Reference::getFieldName).collect(Collectors.joining(", "));
                    throw new ServiceException("Invalid values for these fields: %s".formatted((invalidFields)));
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

    protected enum CreateOrUpdateStrategy {
        ONLY_CREATE(CreateClient.class),
        PUT(PutClient.class),
        PATCH(PatchClient.class);

        private final Class<?> validationGroup;

        CreateOrUpdateStrategy(Class<?> validationGroup) {
            this.validationGroup = validationGroup;
        }

        public Class<?> getValidationGroup() {
            return validationGroup;
        }
    }

    private CreateOrUpdateResult createOrUpdate(RealmModel realm, String clientId, BaseClientRepresentation client, CreateOrUpdateStrategy strategy) throws ServiceException {
        validateUnknownFields(client);
        ClientModel model = null;
        if (!strategy.equals(CreateOrUpdateStrategy.ONLY_CREATE)) {
            assertSameClientIds(clientId, client.getClientId());
            model = avoidClientIdPhishing(realm.getClientByClientId(clientId)).orElse(null);
        }
        boolean alreadyExists = model != null;
        ClientModelMapper mapper = getMapper(client.getProtocol());

        try {
            if (alreadyExists) {
                switch (strategy) {
                    case ONLY_CREATE -> throw new ServiceException("Client already exists", Response.Status.CONFLICT);
                    case PUT, PATCH -> {
                        // Check permissions, execute validations and trigger client policies
                        permissions.clients().requireConfigure(model);
                        validator.validate(client, strategy.getValidationGroup(), Default.class);
                        var proposedRepresentation = getProposedOldRepresentation(realm, client, mapper);
                        session.clientPolicy().triggerOnEvent(new AdminClientUpdateContext(proposedRepresentation, model, permissions.adminAuth()));

                        // Generate random secret if applicable
                        generateClientSecretIfNeeded(client, model);

                        // Update model
                        model = mapper.toModel(client, model);

                        // Validate the fully populated model
                        ValidationUtil.validateClient(session, model, false, r -> {
                            session.getTransactionManager().setRollbackOnly();
                            throw new ServiceException(r.getAllErrorsAsString(), Response.Status.BAD_REQUEST);
                        });

                        session.clientPolicy().triggerOnEvent(new AdminClientUpdatedContext(proposedRepresentation, model, permissions.adminAuth()));
                    }
                }
            } else {
                // Check permissions, execute validations and trigger client policies
                permissions.clients().requireManage();
                validator.validate(client, strategy.getValidationGroup(), Default.class);
                var proposedRepresentation = getProposedOldRepresentation(realm, client, mapper);
                session.clientPolicy().triggerOnEvent(new AdminClientRegisterContext(proposedRepresentation, permissions.adminAuth()));

                // Add basic attributes
                model = realm.addClient(clientId);
                model.setProtocol(client.getProtocol());

                // Generate random secret if applicable
                generateClientSecretIfNeeded(client, model);
                mapper.toModel(client, model);

                // Validate the fully populated model
                ValidationUtil.validateClient(session, model, true, r -> {
                    session.getTransactionManager().setRollbackOnly();
                    throw new ServiceException(r.getAllErrorsAsString(), Response.Status.BAD_REQUEST);
                });
                session.clientPolicy().triggerOnEvent(new AdminClientRegisteredContext(model, permissions.adminAuth()));
            }
        } catch (ClientPolicyException e) {
            throw new ServiceException(e.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        // Setup roles
        var clientRoles = rolesService.resource(model);
        handleRoles(clientRoles, client.getRoles());

        // OIDC specific
        if (client instanceof OIDCClientRepresentation oidcClient) {
            handleServiceAccount(clientRoles, rolesService.resource(realm), model, oidcClient);
        }

        fireAdminEvent(alreadyExists ? OperationType.UPDATE : OperationType.CREATE, mapper.fromModel(model));
        return new CreateOrUpdateResult(mapper.fromModel(model), !alreadyExists);
    }

    /**
     * Fires a v2 admin event for client operations (only enabled for testing now to avoid duplicated admin events)
     *
     * @param operationType  the type of operation (CREATE, UPDATE, DELETE)
     * @param representation the v2 representation of the client
     */
    protected void fireAdminEvent(OperationType operationType, BaseClientRepresentation representation) {
        if (Boolean.parseBoolean(System.getProperty("kc.admin-v2.client-service.events.enabled", "false"))) {
            adminEventBuilder
                    .operation(operationType)
                    .resourcePath(session.getContext().getUri())
                    .representation(representation)
                    .success();
        }
    }

    /**
     * Creates a temporary client to convert BaseClientRepresentation to ClientRepresentation.
     * Required because client policy contexts expect ClientRepresentation (v1), but there's no
     * direct converter from BaseClientRepresentation (v2 API). The temp client is immediately removed.
     * <p>
     * For more details, see the <a href="https://github.com/keycloak/keycloak/issues/47576">keycloak#47576</a>.
     */
    private ClientRepresentation getProposedOldRepresentation(RealmModel realm, BaseClientRepresentation client, ClientModelMapper mapper) {
        String tempId = "__temp__" + client.getClientId() + "__" + System.nanoTime();
        ClientModel tempModel = mapper.toModel(client, realm.addClient(tempId));
        try {
            var proposedRepresentation = ModelToRepresentation.toRepresentation(tempModel, session);
            proposedRepresentation.setClientId(client.getClientId());
            return proposedRepresentation;
        } finally {
            realm.removeClient(tempModel.getId());
        }
    }

    // TODO we should find a way on how to evoke it on the mapper level?
    private void generateClientSecretIfNeeded(BaseClientRepresentation client, ClientModel model) {
        if (client.getProtocol().equals(OIDCClientRepresentation.PROTOCOL)) {
            var auth = ((OIDCClientRepresentation) client).getAuth();
            if (auth != null && isClientSecret(auth.getMethod()) && isBlank(auth.getSecret())) {
                auth.setSecret(KeycloakModelUtils.generateSecret(model));
            }
        }
    }

    protected void assertSameClientIds(String pathId, String payloadId) {
        if (payloadId == null) {
            // When the payload clientId is null, it is not part of the payload at all - validated via @NotBlank validator annotation
            return;
        }
        if (!Objects.equals(pathId, payloadId)) {
            throw new ServiceException("Field 'clientId' in payload does not match the provided 'clientId'", Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Declaratively manage client roles - ensures the client has exactly the roles specified in 'rolesFromRep'
     * <p>
     * Reuses API v1 logic
     */
    protected void handleRoles(RoleContainerResource clientRoles, Set<String> rolesFromRep) {
        Set<String> desiredRoleNames = Optional.ofNullable(rolesFromRep)
                .orElse(Collections.emptySet());

        Set<String> currentRoleNames = clientRoles.getRoles(null, null, null, false)
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());

        // Add missing roles (in desiredRoleNames but not in currentRoleNames)
        desiredRoleNames.stream()
                .filter(roleName -> !currentRoleNames.contains(roleName))
                .forEach(roleName -> {
                    try (var response = clientRoles.createRole(new RoleRepresentation(roleName, "", false))) {
                        // close response and consume payload due to performance reasons
                        EntityUtils.consumeQuietly((HttpEntity) response.getEntity());
                    }
                });

        // Remove extra roles (in currentRoleNames but not in desiredRoleNames)
        currentRoleNames.stream()
                .filter(role -> !desiredRoleNames.contains(role))
                .forEach(clientRoles::deleteRole);
    }

    /**
     * Declaratively manage service account - enables/disables it and ensures it has exactly the roles specified (realm and client roles)
     * <p>
     * Reuses API v1 logic
     */
    protected void handleServiceAccount(RoleContainerResource clientRoleResource, RoleContainerResource realmRoleResource, ClientModel model, OIDCClientRepresentation rep) {
        boolean serviceAccountEnabled = rep.getLoginFlows().contains(OIDCClientRepresentation.Flow.SERVICE_ACCOUNT);

        ClientManager.updateClientServiceAccount(session, model, serviceAccountEnabled);

        if (!serviceAccountEnabled) {
            return;
        }

        var serviceAccountUser = new ClientManager(new RealmManager(session)).getServiceAccountUser(model)
                .orElseThrow(() -> new ServiceException("Cannot find service account user", Response.Status.BAD_REQUEST));
        var serviceAccountRoleResource = realmResource.users().user(serviceAccountUser.getId()).getRoleMappings();

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

    protected ClientModelMapper getMapper(String protocol) {
        return Optional.ofNullable(session.getProvider(ClientModelMapper.class, protocol))
                .orElseThrow(() -> new ServiceException("Mapper not found, unsupported client protocol: " + protocol, Response.Status.BAD_REQUEST));
    }
}
