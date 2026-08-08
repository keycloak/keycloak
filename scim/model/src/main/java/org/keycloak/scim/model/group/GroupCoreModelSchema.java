package org.keycloak.scim.model.group;

import java.util.ArrayList;
import java.util.Collection;
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
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.group.Member;
import org.keycloak.scim.resource.schema.AbstractModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.utils.KeycloakSessionUtil;

import static org.keycloak.utils.StringUtil.isBlank;

public final class GroupCoreModelSchema extends AbstractModelSchema<GroupModel, Group> {

    private static final String GROUP="Group";
    private static final String USER="User";
    private final KeycloakSession session;

    public GroupCoreModelSchema(KeycloakSession session) {
        super(Group.SCHEMA);
        this.session = session;
    }

    @Override
    public String getId() {
        return Group.SCHEMA;
    }

    @Override
    public String getName() {
        return "Group";
    }

    @Override
    public String getDescription() {
        return getName();
    }

    @Override
    protected Set<String> getModelAttributeNames() {
        return Set.of("name", "externalId", "members");
    }

    @Override
    protected Object getAttributeValue(GroupModel model, String name) {
        return switch (name) {
            case "name" -> model.getName();
            case "externalId" -> model.getFirstAttribute("externalId");
            case "members" -> {
                KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
                RealmModel realm = session.getContext().getRealm();
                Permissions permissions = session.getContext().getPermissions();
                List<Object> members = new ArrayList<>();

                if (permissions.hasPermission(model, AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, AdminPermissionsSchema.VIEW_MEMBERS)) {
                   members.addAll(session.users().getGroupMembersStream(realm, model)
                            .filter(this::canViewUser).toList());
                }
                members.addAll(model.getSubGroupsStream().toList());

                yield members;
            }
            default -> null;
        };
    }

    @Override
    protected String getAttributeSchemaName(String name) {
        return switch (name) {
            case "name" -> "displayName";
            case "externalId" -> name;
            case "members" -> "members";
            default -> null;
        };
    }

