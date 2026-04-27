package org.keycloak.services.client;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.validation.groups.Default;
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
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
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
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.validation.ValidationUtil;
import org.keycloak.validation.jakarta.HibernateValidatorProvider;
import org.keycloak.validation.jakarta.JakartaValidatorProvider;
import org.keycloak.validation.jakarta.ValidationContext;

import static org.keycloak.representations.admin.v2.validators.ClientSecretNotBlankValidator.isClientSecret;
import static org.keycloak.utils.StringUtil.isBlank;

/**
 * New client service (name is just temporary) that should not rely on the Admin API v1
 * <p>
 * During development, it will use the legacy client service to unblock incremental development of this service
 */
public class NewClientService extends DefaultClientService implements ClientService {
    private final KeycloakSession session;
    private final AdminPermissionEvaluator permissions;
    private final AdminEventBuilder adminEventBuilder;
    private final JakartaValidatorProvider validator;
    private final RolesService rolesService;

    public NewClientService(@Nonnull KeycloakSession session,
                            @Nonnull RealmModel realm,
                            @Nonnull AdminPermissionEvaluator permissions,
                            // TODO remove the v1 resource once all methods are overridden
                            @Nonnull RealmAdminResource realmResource) {
        super(session, realm, permissions, realmResource);
        this.session = session;
        this.permissions = permissions;
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
                if (client.getProtocol().equals(OIDCClientRepresentation.PROTOCOL)) {
                    // TODO we should find a way on how to evoke it on the mapper level?
                    var auth = ((OIDCClientRepresentation) client).getAuth();
                    if (auth != null && isClientSecret(auth.getMethod()) && isBlank(auth.getSecret())) {
                        auth.setSecret(KeycloakModelUtils.generateSecret(model));
                    }
                }
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
}
