package org.keycloak.services.client;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.ws.rs.core.Response;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.events.admin.v2.AdminEventV2Builder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.ServiceException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientViewContext;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/**
 * New client service (name is just temporary) that should not rely on the Admin API v1
 * <p>
 * During development, it will use the legacy client service to unblock incremental development of this service
 */
public class NewClientService extends DefaultClientService implements ClientService {
    private final KeycloakSession session;
    private final AdminPermissionEvaluator permissions;
    private final AdminEventBuilder adminEventBuilder;

    public NewClientService(@Nonnull KeycloakSession session,
                            @Nonnull RealmModel realm,
                            @Nonnull AdminPermissionEvaluator permissions,
                            // TODO remove the v1 resource once all methods are overridden
                            @Nonnull RealmAdminResource realmResource) {
        super(session, realm, permissions, realmResource);
        this.session = session;
        this.permissions = permissions;
        this.adminEventBuilder = new AdminEventV2Builder(realm, permissions.adminAuth(), session, session.getContext().getConnection()).resource(ResourceType.CLIENT);
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
        try {
            session.clientPolicy().triggerOnEvent(new AdminClientViewContext(client, permissions.adminAuth()));
            permissions.clients().requireView(client);

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
}
