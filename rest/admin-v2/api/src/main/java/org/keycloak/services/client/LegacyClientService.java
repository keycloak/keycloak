package org.keycloak.services.client;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.validation.groups.Default;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ServiceException;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.resources.admin.ClientsResource;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.utils.StringUtil;
import org.keycloak.validation.ValidationUtil;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import static org.keycloak.representations.admin.v2.validation.ClientSecretNotBlankValidator.isClientSecret;

/**
 * Legacy implementation of ClientService for Admin API v2 that uses Admin API v1 under hood.
 */
public class LegacyClientService extends DefaultClientService implements ClientService {
    private final ClientsResource clientsResource;

    public LegacyClientService(@Nonnull KeycloakSession session,
                               @Nonnull RealmModel realm,
                               @Nonnull AdminPermissionEvaluator permissions,
                               @Nonnull RealmAdminResource realmResource) {
        super(session, realm, permissions, realmResource);
        this.clientsResource = realmResource.getClients();
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

    private CreateOrUpdateResult createOrUpdate(RealmModel realm, String clientId, BaseClientRepresentation client, CreateOrUpdateStrategy strategy) throws ServiceException {
        validateUnknownFields(client);

        boolean created = false;
        ClientModel model;
        ClientModelMapper mapper = getMapper(client.getProtocol());

        var clientResource = getClientResource(realm, clientId).orElse(null);
        if (clientResource != null) {
            if (strategy.equals(CreateOrUpdateStrategy.ONLY_CREATE)) {
                throw new ServiceException("Client already exists", Response.Status.CONFLICT);
            }
            validator.validate(client, strategy.getValidationGroup(), Default.class);

            model = mapper.toModel(client, clientResource.viewClientModel());
            var rep = ModelToRepresentation.toRepresentation(model, session);

            try (var response = clientResource.update(rep)) {
                // close response and consume payload due to performance reasons
                EntityUtils.consumeQuietly((HttpEntity) response.getEntity());
            }
        } else {
            created = true;
            validator.validate(client, strategy.getValidationGroup(), Default.class);

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

        handleRoles(clientResource.getRoleContainerResource(), client.getRoles());
        if (client instanceof OIDCClientRepresentation oidcClient) {
            handleServiceAccount(clientResource.getRoleContainerResource(), realmResource.getRoleContainerResource(), model, oidcClient);
        }

        fireAdminEvent(created ? OperationType.CREATE : OperationType.UPDATE, mapper.fromModel(model));

        return new CreateOrUpdateResult(mapper.fromModel(model), created);
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

    private ClientResource assertAndGetClientResource(RealmModel realm, String clientId) {
        var client = Optional.ofNullable(session.clients().getClientByClientId(realm, clientId));
        // handles the phishing check
        return clientsResource.getClient(client.map(ClientModel::getId).orElse(""));
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
