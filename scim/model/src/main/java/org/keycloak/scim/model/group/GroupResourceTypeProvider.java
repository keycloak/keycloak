/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.scim.model.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.Type;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.Permissions;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.model.filter.ScimAttributeJpaExpressionResolver;
import org.keycloak.scim.model.filter.ScimJPAPredicateEvaluator;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.group.Member;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.spi.AbstractScimResourceTypeProvider;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public class GroupResourceTypeProvider extends AbstractScimResourceTypeProvider<GroupModel, Group> implements ScimAttributeJpaExpressionResolver {

    public GroupResourceTypeProvider(KeycloakSession session) {
        super(session, new GroupCoreModelSchema(session));
    }

    @Override
    public Group onCreate(Group group) {
        RealmModel realm = session.getContext().getRealm();
        GroupModel model = session.groups().createGroup(realm, group.getDisplayName());
        populate(model, group);
        group.setCreatedTimestamp(model.getCreatedTimestamp());
        group.setLastModifiedTimestamp(model.getLastModifiedTimestamp());
        return group;
    }

    @Override
    protected Group createResourceTypeInstance(GroupModel model, List<String> attributes, List<String> excludedAttributes) {
        if (session.getContext().getPermissions().isAdminGroup(model)) {
            Group group = new Group();

            group.addSchema(getSchema());
            group.setId(model.getId());
            group.setDisplayName(model.getName());

            return group;
        }

        return super.createResourceTypeInstance(model, attributes, excludedAttributes);
    }

    @Override
    public Group update(Group resource) {
        List<Member> members = resource.getMembers();

        if (!Optional.ofNullable(members).orElse(List.of()).isEmpty()) {
            throw new ModelValidationException("Managing members on updates is not supported");
        }

        return super.update(resource);
    }

    @Override
    protected Group onUpdate(GroupModel model, Group resource) {
        model.setLastModifiedTimestamp(Time.currentTimeMillis());
        resource.setCreatedTimestamp(model.getCreatedTimestamp());
        resource.setLastModifiedTimestamp(model.getLastModifiedTimestamp());
        return resource;
    }

    @Override
    protected GroupModel getModel(String id) {
        RealmModel realm = session.getContext().getRealm();
        GroupModel model = session.groups().getGroupById(realm, id);

        if (model == null || Type.REALM.equals(model.getType())) {
            return model;
        }

        return null;
    }

    @Override
    protected String getRealmResourceType() {
        return AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
    }

    @Override
    protected Stream<GroupModel> getModels(SearchRequest searchRequest) {
        RealmModel realm = session.getContext().getRealm();
        Integer firstResult = searchRequest.getStartIndex() != null ? searchRequest.getStartIndex() - 1 : null;
        Integer maxResults = searchRequest.getCount();
        maxResults = maxResults != null ? Math.max(0, Math.min(maxResults, DEFAULT_MAX_RESULTS)) : DEFAULT_MAX_RESULTS;

        ScimFilterParser.FilterContext filterContext = searchRequest.getFilterContext();

        if (filterContext != null) {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<GroupEntity> query = cb.createQuery(GroupEntity.class);
            Root<GroupEntity> root = query.from(GroupEntity.class);
            List<Predicate> predicates = getGroupPredicates(filterContext, cb, query, root);

            // apply distinct and order by name to ensure consistency with no-filter case
            query.where(predicates).distinct(true).orderBy(cb.asc(root.get("name")));

            return closing(paginateQuery(em.createQuery(query), firstResult, maxResults).getResultStream()
                    .map(entity -> session.groups().getGroupById(realm, entity.getId()))
                    .filter(Objects::nonNull));
        } else {
            return session.groups().getTopLevelGroupsStream(realm, firstResult, maxResults);
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
            Root<GroupEntity> root = query.from(GroupEntity.class);

            List<Predicate> predicates = this.getGroupPredicates(filterContext, cb, query, root);
            query.select(cb.countDistinct(root)).where(predicates);
            return em.createQuery(query).getSingleResult();
        } else {
            return session.groups().getGroupsCount(realm, true);
        }
    }

    @Override
    public boolean onDelete(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.groups().removeGroup(realm, getModel(id));
    }

    @Override
    public Class<Group> getResourceType() {
        return Group.class;
    }

    @Override
    public void close() {
    }

    private List<Predicate> getGroupPredicates(ScimFilterParser.FilterContext filterContext, CriteriaBuilder cb, CriteriaQuery<?> query, Root<GroupEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        RealmModel realm = session.getContext().getRealm();
        Permissions permissions = session.getContext().getPermissions();

        // When FGAP is enabled, only the eq operator is supported for members.value filters. The callback
        // verifies that the caller has VIEW permission on the specific user being matched. Other operators
        // (ne, pr, gt, co, etc.) cannot be safely authorized through value comparison because they can match
        // rows the caller is not permitted to see, so they silently return empty results for this path.
        // This restriction only applies to the members.value/members paths; all other filter attributes are
        // unaffected. When FGAP is disabled, all operators are allowed.
        BiPredicate<String, String> authCheck = (path, value) -> {
            if ("members.value".equalsIgnoreCase(path) || "members".equalsIgnoreCase(path)) {
                if (!realm.isAdminPermissionsEnabled()) {
                    return true;
                }
                if (value == null) {
                    return false;
                }
                UserModel user = session.users().getUserById(realm, value);
                return user != null && permissions.hasPermission(user, AdminPermissionsSchema.USERS_RESOURCE_TYPE, AdminPermissionsSchema.VIEW);
            }
            return true;
        };

        // create filter predicate using the same query and root that will be used for execution
        ScimJPAPredicateEvaluator evaluator = new ScimJPAPredicateEvaluator(this, getSchemas(), cb, root, authCheck);
        predicates.add(evaluator.visit(filterContext).predicate());

        // apply realm restriction and group type restrictions
        predicates.add(cb.equal(root.get("realm"), realm.getId()));
        predicates.add(cb.equal(root.get("type"), GroupModel.Type.REALM.intValue()));
        predicates.add(cb.equal(root.get("parentId"), GroupEntity.TOP_PARENT_ID));

        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, realm, cb, query, root));

        return predicates;
    }

    @Override
    public Expression<?> getAttributeExpression(Attribute<?, ?> attribute, CriteriaBuilder cb, Root<?> root, BiFunction<Class<?>, Supplier<Join<?, ?>>, Join<?, ?>> joinResolver) {
        if ("members".equals(attribute.getName())) {
            Join<?, ?> join = joinResolver.apply(UserGroupMembershipEntity.class, () -> root.join(UserGroupMembershipEntity.class));
            join.on(cb.equal(root.get("id"), join.get("groupId")));
            return join.get("user").get("id");
        }
        return null;
    }

    @Override
    protected boolean isManageable(GroupModel model) {
        return !session.getContext().getPermissions().isAdminGroup(model);
    }
}
