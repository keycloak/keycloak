package org.keycloak.scim.model.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationStorageProvider;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.Permissions;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.scim.filter.ScimFilterParser;
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

        return createResourceTypeInstance(model, null, null);
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

        return createResourceTypeInstance(model, null, null);
    }

    @Override
    protected UserModel getModel(String id) {
        RealmModel realm = session.getContext().getRealm();
        UserModel model = session.users().getUserById(realm, id);

        if (model == null || model.getServiceAccountClientLink() == null) {
            return model;
        }

        return null;
    }

    @Override
    protected String getRealmResourceType() {
        return AdminPermissionsSchema.USERS_RESOURCE_TYPE;
    }

    @Override
    protected User createResourceTypeInstance(UserModel model, List<String> attributes, List<String> excludedAttributes) {
        if (session.getContext().getPermissions().isAdminUser(model)) {
            User user = new User();

            user.addSchema(getSchema());
            user.setId(model.getId());
            user.setUserName(model.getUsername());

            return user;
        }
        return super.createResourceTypeInstance(model, attributes, excludedAttributes);
    }

    @Override
    protected Stream<UserModel> getModels(SearchRequest searchRequest) {
        RealmModel realm = session.getContext().getRealm();
        Integer firstResult = searchRequest.getStartIndex() != null ? searchRequest.getStartIndex() - 1 : null;
        Integer maxResults = searchRequest.getCount();
        maxResults = maxResults != null ? Math.max(0, Math.min(maxResults, DEFAULT_MAX_RESULTS)) : DEFAULT_MAX_RESULTS;

        ScimFilterParser.FilterContext filterContext = searchRequest.getFilterContext();

        if (filterContext != null) {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserEntity> query = cb.createQuery(UserEntity.class);
            Root<UserEntity> root = query.from(UserEntity.class);

            List<Predicate> predicates = getUserPredicates(filterContext, cb, query, root);

            query.where(predicates).distinct(true).orderBy(cb.asc(root.get("username")));

            return closing(paginateQuery(em.createQuery(query), firstResult, maxResults).getResultStream()
                    .map(entity -> session.users().getUserById(realm, entity.getId()))
                    .filter(Objects::nonNull));
        } else {
            return session.users().searchForUserStream(realm, Map.of(UserModel.INCLUDE_SERVICE_ACCOUNT, "false"), firstResult, maxResults);
        }
    }

    @Override
    public Long count(SearchRequest searchRequest) {
        RealmModel realm = session.getContext().getRealm();
        ScimFilterParser.FilterContext filterContext = searchRequest.getFilterContext();

        if (filterContext != null) {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> query = cb.createQuery(Long.class);
            Root<UserEntity> root = query.from(UserEntity.class);

            List<Predicate> predicates = this.getUserPredicates(filterContext, cb, query, root);
            query.select(cb.countDistinct(root)).where(predicates);
            return em.createQuery(query).getSingleResult();
        } else {
            return (long) session.users().getUsersCount(realm, false);
        }
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

        RealmModel realm = session.getContext().getRealm();
        Permissions permissions = session.getContext().getPermissions();

        // When FGAP is enabled, only the eq operator is supported for groups.value filters. The callback
        // verifies that the caller has VIEW permission on the specific group being matched. Other operators
        // (ne, pr, gt, co, etc.) cannot be safely authorized through value comparison because they can match
        // rows the caller is not permitted to see, so they silently return empty results for this path.
        // This restriction only applies to the groups.value/groups paths; all other filter attributes are
        // unaffected. When FGAP is disabled, all operators are allowed.
        BiPredicate<String, String> authCheck = (path, value) -> {
            if ("groups.value".equalsIgnoreCase(path) || "groups".equalsIgnoreCase(path)) {
                if (!realm.isAdminPermissionsEnabled()) {
                    return true;
                }
                if (value == null) {
                    return false;
                }
                GroupModel group = session.groups().getGroupById(realm, value);
                return group != null && permissions.hasPermission(group, AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, AdminPermissionsSchema.VIEW);
            }
            return true;
        };

        // create filter predicate using the same query and root that will be used for execution
        ScimJPAPredicateEvaluator evaluator = new ScimJPAPredicateEvaluator(this, getSchemas(), cb, root, authCheck);
        predicates.add(evaluator.visit(filterContext).predicate());

        // apply service account restriction
        predicates.add(root.get("serviceAccountClientLink").isNull());

        // apply realm restriction
        predicates.add(cb.equal(root.get("realmId"), realm.getId()));

        UserProvider userProvider = session.getProvider(UserProvider.class, "jpa");
        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.USERS, (PartialEvaluationStorageProvider) userProvider, realm, cb, query, root));

        return predicates;
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

    @Override
    protected boolean isManageable(UserModel model) {
        return !session.getContext().getPermissions().isAdminUser(model);
    }
}
