package org.keycloak.services.client.scim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.validation.groups.Default;
import jakarta.ws.rs.core.Response;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.support.EntityManagers;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.events.admin.v2.AdminEventV2Builder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCClientSecretConfigWrapper;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.representations.admin.v2.validation.CreateClient;
import org.keycloak.representations.admin.v2.validation.PatchClient;
import org.keycloak.representations.admin.v2.validation.PutClient;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.scim.filter.ScimFilterException;
import org.keycloak.scim.model.filter.ScimAttributeJpaExpressionResolver;
import org.keycloak.scim.model.filter.ScimJPAPredicateEvaluator;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.spi.BaseResourceTypeProvider;
import org.keycloak.scim.resource.spi.SearchOptions;
import org.keycloak.scim.resource.spi.SortField;
import org.keycloak.services.RolesService;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientSortField;
import org.keycloak.services.client.SimpleClientModel;
import org.keycloak.services.client.query.ClientQueryException;
import org.keycloak.services.client.query.QueryParseUtils;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientRegisteredContext;
import org.keycloak.services.clientpolicy.context.AdminClientUnregisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdatedContext;
import org.keycloak.services.clientpolicy.context.ClientModelContext;
import org.keycloak.services.clientpolicy.context.ClientSecretRotationContext;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.RoleContainerResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.validation.ValidationUtil;
import org.keycloak.validation.jakarta.HibernateValidatorProvider;
import org.keycloak.validation.jakarta.JakartaValidatorProvider;
import org.keycloak.validation.jakarta.ValidationContext;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.representations.admin.v2.validators.ClientSecretNotBlankValidator.isClientSecret;
import static org.keycloak.utils.StreamsUtil.closing;
import static org.keycloak.utils.StringUtil.isBlank;

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
    private final JakartaValidatorProvider validator;
    private final RolesService rolesService;
    
    public ClientResourceTypeProvider(KeycloakSession session) {
        super(session, SCHEMAS.values().stream());
        this.realm = session.getContext().getRealm();
        var authInfo = AdminRoot.authenticateRealmAdminRequest(session);
        session.getContext().setRealm(realm); // authenticateRealmAdminRequest clears the context realm
        this.permissions = AdminPermissions.evaluator(session, realm, authInfo);
        this.adminEventBuilder = new AdminEventV2Builder(realm, permissions.adminAuth(), session, session.getContext().getConnection()).resource(ResourceType.CLIENT);
        this.validator = new HibernateValidatorProvider(new ValidationContext(session, realm));
        this.rolesService = new RolesService(session, realm, permissions, adminEventBuilder);
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
    public Long count(SearchOptions searchRequest, int resourceSize) {
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
    protected BaseClientRepresentation onCreate(BaseClientRepresentation client) {
        return onCreateOrUpdate(client, null, "PUT".equals(getHttpMethod()) ?  PutClient.class : CreateClient.class);
    }

    @Override
    protected BaseClientRepresentation onUpdate(ClientModel model, BaseClientRepresentation client) {
        return onCreateOrUpdate(client, model, "PATCH".equals(getHttpMethod()) ? PatchClient.class : PutClient.class);
    }
    
    private String getHttpMethod() {
        // TODO: based upon the assumption that this class is used only from the v2 endpoints
        // if that assumption ever breaks, we'll either need to provide this in the method signatures (changing the scim
        // contract) or introduce a thread local to differentiate the behavior
        return session.getContext().getHttpRequest().getHttpMethod();
    }
    
    private BaseClientRepresentation onCreateOrUpdate(BaseClientRepresentation client, ClientModel model, Class<?> context) {
        boolean create = model == null;
        validateUnknownFields(client);
        if (isBlank(client.getProtocol())) {
            throw new ServiceException("protocol is required", Response.Status.BAD_REQUEST);
        }
        BaseClientModelSchema schema = getSchema(client.getProtocol());
        if (create && context == PutClient.class && client.getUuid() != null && realm.getClientById(client.getUuid()) != null) {
            throw new ServiceException("uuid already exists, but with a different clientId", Response.Status.BAD_REQUEST);
        }
        if (!create && !Objects.equals(model.getProtocol(), client.getProtocol())) {
            // TODO: duplicates the validation logic, but needs to be done here because the class type is expected to match the protocol
            // an alternative would be to make the FieldResolver use the class, rather than the protocol to get the schema
            throw new ServiceException("protocol cannot be changed for an existing client", Response.Status.BAD_REQUEST);
        }
        validator.validate(client, context, Default.class);
        var proposedRepresentation = getProposedOldRepresentation(realm, client, schema);
        if (client instanceof SAMLClientRepresentation samlClient) {
            proposedRepresentation.setStandardFlowEnabled(null);
            proposedRepresentation.setFrontchannelLogout(samlClient.getFrontChannelLogout());
        }
        
        try {
            if (create) {
                session.clientPolicy().triggerOnEvent(new AdminClientRegisterContext(proposedRepresentation, permissions.adminAuth()));
                // Add basic attributes
                model = realm.addClient(client.getClientId());
                model.setProtocol(client.getProtocol());
            }
    
            // Generate random secret if applicable
            String currentSecret = generateClientSecretIfNeeded(client, model, context == PatchClient.class);
            if (!create) {
                session.clientPolicy().triggerOnEvent(new AdminClientUpdateContext(proposedRepresentation, model, permissions.adminAuth()));
            }
            schema.populate(model, client);
            if (create) {
                setupClientDefaults(client, model, proposedRepresentation);
            }
    
            // Validate the fully populated model
            ValidationUtil.validateClient(session, model, create, r -> {
                session.getTransactionManager().setRollbackOnly();
                throw new ServiceException(r.getAllErrorsAsString(), Response.Status.BAD_REQUEST);
            });
            
            model.updateClient(); // this is fired for both create and update in RepresentationToModel

            if (create) {
                session.clientPolicy().triggerOnEvent(new AdminClientRegisteredContext(model, permissions.adminAuth()));
            } else {
                ClientModelContext updatedContext = currentSecret != null
                        ? new ClientSecretRotationContext(proposedRepresentation, model, currentSecret, permissions.adminAuth())
                        : new AdminClientUpdatedContext(proposedRepresentation, model, permissions.adminAuth());
                session.clientPolicy().triggerOnEvent(updatedContext);

                if (!Boolean.TRUE.equals(session.removeAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED))) {
                    OIDCClientSecretConfigWrapper.fromClientModel(model).removeClientSecretRotationInfo();
                }
            }
                    
            // Setup roles
            var clientRoles = rolesService.resource(model);
            handleRoles(clientRoles, client.getRoles());
    
            // OIDC specific
            if (client instanceof OIDCClientRepresentation oidcClient) {
                handleServiceAccount(model, oidcClient);
            }
    
            EntityManagers.flush(session, false); // flush to ensure the timestamps are updated
            fireAdminEvent(create ? OperationType.CREATE : OperationType.UPDATE, schema.fromModel(model));
            return schema.fromModel(model);
        } catch (ClientPolicyException e) {
            throw new ServiceException(e.getErrorDetail(), Response.Status.BAD_REQUEST);
        }
    }
    
    private void setupClientDefaults(BaseClientRepresentation client, ClientModel model, ClientRepresentation proposedRepresentation) {
        LoginProtocolFactory factory = (LoginProtocolFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(LoginProtocol.class, client.getProtocol());
        if (factory != null) {
            factory.setupClientDefaults(proposedRepresentation, model);
        }
        if (client instanceof OIDCClientRepresentation oidcClient
                && oidcClient.getAuth() != null
                && !isClientSecret(oidcClient.getAuth().getMethod())
                && isBlank(oidcClient.getAuth().getSecret())) {
            // OIDCLoginProtocolFactory generates a secret for every confidential client, while Admin API v2
            // only uses secrets with secret-based authentication methods.
            model.setSecret(null);
        }
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
    protected Stream<ClientModel> getModels(SearchOptions searchOptions) {
        permissions.clients().requireList();
        if (!AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(realm) && !permissions.clients().canView()) {
            // TODO: this requires memory based post processing, which defers the slicing operation
            // meaning that all clients could be iterated in the worst case - may be allowable only if we eventually allow
            // unlimited limit values
            throw new ForbiddenException(); 
        }
        
        // TODO: this validation could be pulled up to the rest layer and/or combinded with scim
        validateFields(searchOptions.getAttributes().stream(), ClientResourceTypeProvider::isKnownField, "%s is an unknown field");
        validateFields(searchOptions.getSort().stream().map(SortField::fieldName),
                field -> ClientSortField.fromApiName(field).isPresent(), "%s is not a sortable field");

        try {
            if (searchOptions.getFilterContext() != null) {
                QueryParseUtils.validate(searchOptions.getFilterContext());
            }
            
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ClientEntity> query = cb.createQuery(ClientEntity.class);
            Root<ClientEntity> root = query.from(ClientEntity.class);
            query.select(root);

            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("realmId"), realm.getId()));
            predicates.add(root.get("protocol").in(OIDCClientRepresentation.PROTOCOL, SAMLClientRepresentation.PROTOCOL));
            predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(
                    session, AdminPermissionsSchema.CLIENTS, realm, cb, query, root));

            ClientResourceTypeProvider provider = new ClientResourceTypeProvider(session);

            ScimJPAPredicateEvaluator evaluator = new ScimJPAPredicateEvaluator(
                    provider, provider.getSchemas(), cb, root);
            if (searchOptions.getFilterContext() != null) {
                predicates.add(evaluator.visit(searchOptions.getFilterContext()).predicate());
            }

            var q = query.where(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
            var orders = new ArrayList<>(searchOptions.getSort().stream().map(sortOption -> {
                String field = provider.getSchemas().stream()
                        .map(s -> s.getAttributeByPath(sortOption.fieldName()))
                        .map(Attribute::getModelAttributeName).findFirst().orElseThrow();
                return sortOption.order().isAscending() ? cb.asc(root.get(field)) : cb.desc(root.get(field));
            }).toList());

            // add default sort by clientId - TODO: does this need to be done for every query
            if (searchOptions.getSort().stream().noneMatch(sortOption -> "clientId".equals(sortOption.fieldName()))) {
                orders.add(cb.asc(root.get("clientId")));
            }

            q.orderBy(orders);

            return closing(paginateQuery(em.createQuery(q), searchOptions.getStartIndex(), searchOptions.getCount()).getResultStream()
                    // Resolve through the provider to preserve adapter augmentation behavior.
                    .map(clientEntity -> session.clients().getClientById(realm, clientEntity.getId()))
                    .filter(Objects::nonNull)); // TODO: none of the clients should be null, this is more indicative of a potential problem
        } catch (ClientQueryException | ScimFilterException e) {
            throw new ServiceException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (ModelException e) {
            throw new ServiceException(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }
    
    // TODO: still need to have well defined handling for polymorphic fields
    private void validateFields(Stream<String> fields, Predicate<String> fieldValidator, String message) {
        fields.forEach(field -> {
            if (!fieldValidator.test(field)) {
                throw new ServiceException(message.formatted(field), Response.Status.BAD_REQUEST);
            }
        });
    }
    
    @Override
    public ClientModel getModel(String id) {
        return realm.getClientByClientId(id);
    }

    @Override
    protected String getRealmResourceType() {
        return AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE;
    }

    @Override
    protected void populate(ClientModel model, BaseClientRepresentation resource) {
        // TODO: not currently used - see the logic in onCreateOrUpdate instead as the population is contextual. This may need to be refined later.
        // could also introduce more logic into BaseResourceTypeProvider to control when this is invoked
    }
    
    @Override
    protected BaseClientRepresentation createResourceTypeInstance(ClientModel model, List<String> attributes,
            List<String> excludedAttributes) {
        return populateFromSchema(getSchema(model.getProtocol()), model, attributes, excludedAttributes);
    }
    
    private static <R extends BaseClientRepresentation> R populateFromSchema(
            BaseClientModelSchema<R> schema, ClientModel client, List<String> includeFields, List<String> excludedFields) {
        R rep = schema.createRepresentation();
        schema.populate(rep, client, includeFields, excludedFields);
        return rep;
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
    
    /**
     * Creates a temporary client to convert BaseClientRepresentation to ClientRepresentation.
     * Required because client policy contexts expect ClientRepresentation (v1), but there's no
     * direct converter from BaseClientRepresentation (v2 API).
     * <p>
     * For more details, see the <a href="https://github.com/keycloak/keycloak/issues/47576">keycloak#47576</a>.
     */
    private ClientRepresentation getProposedOldRepresentation(RealmModel realm, BaseClientRepresentation client, BaseClientModelSchema schema) {
        String clientId = client.getClientId();
        SimpleClientModel tempModel = new SimpleClientModel("", realm);
        schema.populate(tempModel, client);
        var proposedRepresentation = ModelToRepresentation.toRepresentation(tempModel, session);
        proposedRepresentation.setClientId(clientId);
        proposedRepresentation.setId(null);
        return proposedRepresentation;
    }

    private String generateClientSecretIfNeeded(BaseClientRepresentation client, ClientModel model, boolean rotateSecret) {
        String currentSecret = null;
        if (client instanceof OIDCClientRepresentation oidcClient
                && OIDCClientRepresentation.PROTOCOL.equals(client.getProtocol())) {
            var auth = oidcClient.getAuth();
            if (auth != null && isClientSecret(auth.getMethod()) && isBlank(auth.getSecret())) {
                if (rotateSecret) {
                    currentSecret = model.getSecret(); // return current password for rotation
                    auth.setSecret(KeycloakModelUtils.generateSecret(model));
                } else {
                    // for non-rotation, only create the secret if it doesn't already exist
                    if (!isBlank(model.getSecret())) {
                        auth.setSecret(model.getSecret());
                    } else {
                        auth.setSecret(KeycloakModelUtils.generateSecret(model));
                    }
                }
            }
        }
        return currentSecret;
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
     * Applies mappings on the {@link UserModel} with the same permission checks as the Admin REST role-mapping resources, but without
     * routing through nested JAX-RS resources (which are not suited for in-process service calls).
     */
    protected void handleServiceAccount(ClientModel model, OIDCClientRepresentation rep) {
        boolean serviceAccountEnabled = rep.getLoginFlows().contains(OIDCClientRepresentation.Flow.SERVICE_ACCOUNT);

        ClientManager.updateClientServiceAccount(session, model, serviceAccountEnabled);

        if (!serviceAccountEnabled) {
            return;
        }

        UserModel serviceAccountUser = new ClientManager(new RealmManager(session)).getServiceAccountUser(model)
                .orElseThrow(() -> new ServiceException("Cannot find service account user", Response.Status.BAD_REQUEST));

        RealmModel realm = model.getRealm();
        Set<String> desiredRoleNames = Optional.ofNullable(rep.getServiceAccountRoles()).orElse(Collections.emptySet());
        Set<RoleModel> currentRoles = serviceAccountUser.getRoleMappingsStream().collect(Collectors.toSet());
        Set<String> currentRoleNames = currentRoles.stream().map(RoleModel::getName).collect(Collectors.toSet());

        // serviceAccountRoles are plain names; client roles on this client are resolved before realm roles (name collisions favor the client).
        List<RoleModel> rolesToAdd = new ArrayList<>();
        for (String roleName : desiredRoleNames) {
            if (currentRoleNames.contains(roleName)) {
                continue;
            }
            RoleModel clientRole = model.getRole(roleName);
            RoleModel resolved = clientRole != null ? clientRole : realm.getRole(roleName);
            if (resolved == null) {
                throw new ServiceException("Cannot assign role to the service account (field 'serviceAccount.roles') as it does not exist", Response.Status.BAD_REQUEST);
            }
            rolesToAdd.add(resolved);
        }

        List<RoleModel> rolesToRemove = new ArrayList<>();
        for (RoleModel role : currentRoles) {
            if (!desiredRoleNames.contains(role.getName())) {
                rolesToRemove.add(role);
            }
        }

        if (rolesToAdd.isEmpty() && rolesToRemove.isEmpty()) {
            return;
        }

        permissions.users().requireMapRoles(serviceAccountUser);
        for (RoleModel role : rolesToAdd) {
            permissions.roles().requireMapRole(role);
            serviceAccountUser.grantRole(role);
        }
        for (RoleModel role : rolesToRemove) {
            permissions.roles().requireMapRole(role);
            serviceAccountUser.deleteRoleMapping(role);
        }
    }

    protected void validateUnknownFields(BaseClientRepresentation rep) {
        if (!rep.getAdditionalFields().isEmpty()) {
            throw new ServiceException("Payload contains unknown fields: " + rep.getAdditionalFields().keySet(), Response.Status.BAD_REQUEST);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <R extends BaseClientRepresentation> BaseClientModelSchema<R> getSchema(String protocol) {
        BaseClientModelSchema<?> schema = getSchemaMap().get(protocol);
        if (schema == null) {
            throw new ServiceException("Schema not found, unsupported client protocol: " + protocol,
                    Response.Status.BAD_REQUEST);
        }
        return (BaseClientModelSchema<R>) schema;
    }
    
    public AdminPermissionEvaluator getPermissions() {
        return permissions;
    }

}
