package org.keycloak.scim.model.user;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.scim.filter.FilterUtils;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.model.filter.AttributeInfo;
import org.keycloak.scim.model.filter.AttributeNameResolver;
import org.keycloak.scim.model.filter.ScimJPAPredicateEvaluator;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.spi.AbstractScimResourceTypeProvider;
import org.keycloak.scim.resource.user.User;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.userprofile.ValidationException.Error;
import org.keycloak.utils.StringUtil;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.scim.model.user.AbstractUserModelSchema.ANNOTATION_SCIM_SCHEMA;
import static org.keycloak.scim.model.user.AbstractUserModelSchema.ANNOTATION_SCIM_SCHEMA_ATTRIBUTE;
import static org.keycloak.utils.StreamsUtil.closing;

public class UserResourceTypeProvider extends AbstractScimResourceTypeProvider<UserModel, User> {

    public UserResourceTypeProvider(KeycloakSession session) {
        super(session, new UserCoreModelSchema(session), List.of(new UserEnterpriseModelSchema(session)));
    }

    @Override
    public User onCreate(User resource) {
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        UserProfile profile = provider.create(UserProfileContext.SCIM, Map.of(UserModel.USERNAME, resource.getUserName()));
        UserModel model = profile.create(false);

        populate(model, resource);

        try {
            profile = provider.create(UserProfileContext.SCIM, model);
            profile.validate();
        } catch (ValidationException ve) {
            throw handleValidationException(ve);
        }

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

        return resource;
    }

    @Override
    protected UserModel getModel(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.users().getUserById(realm, id);
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

        if (StringUtil.isNotBlank(searchRequest.getFilter())) {
            // Parse filter into AST
            ScimFilterParser.FilterContext filterContext = FilterUtils.parseFilter(searchRequest.getFilter());

            // Execute JPA query with filter
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserEntity> query = cb.createQuery(UserEntity.class);
            Root<UserEntity> root = query.from(UserEntity.class);

            // Create filter predicate using the same query and root that will be used for execution
            ScimJPAPredicateEvaluator evaluator = new ScimJPAPredicateEvaluator(new UserAttributeNameResolver(session, this), cb, query, root);
            Predicate filterPredicate = evaluator.visit(filterContext).predicate();

            // Apply realm restriction
            Predicate realmPredicate = cb.equal(root.get("realmId"), realm.getId());

            // Combine with filter predicate
            query.where(cb.and(realmPredicate, filterPredicate));

            // Execute query and convert to UserModel stream
            return closing(paginateQuery(em.createQuery(query), firstResult, maxResults).getResultStream()
                    .map(entity -> new UserAdapter(session, realm, em, entity)));
        } else {
            return session.users().searchForUserStream(realm, Map.of(), firstResult, maxResults);
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

    private static class UserAttributeNameResolver implements AttributeNameResolver {

        private KeycloakSession session;
        private UserResourceTypeProvider provider;

        public UserAttributeNameResolver(KeycloakSession session, UserResourceTypeProvider provider) {
            this.session = session;
            this.provider = provider;
        }

        @Override
        public AttributeInfo resolve(String scimAttrPath) {

            // first split the attribute path into schema and attribute name. If no schema is specified, use the core user schema by default
            String[] splitAttrPath = provider.splitScimAttribute(scimAttrPath);

            // iterate through user profile attributes, finding one whose scim.schema.attribute annotation matches the given scimAttrPath
            Attributes attributes = session.getProvider(UserProfileProvider.class).create(UserProfileContext.SCIM, Map.of()).getAttributes();
            Set<String> allAttrNames = attributes.toMap().keySet();
            for (String attrName : allAttrNames) {
                var annotations = attributes.getMetadata(attrName).getAnnotations();
                if (annotations != null) {
                    String scimAttr = (String) annotations.get(ANNOTATION_SCIM_SCHEMA_ATTRIBUTE);
                    String scimAttrSchema = (String) annotations.get(ANNOTATION_SCIM_SCHEMA);
                    if (splitAttrPath[0].equals(scimAttrSchema) && splitAttrPath[1].equals(scimAttr)) {
                        // we found the attribute with the matching SCIM attribute path and schema, so return it
                        boolean primary = Boolean.parseBoolean((String) annotations.get("primary"));
                        String attrType = (String) annotations.get("type");
                        return new AttributeInfo(attrName, primary, attrType);
                    }
                }
            }
            // haven't found the attribute in the user profile, so return null to indicate that this is an unknown attribute.
            return null;
        }
    }

}
