package org.keycloak.scim.model.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.util.TriConsumer;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.Permissions;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.scim.model.group.GroupOwnership;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.resource.Scim;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.spi.MembershipChange;
import org.keycloak.scim.resource.spi.ScimMutabilityException;
import org.keycloak.scim.resource.user.Email;
import org.keycloak.scim.resource.user.GroupMembership;
import org.keycloak.scim.resource.user.Name;
import org.keycloak.scim.resource.user.User;
import org.keycloak.utils.GroupUtils;
import org.keycloak.utils.KeycloakSessionUtil;

public final class UserCoreModelSchema extends AbstractUserModelSchema {

    private final List<MembershipChange> membershipChanges = new ArrayList<>();

    public UserCoreModelSchema(KeycloakSession session) {
        super(session, Scim.getCoreSchema(User.class));
    }

    @Override
    public String getName() {
        return "User";
    }

    @Override
    public String getDescription() {
        return "User Account";
    }

    @Override
    protected boolean hasSchema(String attributeName) {
        String schema = Attribute.getSchema(attributeName);

        return schema == null || getId().equals(schema);
    }

    @Override
    protected Map<String, Attribute<UserModel, User>> getAttributeMappers() {
        List<Attribute<UserModel, User>> attributes = new ArrayList<>();

        attributes.addAll(Attribute.<UserModel, User>simple("userName")
                .required()
                .storedLowerCase()
                .serverUnique()
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withModelSetter(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>complex("emails", Email.class)
                .modelAttributeResolver(this::createModelAttributeResolver)
                .storedLowerCase()
                .globalUnique()
                .multivalued()
                .withModelSetter((TriConsumer<UserModel, String, Set<Email>>) (model, name, values) -> {
                    for (Email value : values) {
                        model.setEmail(value.getValue());
                        break;
                    }
                }, (BiConsumer<User, Collection<String>>) (user, emails) -> {
                    if (emails == null || emails.isEmpty()) {
                        return;
                    }
                    user.setEmail(emails.iterator().next());
                })
                .withModelRemover((TriConsumer<UserModel, String, Set<Email>>) (model, name, values) -> {
                    model.setEmail(null);
                })
                .withModelAdder((TriConsumer<UserModel, String, Set<Email>>) (model, name, values) -> {
                    for (Email value : values) {
                        model.setEmail(value.getValue());
                        break;
                    }
                })
                .build());
        attributes.addAll(Attribute.<UserModel, User>complex("name", Name.class)
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withAttribute("givenName", UserModel::setSingleAttribute)
                .withAttribute("formatted", UserModel::setSingleAttribute)
                .withAttribute("familyName", UserModel::setSingleAttribute)
                .withAttribute("middleName", UserModel::setSingleAttribute)
                .withAttribute("honorificPrefix", UserModel::setSingleAttribute)
                .withAttribute("honorificSuffix", UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("displayName")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withModelSetter(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("title")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withModelSetter(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("externalId")
                .caseExact()
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withModelSetter(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("userType")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withModelSetter(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("nickName")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withModelSetter(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("locale")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withModelSetter(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("timezone")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withModelSetter(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("preferredLanguage")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withModelSetter(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("profileUrl")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withModelSetter(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("active")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .bool()
                .withModelSetter(
                        (model, name, value) -> model.setEnabled(Boolean.parseBoolean(Optional.ofNullable(value).orElse("").toString()))
                        , (user, value) -> {
                            if (value == null) {
                                return;
                            }
                            user.setActive(Boolean.parseBoolean(value.toString()));
                        }
                )
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("meta.created")
                .timestamp()
                .immutable()
                .modelAttributeResolver(this::createModelAttributeResolver)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("meta.lastModified")
                .timestamp()
                .modelAttributeResolver(attribute -> "lastModifiedTimestamp")
                .build());
        attributes.addAll(Attribute.<UserModel, User>complex("groups", GroupMembership.class)
                .multivalued()
                .returned(Attribute.RETURNED_REQUEST)
                .modelAttributeResolver(Attribute::getName)
                .withModelSetter((TriConsumer<UserModel, String, Set<GroupMembership>>) (model, name, values) -> {
                    KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
                    RealmModel realm = session.getContext().getRealm();
                    checkUserMembershipPermission(session.getContext().getPermissions(), model);
                    if (GroupOwnership.isEnabled(realm)) {
                        Set<String> requested = groupIds(values);
                        Set<String> direct = directGroupIds(model);
                        Set<String> changed = new HashSet<>(requested);
                        changed.removeAll(direct);
                        direct.stream().filter(id -> !requested.contains(id)).forEach(changed::add);
                        checkOwnedGroupsUnchanged(session, changed);
                    }
                    List<GroupModel> remove = new ArrayList<>();

                    for (GroupUtils.GroupMembership membership : GroupUtils.getAllMemberships(session, model.getGroupsStream().toList())) {
                        if (values.stream().noneMatch(m -> m.getValue().equals(membership.group().getId()))) {
                            remove.add(membership.group());
                        }
                    }

                    for (GroupMembership membership : values) {
                        GroupModel group = session.groups().getGroupById(realm, membership.getValue());

                        if (group == null || !canViewGroup(group)) {
                            throw new ModelValidationException("Group with id " + membership.getValue() + " not found");
                        }

                        checkGroupMembershipPermission(session.getContext().getPermissions(), group);
                        if (!RoleUtils.isDirectMember(model.getGroupsStream(), group)) {
                            model.joinGroup(group);
                            membershipChanges.add(new MembershipChange(group, model, true));
                        }
                    }

                    for (GroupModel group : remove) {
                        checkGroupMembershipPermission(session.getContext().getPermissions(), group);
                        if (RoleUtils.isDirectMember(model.getGroupsStream(), group)) {
                            model.leaveGroup(group);
                            membershipChanges.add(new MembershipChange(group, model, false));
                        }
                    }
                }, (BiConsumer<User, Collection<GroupModel>>) (user, groups) -> {
                    KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();

                    for (GroupUtils.GroupMembership membership : GroupUtils.getAllMemberships(session, groups)) {
                        GroupMembership rep = new GroupMembership();

                        rep.setValue(membership.group().getId());
                        rep.setDisplay(membership.group().getName());
                        rep.setType(membership.direct() ? "direct" : "indirect");

                        user.addGroup(rep);
                    }
                })
                .withModelRemover((TriConsumer<UserModel, String, Set<GroupMembership>>) (model, name, values) -> {
                    KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
                    RealmModel realm = session.getContext().getRealm();
                    checkUserMembershipPermission(session.getContext().getPermissions(), model);
                    if (GroupOwnership.isEnabled(realm)) {
                        Set<String> removed = groupIds(values);
                        removed.retainAll(directGroupIds(model));
                        checkOwnedGroupsUnchanged(session, removed);
                    }

                    for (GroupMembership membership : values) {
                        GroupModel group = session.groups().getGroupById(realm, membership.getValue());

                        if (group == null || !canViewGroup(group)) {
                            throw new ModelValidationException("Group with id " + membership.getValue() + " not found");
                        }

                        checkGroupMembershipPermission(session.getContext().getPermissions(), group);
                        if (RoleUtils.isDirectMember(model.getGroupsStream(), group)) {
                            model.leaveGroup(group);
                            membershipChanges.add(new MembershipChange(group, model, false));
                        }
                    }
                })
                .withModelAdder((TriConsumer<UserModel, String, Set<GroupMembership>>) (model, name, values) -> {
                    KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
                    RealmModel realm = session.getContext().getRealm();
                    checkUserMembershipPermission(session.getContext().getPermissions(), model);
                    if (GroupOwnership.isEnabled(realm)) {
                        Set<String> added = groupIds(values);
                        added.removeAll(directGroupIds(model));
                        checkOwnedGroupsUnchanged(session, added);
                    }

                    for (GroupMembership membership : values) {
                        GroupModel group = session.groups().getGroupById(realm, membership.getValue());

                        if (group == null || !canViewGroup(group)) {
                            throw new ModelValidationException("Group with id " + membership.getValue() + " not found");
                        }

                        checkGroupMembershipPermission(session.getContext().getPermissions(), group);
                        if (!RoleUtils.isDirectMember(model.getGroupsStream(), group)) {
                            model.joinGroup(group);
                            membershipChanges.add(new MembershipChange(group, model, true));
                        }
                    }
                })
                .build());

        return attributes.stream().collect(Collectors.toMap(Attribute::getName, Function.identity()));
    }

    private static Set<String> groupIds(Collection<?> values) {
        Set<String> ids = new HashSet<>();

        if (values != null) {
            for (Object value : values) {
                if (value instanceof GroupMembership membership) {
                    ids.add(membership.getValue());
                } else if (value != null) {
                    ids.add(value.toString());
                }
            }
        }

        return ids;
    }

    private static Set<String> directGroupIds(UserModel model) {
        return model.getGroupsStream().map(GroupModel::getId).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * A group owned by a SCIM client takes its members from that client's writes to the Group resource. A membership
     * changed through a User would go around the owner, and RFC 7643 section 4.1.2 makes a User's {@code groups}
     * read-only anyway, so such a write is refused. A write that changes no membership of an owned group is accepted,
     * and groups that no SCIM client owns behave as before. Callers check {@link GroupOwnership#isEnabled} first, so a
     * realm without group ownership reads nothing more than before.
     *
     * @param changed the ids of the groups the write would add the user to or remove the user from
     */
    private static void checkOwnedGroupsUnchanged(KeycloakSession session, Set<String> changed) {
        if (!GroupOwnership.ownedAmong(session, changed).isEmpty()) {
            throw new ScimMutabilityException("Membership of a group owned by a SCIM client cannot be changed through the User resource. Use the Group resource instead.");
        }
    }

    @Override
    public void populate(User resource, UserModel model) {
        super.populate(resource, model);
        setTimestamps(resource, model);
    }

    @Override
    public void populate(User resource, UserModel model, List<String> requestedAttributes, List<String> excludedAttributes) {
        super.populate(resource, model, requestedAttributes, excludedAttributes);
        setTimestamps(resource, model);
    }

    List<MembershipChange> getMembershipChanges() {
        return membershipChanges;
    }

    void clearMembershipChanges() {
        membershipChanges.clear();
    }

    private static void checkUserMembershipPermission(Permissions permissions, UserModel user) {
        if (!permissions.hasPermission(user, AdminPermissionsSchema.USERS_RESOURCE_TYPE, AdminPermissionsSchema.MANAGE_GROUP_MEMBERSHIP)) {
            throw new ForbiddenException();
        }
    }

    private static void checkGroupMembershipPermission(Permissions permissions, GroupModel group) {
        if (isOrganizationGroup(group)) {
            throw new ModelValidationException("Cannot access organization related group via non Organization API.");
        }
        if (permissions.isAdminGroup(group)) {
            throw new ForbiddenException();
        }
        if (!permissions.hasPermission(group, AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, AdminPermissionsSchema.MANAGE_MEMBERSHIP)) {
            throw new ForbiddenException();
        }
    }

    private void setTimestamps(User resource, UserModel model) {
        Long createdTimestamp = model.getCreatedTimestamp();
        if (createdTimestamp != null) {
            resource.setCreatedTimestamp(createdTimestamp);
        }
        Long lastModified = model.getLastModifiedTimestamp();
        if (lastModified != null) {
            resource.setLastModifiedTimestamp(lastModified);
        }
    }
}