    @Override
    protected Map<String, Attribute<GroupModel, Group>> getAttributeMappers() {
        List<Attribute<GroupModel, Group>> attributes = new ArrayList<>(Attribute.<GroupModel, Group>simple("displayName")
                    .notCaseExact()
                    .modelAttributeResolver((attribute) -> {
                        if (attribute.getName().equals("displayName")) {
                            return "name";
                        }
                        return null;
                    })
                    .withModelSetter((m, name) -> {
                        if (name != null) {
                            m.setName(name);
                        }
                    })
                    .build());
        attributes.addAll(Attribute.<GroupModel, Group>simple("externalId")
                .immutable()
                .string()
                .withModelSetter(GroupModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<GroupModel, Group>simple("meta.created")
                .timestamp()
                .immutable()
                .modelAttributeResolver(attribute -> "createdTimestamp")
                .build());
        attributes.addAll(Attribute.<GroupModel, Group>simple("meta.lastModified")
                .timestamp()
                .modelAttributeResolver(attribute -> "lastModifiedTimestamp")
                .build());
        attributes.addAll(Attribute.<GroupModel, Group>complex("members", Member.class)
                .multivalued()
                .returned(Attribute.RETURNED_REQUEST)
                .modelAttributeResolver(Attribute::getName)
                .withModelSetter((TriConsumer<GroupModel, String, Set<Member>>) (model, name, values) -> {
                    if (!Optional.ofNullable(values).orElse(Set.of()).isEmpty()) {
                        // managing members on updates are not supported, client should use PATCH
                        throw new ModelValidationException("Managing members on updates are not supported");
                    }
                }, (BiConsumer<Group, Collection<Object>>) (group, members) -> {
                    for (Object memberModel : Optional.ofNullable(members).orElse(List.of())) {
                        Member member = new Member();
                        if (memberModel instanceof UserModel user) {
                             if (!canViewUser(user)) {
                                  throw new ModelValidationException("User with id " + user.getId() + " not found");
                             }
                            member.setValue(user.getId());
                            member.setDisplay(user.getUsername());
                            member.setType(USER);
                        } else if (memberModel instanceof GroupModel subGroup) {
                            if (!canViewGroup(subGroup)) {
                                throw new ModelValidationException("Group with id " + subGroup.getId() + " not found");
                            }
                            member.setValue(subGroup.getId());
                            member.setDisplay(subGroup.getName());
                            member.setType(GROUP);
                        }
                        group.addMember(member);
                    }
                })
                .withModelRemover((TriConsumer<GroupModel, String, Set<Member>>) (model, name, values) -> {
                    KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
                    RealmModel realm = session.getContext().getRealm();
                    checkGroupMembershipPermission(session.getContext().getPermissions(), model);

                    for (Member member : values) {
                        if (GROUP.equalsIgnoreCase(member.getType())) {
                            GroupModel subGroup = session.groups().getGroupById(realm, member.getValue());
                            if (subGroup == null || !canViewGroup(subGroup) || !model.getId().equals(subGroup.getParentId())) {
                                throw new ModelValidationException("Group with id " + member.getValue() + " is not subgroup of " + model.getId());
                            }
                            checkGroupMembershipPermission(session.getContext().getPermissions(), subGroup);
                            realm.moveGroup(subGroup, null);
                        } else if (USER.equalsIgnoreCase(member.getType())) {
                            UserModel user = session.users().getUserById(realm, member.getValue());
                            if (user == null || !canViewUser(user)) {
                                throw new ModelValidationException("User with id " + member.getValue() + " not found");
                            }
                            checkRequireManageGroupMembership(session.getContext().getPermissions(), user);
                            user.leaveGroup(model);
                        } else {
                            UserModel user = session.users().getUserById(realm, member.getValue());
                            if (user != null && canViewUser(user)) {
                                checkRequireManageGroupMembership(session.getContext().getPermissions(), user);
                                user.leaveGroup(model);
                            } else {
                                GroupModel subGroup = session.groups().getGroupById(realm, member.getValue());
                                if (subGroup == null || !canViewGroup(subGroup) || !model.getId().equals(subGroup.getParentId())) {
                                    throw new ModelValidationException("User or Group with id " + member.getValue() + " not found");
                                }
                                checkGroupMembershipPermission(session.getContext().getPermissions(), subGroup);
                                realm.moveGroup(subGroup, null);
                            }
                        }
                    }
                })
                .withModelAdder((TriConsumer<GroupModel, String, Set<Member>>) (model, name, values) -> {
                    KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
                    RealmModel realm = session.getContext().getRealm();
                    checkGroupMembershipPermission(session.getContext().getPermissions(), model);

                    for (Member member : values) {
                        if (GROUP.equalsIgnoreCase(member.getType())) {
                            if (model.getId().equals(member.getValue())) {
                                throw new ModelValidationException("A group cannot be a member of itself");
                            }

                            GroupModel subGroup = session.groups().getGroupById(realm, member.getValue());
                            if (subGroup == null || !canViewGroup(subGroup)) {
                                throw new ModelValidationException("Group with id " + member.getValue() + " not found");
                            }
                            if (!model.getId().equals(subGroup.getParentId())) {
                                checkGroupMembershipPermission(session.getContext().getPermissions(), subGroup);
                                realm.moveGroup(subGroup, model);
                            }
                        } else {
                            UserModel user = session.users().getUserById(realm, member.getValue());
                            if (user == null || !canViewUser(user)) {
                                throw new ModelValidationException("User with id " + member.getValue() + " not found");
                            }
                            checkRequireManageGroupMembership(session.getContext().getPermissions(), user);
                            user.joinGroup(model);
                        }
                    }
                })
                .build());
        return attributes.stream().collect(Collectors.toMap(Attribute::getName, Function.identity()));
    }

    @Override
    public void populate(Group resource, GroupModel model) {
        super.populate(resource, model);
        setTimestamps(resource, model);
    }

    @Override
    public void populate(Group resource, GroupModel model, List<String> requestedAttributes, List<String> excludedAttributes) {
        super.populate(resource, model, requestedAttributes, excludedAttributes);
        setTimestamps(resource, model);
    }

    @Override
    public void validate(Group representation) throws ModelValidationException {
        if (isBlank(representation.getDisplayName())) {
            throw new ModelValidationException("Display name is required");
        }
    }

    private void setTimestamps(Group resource, GroupModel model) {
        Long createdTimestamp = model.getCreatedTimestamp();
        if (createdTimestamp != null) {
            resource.setCreatedTimestamp(createdTimestamp);
        }
        Long lastModified = model.getLastModifiedTimestamp();
        if (lastModified != null) {
            resource.setLastModifiedTimestamp(lastModified);
        }
    }

    private static void checkGroupMembershipPermission(Permissions permissions, GroupModel group) {
        if (GroupModel.Type.ORGANIZATION.equals(group.getType()) && group.getOrganization() != null) {
            throw new ModelValidationException("Cannot access organization related group via non Organization API.");
        }
        if (permissions.isAdminGroup(group)) {
            throw new ForbiddenException();
        }
        if (!permissions.hasPermission(group, AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, AdminPermissionsSchema.MANAGE_MEMBERSHIP)) {
            throw new ForbiddenException();
        }
    }

    private void checkRequireManageGroupMembership(Permissions permissions, UserModel model) {
        if (permissions.isAdminUser(model)) {
            throw new ForbiddenException();
        }
        if (!permissions.hasPermission(model, AdminPermissionsSchema.USERS_RESOURCE_TYPE, AdminPermissionsSchema.MANAGE_GROUP_MEMBERSHIP)) {
            throw new ForbiddenException();
        }
    }

    private boolean canViewUser(UserModel u) {
        Permissions permissions = session.getContext().getPermissions();
        return permissions.hasPermission(u, AdminPermissionsSchema.USERS_RESOURCE_TYPE, AdminPermissionsSchema.VIEW);
    }

    private boolean canViewGroup(GroupModel u) {
        Permissions permissions = session.getContext().getPermissions();
        return permissions.hasPermission(u, AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, AdminPermissionsSchema.VIEW);
    }
}
