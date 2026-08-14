package org.keycloak.services.client;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.scim.filter.ScimFilterException;
import org.keycloak.scim.filter.ScimFilterParser.FilterContext;
import org.keycloak.services.PatchType;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.query.ClientQueryException;
import org.keycloak.services.client.query.QueryFieldExtractor;
import org.keycloak.services.client.query.QueryParseUtils;
import org.keycloak.services.client.scim.BaseClientModelSchema;
import org.keycloak.services.client.scim.ClientJpaQueryExecutor;
import org.keycloak.services.client.scim.OIDCClientModelSchema;
import org.keycloak.services.client.scim.SAMLClientModelSchema;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.utils.StringUtil;

public class ScimBackedClientService implements ClientService {

    private static final Map<String, BaseClientModelSchema<?>> SCHEMAS = Map.of(
            OIDCClientRepresentation.PROTOCOL, OIDCClientModelSchema.INSTANCE,
            SAMLClientRepresentation.PROTOCOL, SAMLClientModelSchema.INSTANCE);

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
        FilterContext filterContext;
        try {
            filterContext = parseQuery(searchOptions);
        } catch (ClientQueryException e) {
            return delegate.getClients(realm, projectionOptions, searchOptions, sortAndSliceOptions);
        }
        if (!canUseJpaQuery(realm, filterContext)) {
            return delegate.getClients(realm, projectionOptions, searchOptions, sortAndSliceOptions);
        }

        permissions.clients().requireList();
        validateProjectionFields(projectionOptions);

        int offset = sortAndSliceOptions.offset();
        int limit = sortAndSliceOptions.limit();

        try {
            if (filterContext != null) {
                QueryParseUtils.validate(filterContext);
            }

            Set<String> includeFields = projectionOptions.getFields();
            List<String> includeList = includeFields.isEmpty() ? null : includeFields.stream().toList();
            Stream<BaseClientRepresentation> stream = ClientJpaQueryExecutor.findClients(
                            session, realm, filterContext, sortAndSliceOptions.getSortOptions(), offset, limit)
                    .<BaseClientRepresentation>map(client -> {
                        BaseClientModelSchema<?> schema = SCHEMAS.get(client.getProtocol());
                        if (schema == null) return null;
                        return populateFromSchema(schema, client, includeList);
                    })
                    .filter(Objects::nonNull);

            return stream;
        } catch (ClientQueryException | ScimFilterException e) {
            throw new ServiceException(e.getMessage(), Status.BAD_REQUEST);
        } catch (ModelException e) {
            throw new ServiceException(e.getMessage(), Status.BAD_REQUEST);
        }
    }

    private FilterContext parseQuery(ClientSearchOptions searchOptions) {
        if (searchOptions == null || StringUtil.isBlank(searchOptions.query())) {
            return null;
        }
        return QueryParseUtils.parse(searchOptions.query());
    }

    private boolean canViewAll(RealmModel realm) {
        return AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm) || permissions.clients().canView();
    }

    private boolean canUseJpaQuery(RealmModel realm, FilterContext filterContext) {
        if (!canViewAll(realm)) {
            return false;
        }
        if (filterContext == null) {
            return true;
        }
        Set<String> queryFields = QueryFieldExtractor.extractFields(filterContext);
        return BaseClientModelSchema.JPA_FIELDS.containsAll(queryFields);
    }

    private static <R extends BaseClientRepresentation> R populateFromSchema(
            BaseClientModelSchema<R> schema, ClientModel client, List<String> includeFields) {
        R rep = schema.createRepresentation();
        schema.populate(rep, client, includeFields, null);
        return rep;
    }

    // TODO: still need to have well defined handling for polymorphic fields
    private void validateProjectionFields(ClientProjectionOptions projectionOptions) {
        projectionOptions.getFields().forEach(field -> {
            if (SCHEMAS.values().stream().noneMatch(s -> s.getAttributes().containsKey(field))) {
                throw new ServiceException("%s is an unknown field".formatted(field), Status.BAD_REQUEST);
            }
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
