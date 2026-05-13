package org.keycloak.scim.model.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.keycloak.Config;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationStorageProvider;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.models.jpa.entities.UserRoleMappingEntity;
import org.keycloak.scim.filter.FilterUtils;
import org.keycloak.scim.filter.ScimFilterParser.FilterContext;
import org.keycloak.scim.model.filter.ScimAttributeJpaExpressionResolver;
import org.keycloak.scim.model.filter.ScimJPAPredicateEvaluator;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.spi.AbstractScimResourceTypeProvider;
import org.keycloak.scim.resource.user.User;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.userprofile.ValidationException.Error;
import org.keycloak.utils.StringUtil;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public class UserResourceTypeProvider extends AbstractScimResourceTypeProvider<UserModel, User> implements ScimAttributeJpaExpressionResolver {

    public UserResourceTypeProvider(KeycloakSession session) {
        super(session, new UserCoreModelSchema(session), List.of(new UserEnterpriseModelSchema(session), new UserExtensionModelSchema(session)));
    }

    @Override
    public String getDescription() {
        return "User Account";
    }

    @Override
    public User onCreate(User resource) {
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        String userName = resource.getUserName();

        if (userName == null) {
            throw new ModelValidationException("username is required");
        }

        UserProfile profile = provider.create(UserProfileContext.SCIM, Map.of(UserModel.USERNAME, userName));
        UserModel model = profile.create(false);

        populate(model, resource);

        try {
            profile = provider.create(UserProfileContext.SCIM, model);
            profile.validate();
        } catch (ValidationException ve) {
            throw handleValidationException(ve);
        }

        resource.setCreatedTimestamp(model.getCreatedTimestamp());
        resource.setLastModifiedTimestamp(model.getLastModifiedTimestamp());

        return resource;
    }

    @Override
    protected User onUpdate(UserModel model, User resource) {
        try {
            UserProfileProvider userProfileProvider = session.getProvider(UserProfileProvider.class);
            UserProfile profile = userProfileProvider.create(UserProfileContext.SCIM, model);
            profile.update();
        } catch (ValidationException ve) {
            throw handleValidationException(ve);
        }

        model.setLastModifiedTimestamp(Time.currentTimeMillis());
        resource.setCreatedTimestamp(model.getCreatedTimestamp());
        resource.setLastModifiedTimestamp(model.getLastModifiedTimestamp());

        return resource;
    }

    @Override
    protected UserModel getModel(String id) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, id);

        if (user != null && AdminUserUtils.isAdminUser(session, realm, user)) {
            return null;
        }

        return user;
    }

    @Override
    protected String getRealmResourceType() {
        return AdminPermissionsSchema.USERS_RESOURCE_TYPE;
    }

    @Override
    protected Stream<UserModel> getModels(SearchRequest searchRequest) {
        RealmModel realm = session.getContext().getRealm();
        Integer firstResult = searchRequest.getStartIndex() != null ? searchRequest.getStartIndex() - 1 : null;
        Integer maxResults = searchRequest.getCount();
        maxResults = maxResults != null ? Math.min(maxResults, DEFAULT_MAX_RESULTS) : DEFAULT_MAX_RESULTS;

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserEntity> query = cb.createQuery(UserEntity.class);
        Root<UserEntity> root = query.from(UserEntity.class);

        FilterContext filterContext = StringUtil.isNotBlank(searchRequest.getFilter())
                ? FilterUtils.parseFilter(searchRequest.getFilter())
                : null;

        List<Predicate> predicates = getUserPredicates(filterContext, cb, query, root);

        query.where(predicates).distinct(true).orderBy(cb.asc(root.get("username")));

        return closing(paginateQuery(em.createQuery(query), firstResult, maxResults).getResultStream()
                .map(entity -> new UserAdapter(session, realm, em, entity)));
    }

    @Override
    public Long count(SearchRequest searchRequest) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<UserEntity> root = query.from(UserEntity.class);

        FilterContext filterContext = StringUtil.isNotBlank(searchRequest.getFilter())
                ? FilterUtils.parseFilter(searchRequest.getFilter())
                : null;

        List<Predicate> predicates = getUserPredicates(filterContext, cb, query, root);
        query.select(cb.countDistinct(root)).where(predicates);
        return em.createQuery(query).getSingleResult();
    }

    @Override
    public Class<User> getResourceType() {
        return User.class;
    }

    @Override
    public boolean onDelete(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.users().removeUser(realm, getModel(id));
    }

    @Override
    public void close() {

    }

    private ModelValidationException handleValidationException(ValidationException ve) {
        List<Error> errors = ve.getErrors();

        if (errors.isEmpty()) {
            throw new ModelValidationException(ve.getMessage());
        }

        Error firstError = errors.get(0);
        ModelValidationException exception = new ModelValidationException(firstError.getMessage());

        exception.setParameters(firstError.getMessageParameters());

        return exception;
    }

    private List<Predicate> getUserPredicates(FilterContext filterContext, CriteriaBuilder cb, CriteriaQuery<?> query, Root<UserEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (filterContext != null) {
            ScimJPAPredicateEvaluator evaluator = new ScimJPAPredicateEvaluator(this, getSchemas(), cb, root);
            predicates.add(evaluator.visit(filterContext).predicate());
        }

        predicates.add(root.get("serviceAccountClientLink").isNull());

        RealmModel realm = session.getContext().getRealm();
        predicates.add(cb.equal(root.get("realmId"), realm.getId()));

        predicates.add(excludeAdminUsers(cb, query, root, realm));

        UserProvider userProvider = session.getProvider(UserProvider.class, "jpa");
        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.USERS, (PartialEvaluationStorageProvider) userProvider, realm, cb, query, root));

        return predicates;
    }

    private Predicate excludeAdminUsers(CriteriaBuilder cb, CriteriaQuery<?> query, Root<UserEntity> root, RealmModel realm) {
        ClientModel adminClient;

        if (realm.getName().equals(Config.getAdminRealm())) {
            adminClient = realm.getMasterAdminClient();
        } else {
            adminClient = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        }

        if (adminClient == null) {
            return cb.conjunction();
        }

        List<String> adminRoleIds = adminClient.getRolesStream()
                .map(RoleModel::getId)
                .collect(Collectors.toList());

        if (realm.getName().equals(Config.getAdminRealm())) {
            RoleModel adminRole = realm.getRole(AdminRoles.ADMIN);
            if (adminRole != null) {
                adminRoleIds.add(adminRole.getId());
            }
        }

        if (adminRoleIds.isEmpty()) {
            return cb.conjunction();
        }

        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<UserRoleMappingEntity> urm = subquery.from(UserRoleMappingEntity.class);
        subquery.select(cb.literal(1));
        subquery.where(
                cb.and(
                        cb.equal(urm.get("user").get("id"), root.get("id")),
                        urm.get("roleId").in(adminRoleIds)
                )
        );

        return cb.not(cb.exists(subquery));
    }

    @Override
    public Expression<?> getAttributeExpression(Attribute<?, ?> attribute, CriteriaBuilder cb, Root<?> root, BiFunction<Class<?>, Supplier<Join<?, ?>>, Join<?, ?>> joinResolver) {
        if ("groups".equals(attribute.getName())) {
            Join<?, ?> join = joinResolver.apply(UserGroupMembershipEntity.class, () -> root.join(UserGroupMembershipEntity.class));
            join.on(cb.equal(root.get("id"), join.get("user").get("id")));
            return join.get("groupId");
        }
        return null;
    }
}
