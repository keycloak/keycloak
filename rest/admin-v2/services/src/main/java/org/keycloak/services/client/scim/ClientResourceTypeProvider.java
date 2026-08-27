package org.keycloak.services.client.scim;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.ws.rs.core.Response;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.events.admin.v2.AdminEventV2Builder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.scim.model.filter.ScimAttributeJpaExpressionResolver;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.spi.BaseResourceTypeProvider;
import org.keycloak.services.ServiceException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientUnregisterContext;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

/*
 * TODO: the static methods rely upon static schema definitions, which is something we may eventually want to make stateful 
 */
public class ClientResourceTypeProvider extends BaseResourceTypeProvider<ClientModel, BaseClientRepresentation> implements ScimAttributeJpaExpressionResolver {

    private final static Map<String, BaseClientModelSchema<? extends BaseClientRepresentation>> SCHEMAS = Map.of(
            OIDCClientRepresentation.PROTOCOL, OIDCClientModelSchema.INSTANCE,
            SAMLClientRepresentation.PROTOCOL, SAMLClientModelSchema.INSTANCE);
    
    private final AdminPermissionEvaluator permissions;
    private final AdminEventBuilder adminEventBuilder;
    private final RealmModel realm;
    
    public ClientResourceTypeProvider(KeycloakSession session) {
        super(session, SCHEMAS.values().stream());
        this.realm = session.getContext().getRealm();
        var authInfo = AdminRoot.authenticateRealmAdminRequest(session);
        session.getContext().setRealm(realm); // authenticateRealmAdminRequest clears the context realm
        this.permissions = AdminPermissions.evaluator(session, realm, authInfo);
        this.adminEventBuilder = new AdminEventV2Builder(realm, permissions.adminAuth(), session, session.getContext().getConnection()).resource(ResourceType.CLIENT);
    }

    public Map<String, BaseClientModelSchema<? extends BaseClientRepresentation>> getSchemaMap() {
        return SCHEMAS;
    }

    @Override
    public void close() {
        // TODO: v2 may not need these classes to be Providers
    }

    @Override
    public String getSchema() {
        throw new UnsupportedOperationException("not needed by v2, and does not seem to work well with polymorphic types");
    }

    @Override
    public Class<BaseClientRepresentation> getResourceType() {
        throw new UnsupportedOperationException("not needed by v2");
    }

    @Override
    public Long count(SearchRequest searchRequest, int resourceSize) {
        throw new UnsupportedOperationException("not needed by v2");
    }

    @Override
    public Expression<?> getAttributeExpression(Attribute<?, ?> attribute, CriteriaBuilder cb, Root<?> root,
            BiFunction<Class<?>, Supplier<Join<?, ?>>, Join<?, ?>> joinResolver) {
        if ("roles".equals(attribute.getName())) {
            Join<?, ?> join = joinResolver.apply(RoleEntity.class, () -> root.join(RoleEntity.class));
            join.on(cb.equal(root.get("id"), join.get("clientId")));
            return join.get("name");
        } else if ("auth.method".equals(attribute.getName())) {
            return cb.selectCase().when(cb.and(cb.equal(root.get("protocol"), OIDCClientRepresentation.PROTOCOL),
                    cb.isFalse(root.get("publicClient"))), root.get("clientAuthenticatorType"));
        }
        return null;
    }

    @Override
    protected String getModelId(BaseClientRepresentation resource) {
        return resource.getClientId();
    }

    @Override
    protected BaseClientRepresentation onCreate(BaseClientRepresentation resource) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    protected BaseClientRepresentation onUpdate(ClientModel model, BaseClientRepresentation resource) {
        throw new UnsupportedOperationException("TODO");
    }
    
    @Override
    protected boolean hasPermission(ClientModel model, String realmResourceType, String scope) {
        // TODO: session.getContext().getPermissions() expects the context
        // to have the auth realm set
        // however subsequent v2 logic expects to have the context realm set
        session.getContext().setRealm(permissions.adminAuth().getRealm());
        try {
            return super.hasPermission(model, realmResourceType, scope);
        } finally {
            session.getContext().setRealm(realm);
        }
    }

    @Override
    protected boolean onDelete(ClientModel client) {
        // TODO: the scim rest layer is handling the null check - seems like this should be in a common place
        if (client == null) {
            throw new ServiceException("Could not find client", Response.Status.NOT_FOUND);
        }

        try {
            session.clientPolicy().triggerOnEvent(new AdminClientUnregisterContext(client, permissions.adminAuth()));
        } catch (ClientPolicyException e) {
            throw new ServiceException(e.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        var clientRepresentation = Optional.ofNullable(getSchemaMap().get(client.getProtocol())).map(s -> s.fromModel(client))
                .orElseThrow(() -> new ServiceException("Cannot map client model", Response.Status.BAD_REQUEST));

        if (new ClientManager(new RealmManager(session)).removeClient(realm, client)) {
            fireAdminEvent(OperationType.DELETE, clientRepresentation);
        } else {
            throw new ServiceException("Could not delete client", Response.Status.BAD_REQUEST);
        }
        
        return true; // TODO: v2 throws a ServiceException rather than returning false - that provides a proper message
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

    @Override
    protected Stream<ClientModel> getModels(SearchRequest searchRequest) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    protected ClientModel getModel(String id) {
        return realm.getClientByClientId(id);
    }

    @Override
    protected String getRealmResourceType() {
        return AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE;
    }

    @Override
    protected void populate(ClientModel model, BaseClientRepresentation resource) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    protected BaseClientRepresentation createResourceTypeInstance(ClientModel model, List<String> attributes,
            List<String> excludedAttributes) {
        throw new UnsupportedOperationException("TODO");
    }

    public static boolean isKnownField(String fieldPath) {
        return SCHEMAS.values().stream().anyMatch(schema -> schema.getAttributeByPath(fieldPath) != null);
    }

    @SuppressWarnings("unchecked")
    public static Object resolveField(String fieldPath, BaseClientRepresentation client) {
        String protocol = client.getProtocol();
        BaseClientModelSchema schema = protocol != null ? SCHEMAS.get(protocol) : null;
        if (schema != null) {
            return schema.getRepresentationValue(client, fieldPath);
        }
        return null;
    }

    public static BaseClientRepresentation fromModel(ClientModel client) {
        String protocol = client.getProtocol();
        BaseClientModelSchema<?> schema = protocol != null ? SCHEMAS.get(protocol) : null;
        if (schema != null) {
            return schema.fromModel(client);
        }
        return null;
    }

}
