package org.keycloak.services.client;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.delegate.ClientModelLazyDelegate;
import org.keycloak.models.mapper.ClientModelMappers;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.PatchType;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.query.ClientQueryException;
import org.keycloak.services.client.query.QueryFieldExtractor;
import org.keycloak.services.client.query.QueryParseUtils;
import org.keycloak.services.client.scim.ClientJpaQueryExecutor;
import org.keycloak.services.client.scim.ClientJpaQuerySchema;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientViewContext;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public class ScimBackedClientService implements ClientService {

    private static final ClientModelMappers MAPPERS = new ClientModelMappers();

    private final KeycloakSession session;
    private final AdminPermissionEvaluator permissions;
    private final DefaultClientService delegate;

    public ScimBackedClientService(@Nonnull KeycloakSession session,
                                   @Nonnull AdminPermissionEvaluator permissions,
                                   @Nonnull DefaultClientService delegate) {
        this.session = session;
        this.permissions = permissions;
        this.delegate = delegate;
    }

    @Override
    public Optional<BaseClientRepresentation> getClient(RealmModel realm, String clientId) throws ServiceException {
        return delegate.getClient(realm, clientId);
    }

    @Override
    public Stream<BaseClientRepresentation> getClients(RealmModel realm,
                                                       ClientProjectionOptions projectionOptions,
                                                       ClientSearchOptions searchOptions,
                                                       ClientSortAndSliceOptions sortAndSliceOptions) {
        if (!canUseJpaQuery(realm, searchOptions)) {
            return delegate.getClients(realm, projectionOptions, searchOptions, sortAndSliceOptions);
        }

        permissions.clients().requireList();
        validateProjectionFields(projectionOptions);

        boolean canView = canViewAll(realm);
        int offset = sortAndSliceOptions.offset();
        int limit = sortAndSliceOptions.limit();

        try {
            var filterContext = QueryParseUtils.parse(searchOptions.query());
            QueryParseUtils.validate(filterContext);

            Stream<BaseClientRepresentation> stream = ClientJpaQueryExecutor.findClientIds(
                            session, realm, filterContext, offset, limit)
                    .map(id -> new ClientModelLazyDelegate.WithId(session, realm, id))
                    .filter(client -> canView || permissions.clients().canView(client))
                    .filter(client -> client.getProtocol() != null)
                    .map(this::mapClientWithPolicies)
                    .filter(Objects::nonNull);

            return applyProjection(stream, projectionOptions);
        } catch (ClientQueryException e) {
            throw new ServiceException(e.getMessage(), jakarta.ws.rs.core.Response.Status.BAD_REQUEST);
        } catch (ModelException e) {
            throw new ServiceException(e.getMessage(), jakarta.ws.rs.core.Response.Status.BAD_REQUEST);
        }
    }

    private boolean canViewAll(RealmModel realm) {
        return AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm) || permissions.clients().canView();
    }

    private BaseClientRepresentation mapClientWithPolicies(ClientModel client) {
        try {
            session.clientPolicy().triggerOnEvent(new AdminClientViewContext(client, permissions.adminAuth()));
        } catch (ClientPolicyException e) {
            throw new ServiceException(e.getErrorDetail(), jakarta.ws.rs.core.Response.Status.BAD_REQUEST);
        }
        return delegate.getMapper(client.getProtocol()).fromModel(client);
    }

    private boolean canUseJpaQuery(RealmModel realm, ClientSearchOptions searchOptions) {
        if (!canViewAll(realm)) {
            return false;
        }
        if (searchOptions == null || searchOptions.query() == null || searchOptions.query().isBlank()) {
            return false;
        }
        try {
            var filterContext = QueryParseUtils.parse(searchOptions.query());
            Set<String> queryFields = QueryFieldExtractor.extractFields(filterContext);
            return ClientJpaQuerySchema.JPA_FIELDS.containsAll(queryFields);
        } catch (ClientQueryException e) {
            return false;
        }
    }

    private void validateProjectionFields(ClientProjectionOptions projectionOptions) {
        projectionOptions.getFields().forEach(field -> {
            if (!MAPPERS.isKnownField(field)) {
                throw new ServiceException("%s is an unknown field".formatted(field),
                        jakarta.ws.rs.core.Response.Status.BAD_REQUEST);
            }
        });
    }

    private Stream<BaseClientRepresentation> applyProjection(Stream<BaseClientRepresentation> stream,
                                                             ClientProjectionOptions projectionOptions) {
        if (projectionOptions.getFields().isEmpty()) {
            return stream;
        }
        return stream.map(rep -> {
            MAPPERS.applyProjection(rep, projectionOptions.getFields());
            return rep;
        });
    }

    @Override
    public Stream<BaseClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions) {
        return delegate.deleteClients(realm, searchOptions);
    }

    @Override
    public void deleteClient(RealmModel realm, String clientId) throws ServiceException {
        delegate.deleteClient(realm, clientId);
    }

    @Override
    public CreateOrUpdateResult createOrUpdateClient(RealmModel realm, String clientId, BaseClientRepresentation client)
            throws ServiceException {
        return delegate.createOrUpdateClient(realm, clientId, client);
    }

    @Override
    public BaseClientRepresentation createClient(RealmModel realm, BaseClientRepresentation client) throws ServiceException {
        return delegate.createClient(realm, client);
    }

    @Override
    public BaseClientRepresentation patchClient(RealmModel realm, String clientId, PatchType patchType, InputStream patch)
            throws ServiceException {
        return delegate.patchClient(realm, clientId, patchType, patch);
    }
}
