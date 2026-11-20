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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

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
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.protocol.request.PatchRequest.PatchOperation;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.group.Member;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.schema.path.Path;
import org.keycloak.scim.resource.spi.AbstractScimResourceTypeProvider;
import org.keycloak.scim.resource.spi.MembershipChange;
import org.keycloak.scim.resource.spi.SearchOptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public class GroupResourceTypeProvider extends AbstractScimResourceTypeProvider<GroupModel, Group> implements ScimAttributeJpaExpressionResolver {

    private final GroupCoreModelSchema schema;

    /**
     * The owned ids on the page being listed. A single read leaves this null and looks the group up on its own.
     */
    private Set<String> ownedOnPage;

    public GroupResourceTypeProvider(KeycloakSession session) {
        this(session, new GroupCoreModelSchema(session));
    }

    private GroupResourceTypeProvider(KeycloakSession session, GroupCoreModelSchema schema) {
        super(session, schema);
        this.schema = schema;
    }

    @Override
    public List<MembershipChange> pollMembershipChanges() {
        List<MembershipChange> changes = List.copyOf(schema.getMembershipChanges());
        schema.clearMembershipChanges();
        return changes;
    }

    /**
     * A group created with members in a realm with group ownership records each join, as PUT and PATCH do. Elsewhere
     * a create with members is refused, so nothing is reported.
     */
    @Override
    public List<MembershipChange> pollCreateMembershipChanges() {
        return pollMembershipChanges();
    }

    @Override
    protected Group onCreate(Group group) {
        RealmModel realm = session.getContext().getRealm();

        if (!GroupOwnership.isEnabled(realm)) {
            GroupModel model = session.groups().createGroup(realm, group.getDisplayName());
            populate(model, group);
            group.setCreatedTimestamp(model.getCreatedTimestamp());
            group.setLastModifiedTimestamp(model.getLastModifiedTimestamp());
            return group;
        }

        // The caller becomes the group's owner, whether or not it sends members. Every member is checked before the
        // group is created.
        GroupOwnership ownership = new GroupOwnership(session, schema);
        String owner = ownership.requireCaller();
        List<Member> members = group.getMembers();
        Set<String> desired = new LinkedHashSet<>();

        for (String userId : memberValues(members)) {
            desired.add(ownership.named(userId));
        }

        group.setMembers(null);
        GroupModel model = session.groups().createGroup(realm, group.getDisplayName());
        populate(model, group);
        ownership.recordOwner(model, owner);

        if (!desired.isEmpty()) {
            ownership.checkGroup(model);
            ownership.lock(model);
            ownership.apply(model, ownership.plan(model, Set.of(), desired), schema::recordMembershipChange);
        }

        group.setMembers(members);
        group.setCreatedTimestamp(model.getCreatedTimestamp());
        group.setLastModifiedTimestamp(model.getLastModifiedTimestamp());
        return group;
    }

    @Override
    public Group get(String id, List<String> attributes, List<String> excludedAttributes) {
        ownedOnPage = null;
        return super.get(id, attributes, excludedAttributes);
    }

    @Override
    protected Stream<Group> render(Stream<GroupModel> models, SearchOptions searchOptions) {
        if (!GroupOwnership.isEnabled(session.getContext().getRealm())
                || !returnsMembers(searchOptions.getAttributes(), searchOptions.getExcludedAttributes())) {
            ownedOnPage = null;
            return super.render(models, searchOptions);
        }

        List<GroupModel> page = models.toList();
        ownedOnPage = GroupOwnership.ownedAmong(session, page.stream().map(GroupModel::getId).toList());
        return page.stream().map(model -> createResourceTypeInstance(model, searchOptions.getAttributes(), searchOptions.getExcludedAttributes()));
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

        Group group = super.createResourceTypeInstance(model, attributes, excludedAttributes);

        boolean owned = ownedOnPage != null
                ? ownedOnPage.contains(model.getId())
                : new GroupOwnership(session, schema).ownerOf(model) != null;

        if (GroupOwnership.isEnabled(session.getContext().getRealm()) && returnsMembers(attributes, excludedAttributes) && owned) {
            // An owner that sends full member lists may build the next list from a read, so a read of an owned group
            // shows every member without being asked, unless the request leaves them out.
            List<Member> members = schema.readMembers(model);
            group.setMembers(members.isEmpty() ? null : members);
        }

        return group;
    }

    /**
     * Whether a read of an owned group includes members: always, unless the request excludes them or asks only for
     * other attributes.
     */
    private static boolean returnsMembers(List<String> attributes, List<String> excludedAttributes) {
        if (excludedAttributes != null && excludedAttributes.stream().anyMatch(GroupResourceTypeProvider::namesMembers)) {
            return false;
        }

        return attributes == null || attributes.stream().allMatch(String::isBlank)
                || attributes.stream().anyMatch(GroupResourceTypeProvider::namesMembers);
    }

    /**
     * Whether an attribute name in a request refers to {@code members}: {@code members}, a sub-attribute such as
     * {@code members.value}, or either with the schema URN in front.
     */
    private static boolean namesMembers(String name) {
        String attribute = name.trim();
        int colon = attribute.lastIndexOf(':');

        if (colon >= 0) {
            attribute = attribute.substring(colon + 1);
        }

        String lower = attribute.toLowerCase(Locale.ROOT);
        return lower.equals("members") || lower.startsWith("members.");
    }

    @Override
    public Group update(Group resource) {
        if (GroupOwnership.isEnabled(session.getContext().getRealm())) {
            GroupModel model = getModel(resource.getId());

            // Callers that cannot manage the group get the regular refusal below, and learn nothing about its owner.
            if (model != null && hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
                GroupOwnership ownership = new GroupOwnership(session, schema);
                String owner = ownership.ownerOf(model);

                if (owner != null) {
                    ownership.checkOwner(owner);

                    if (resource.getMembers() != null) {
                        return updateAsOwner(model, resource, ownership);
                    }
                }
            }
        }

        if (resource.getMembers() != null) {
            throw new ModelValidationException("Managing members on updates is not supported");
        }

        return super.update(resource);
    }

    /**
     * PUT from the owner: the request's member list becomes the group's members, except that members the caller may
     * not change stay. Every member is checked before anything changes, and the group stays locked until the
     * transaction ends.
     */
    private Group updateAsOwner(GroupModel model, Group resource, GroupOwnership ownership) {
        ownership.checkGroup(model);
        ownership.lock(model);
        Set<String> current = ownership.currentMembers(model);
        Set<String> desired = new LinkedHashSet<>();

        for (String userId : memberValues(resource.getMembers())) {
            desired.add(ownership.listed(userId, current));
        }

        GroupOwnership.Change change = ownership.plan(model, current, desired);

        resource.setMembers(null);
        Group updated = super.update(resource);
        ownership.apply(model, change, schema::recordMembershipChange);
        List<Member> members = schema.readMembers(model);
        updated.setMembers(members.isEmpty() ? null : members);

        return updated;
    }

    @Override
    protected void applyPatch(Group existing, GroupModel model, List<PatchOperation> operations) {
        if (GroupOwnership.isEnabled(session.getContext().getRealm())) {
            GroupOwnership ownership = new GroupOwnership(session, schema);
            String owner = ownership.ownerOf(model);

            if (owner != null) {
                ownership.checkOwner(owner);

                if (operations.stream().anyMatch(this::isMembersOperation)) {
                    patchAsOwner(existing, model, operations, ownership);
                    return;
                }
            }
        }

        super.applyPatch(existing, model, operations);
    }

    /**
     * PATCH from the owner. The member operations are replayed, in order, on the group's current members: add puts
     * members in, replace sets the full list, remove takes members out, and a remove without a filter or value takes
     * out every member. Members the caller may not change stay whatever the operations say, but naming one in an add
     * or remove is refused, as the regular PATCH refuses it. Every member is checked before anything changes. Other
     * operations go through the regular path.
     */
    private void patchAsOwner(Group existing, GroupModel model, List<PatchOperation> operations, GroupOwnership ownership) {
        ownership.checkGroup(model);
        ownership.lock(model);
        Set<String> current = ownership.currentMembers(model);
        Set<String> desired = new LinkedHashSet<>(current);
        List<PatchOperation> others = new ArrayList<>();

        for (PatchOperation operation : operations) {
            String op = Optional.ofNullable(operation.getOp()).orElse("").toLowerCase(Locale.ROOT);
            String path = operation.getPath();

            if (isResourceRoot(path)) {
                JsonNode value = operation.getValue();
                String field = value != null && value.isObject() ? membersField(value) : null;

                if (field == null) {
                    others.add(operation);
                    continue;
                }

                List<String> ids = memberIds(value.get(field));

                switch (op) {
                    case "add" -> ids.forEach(id -> desired.add(ownership.named(id)));
                    case "replace" -> {
                        desired.clear();
                        ids.forEach(id -> desired.add(ownership.listed(id, current)));
                    }
                    default -> throw new ModelValidationException("Unsupported operation on members: " + operation.getOp());
                }

                ObjectNode rest = value.deepCopy();
                rest.remove(field);

                if (!rest.isEmpty()) {
                    PatchOperation remaining = new PatchOperation();
                    remaining.setOp(operation.getOp());
                    remaining.setPath(path);
                    remaining.setValue(rest);
                    others.add(remaining);
                }

                continue;
            }

            MembersPath membersPath = membersPath(path);

            if (membersPath == null) {
                others.add(operation);
                continue;
            }

            List<String> filterValues = membersPath.filterValues();

            switch (op) {
                case "add" -> {
                    rejectFilter(filterValues);
                    memberIds(operation.getValue()).forEach(id -> desired.add(ownership.named(id)));
                }
                case "replace" -> {
                    rejectFilter(filterValues);
                    desired.clear();
                    memberIds(operation.getValue()).forEach(id -> desired.add(ownership.listed(id, current)));
                }
                case "remove" -> {
                    if (filterValues != null) {
                        filterValues.forEach(id -> desired.remove(ownership.named(id)));
                    } else if (operation.getValue() == null || operation.getValue().isNull()) {
                        // no filter and no value: every member goes, except those the caller may not change. An
                        // explicit empty list removes nobody.
                        desired.clear();
                    } else {
                        // a value listing the members, as Microsoft Entra ID sends it
                        memberIds(operation.getValue()).forEach(id -> desired.remove(ownership.named(id)));
                    }
                }
                default -> throw new ModelValidationException("Unsupported operation on members: " + operation.getOp());
            }
        }

        GroupOwnership.Change change = ownership.plan(model, current, desired);

        if (!others.isEmpty()) {
            super.applyPatch(existing, model, others);
        }

        ownership.apply(model, change, schema::recordMembershipChange);
    }

    private static void rejectFilter(List<String> filterValues) {
        if (filterValues != null) {
            throw new ModelValidationException("A filter in the path is only supported when removing members");
        }
    }

    private boolean isMembersOperation(PatchOperation operation) {
        String path = operation.getPath();

        if (isResourceRoot(path)) {
            JsonNode value = operation.getValue();
            return value != null && value.isObject() && membersField(value) != null;
        }

        return membersPath(path) != null;
    }

    /**
     * Whether a PATCH path targets the group itself: no path, or the Group schema URN (RFC 7644 section 3.5.2). The
     * value then holds attributes by name, {@code members} among them.
     */
    private boolean isResourceRoot(String path) {
        return path == null || path.isBlank() || path.trim().equalsIgnoreCase(schema.getId());
    }

    /**
     * A PATCH path that targets {@code members}.
     *
     * @param filterValues the user ids from a {@code members[value eq "a" or value eq "b"]} filter, or {@code null}
     *                     without a filter
     */
    private record MembersPath(List<String> filterValues) {
    }

    /**
     * @return {@code null} when the path targets another attribute
     */
    private MembersPath membersPath(String path) {
        Path parsed;

        try {
            parsed = new Path(schema, path);
        } catch (RuntimeException ex) {
            throw new ModelValidationException("Only a members[value eq \"id\"] filter is supported in the path");
        }

        Attribute<GroupModel, Group> attribute = schema.getAttributeByPath(parsed.getPath());

        if (attribute == null || !"members".equals(attribute.getName())) {
            return null;
        }

        JsonNode value = parsed.getValue(attribute);

        if (value == null || value.isNull()) {
            return new MembersPath(null);
        }

        return new MembersPath(filterIds(value));
    }

    /**
     * The user ids from a filter the shared parser accepted: one comparison, or an {@code or} of them.
     */
    private static List<String> filterIds(JsonNode value) {
        List<String> ids = new ArrayList<>();

        if (value.isArray()) {
            value.forEach(node -> ids.add(filterId(node)));
        } else if (value.isObject()) {
            ids.add(filterId(value));
        } else {
            throw new ModelValidationException("Only a members[value eq \"id\"] filter is supported in the path");
        }

        return ids;
    }

    private static String filterId(JsonNode node) {
        JsonNode id = node.get("value");

        if (id == null || !id.isTextual()) {
            throw new ModelValidationException("Only a members[value eq \"id\"] filter is supported in the path");
        }

        return id.asText();
    }

    private static String membersField(JsonNode value) {
        Iterator<String> names = value.fieldNames();

        while (names.hasNext()) {
            String name = names.next();
            if ("members".equalsIgnoreCase(name)) {
                return name;
            }
        }

        return null;
    }

    /**
     * The user ids in a {@code members} value: an array, or a single item, where each item is an object with a
     * {@code value} or a bare user id. A missing value is kept as {@code null} so that the check reports it.
     */
    private static List<String> memberIds(JsonNode value) {
        if (value == null || value.isNull()) {
            return List.of();
        }

        List<JsonNode> items = new ArrayList<>();

        if (value.isArray()) {
            value.forEach(items::add);
        } else {
            items.add(value);
        }

        List<String> ids = new ArrayList<>();

        for (JsonNode item : items) {
            if (item.isTextual()) {
                // a bare user id, as the regular PATCH path also accepts
                ids.add(item.asText());
            } else if (item.isObject()) {
                JsonNode id = item.get("value");
                ids.add(id == null || id.isNull() ? null : id.asText());
            } else {
                throw new ModelValidationException("Invalid members value");
            }
        }

        return ids;
    }

    private static List<String> memberValues(List<Member> members) {
        List<String> ids = new ArrayList<>();

        if (members != null) {
            for (Member member : members) {
                ids.add(member == null ? null : member.getValue());
            }
        }

        return ids;
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
    protected Stream<GroupModel> getModels(SearchOptions searchRequest) {
        RealmModel realm = session.getContext().getRealm();

        ScimFilterParser.FilterContext filterContext = searchRequest.getFilterContext();

        if (filterContext != null) {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<GroupEntity> query = cb.createQuery(GroupEntity.class);
            Root<GroupEntity> root = query.from(GroupEntity.class);
            List<Predicate> predicates = getGroupPredicates(filterContext, cb, query, root);

            // apply distinct and order by name to ensure consistency with no-filter case
            query.where(predicates).distinct(true).orderBy(cb.asc(root.get("name")));

            return closing(paginateQuery(em.createQuery(query), searchRequest.getStartIndex() - 1, searchRequest.getCount()).getResultStream()
                    .map(entity -> session.groups().getGroupById(realm, entity.getId()))
                    .filter(Objects::nonNull));
        } else {
            return session.groups().getTopLevelGroupsStream(realm, searchRequest.getStartIndex() - 1, searchRequest.getCount());
        }
    }

    @Override
    public Long count(SearchOptions searchRequest, int resourceSize) {
        if (isViewFullyDenied()) {
            return 0L;
        }

        if (resourceSize < searchRequest.getCount() && (resourceSize > 0 || searchRequest.getStartIndex() == 1)) {
            return (long) (searchRequest.getStartIndex() - 1 + resourceSize);
        }

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
    public boolean onDelete(GroupModel model) {
        RealmModel realm = session.getContext().getRealm();
        Permissions permissions = session.getContext().getPermissions();
        rejectIfAdminDescendant(model, permissions);
        if (GroupOwnership.isEnabled(realm)) {
            new GroupOwnership(session, schema).checkDelete(model);
        }
        return session.groups().removeGroup(realm, model);
    }

    private void rejectIfAdminDescendant(GroupModel group, Permissions permissions) {
        group.getSubGroupsStream().forEach(subGroup -> {
            if (permissions.isAdminGroup(subGroup)) {
                throw new ForbiddenException();
            }
            rejectIfAdminDescendant(subGroup, permissions);
        });
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
        // unaffected. Permission checks are required regardless of whether FGAP is enabled.
        BiPredicate<String, String> authCheck = (path, value) -> {
            if ("members.value".equalsIgnoreCase(path) || "members".equalsIgnoreCase(path)) {
                if (value == null) {
                    return permissions.hasPermission(AdminPermissionsSchema.USERS_RESOURCE_TYPE, AdminPermissionsSchema.VIEW);
                }
                UserModel user = session.users().getUserById(realm, value);
                return user != null && permissions.hasPermission(user, AdminPermissionsSchema.USERS_RESOURCE_TYPE, AdminPermissionsSchema.VIEW);
            }
            return true;
        };

        // create filter predicate using the same query and root that will be used for execution
        ScimJPAPredicateEvaluator evaluator = new ScimJPAPredicateEvaluator(this, getSchemas(), cb, query, root, authCheck);
        predicates.add(evaluator.visit(filterContext).predicate());

        // apply realm restriction and group type restrictions
        predicates.add(cb.equal(root.get("realm"), realm.getId()));
        predicates.add(cb.equal(root.get("type"), GroupModel.Type.REALM.intValue()));
        predicates.add(cb.equal(root.get("parentId"), GroupEntity.TOP_PARENT_ID));

        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, realm, cb, query, root));

        return predicates;
    }

    @Override
    public Expression<?> getAttributeExpression(Attribute<?, ?> attribute, CriteriaBuilder cb, Root<?> root, Subquery<?> subquery) {
        if ("members".equals(attribute.getName())) {
            Root<UserGroupMembershipEntity> membership = subquery.from(UserGroupMembershipEntity.class);
            // Service accounts are not exposed as SCIM group members, so exclude them from all member
            // filters (eq, ne, pr, ...) to keep the filter path consistent with member serialization.
            subquery.where(cb.and(
                    cb.equal(membership.get("groupId"), root.get("id")),
                    cb.isNull(membership.get("user").get("serviceAccountClientLink"))));
            return membership.get("user").get("id");
        }
        return null;
    }

    @Override
    protected boolean isManageable(GroupModel model) {
        return !session.getContext().getPermissions().isAdminGroup(model);
    }
}
