package org.keycloak.scim.model.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.common.util.TriConsumer;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.Scim;
import org.keycloak.scim.resource.common.Email;
import org.keycloak.scim.resource.common.Name;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.user.GroupMembership;
import org.keycloak.scim.resource.user.User;
import org.keycloak.utils.GroupUtils;
import org.keycloak.utils.KeycloakSessionUtil;

public final class UserCoreModelSchema extends AbstractUserModelSchema {

    public UserCoreModelSchema(KeycloakSession session) {
        super(session, Scim.getCoreSchema(User.class));
    }

    @Override
    protected Map<String, Attribute<UserModel, User>> doGetAttributes() {
        List<Attribute<UserModel, User>> attributes = new ArrayList<>();

        attributes.addAll(Attribute.<UserModel, User>simple("userName")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .primary()
                .withModelSetter(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>complex("emails", Email.class)
                .modelAttributeResolver(this::createModelAttributeResolver)
                .primary()
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
                .withAttribute("givenName", UserModel::setSingleAttribute, true)
                .withAttribute("formatted", UserModel::setSingleAttribute)
                .withAttribute("familyName", UserModel::setSingleAttribute, true)
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
                .primary()
                .bool()
                .withModelSetter(
                        (model, name, value) -> model.setEnabled(Boolean.parseBoolean(Optional.ofNullable(value).orElse("").toString()))
                        , (user, value) -> user.setActive(Boolean.parseBoolean(value.toString()))
                )
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("meta.created")
                .primary()
                .timestamp()
                .immutable()
                .modelAttributeResolver(this::createModelAttributeResolver)
                .build());
        attributes.addAll(Attribute.<UserModel, User>complex("groups", GroupMembership.class)
                .modelAttributeResolver(Attribute::getName)
                .multivalued()
                .withModelSetter((TriConsumer<UserModel, String, Set<GroupMembership>>) (model, name, values) -> {
                    KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
                    RealmModel realm = session.getContext().getRealm();
                    List<GroupModel> remove = new ArrayList<>();

                    for (GroupUtils.GroupMembership membership : GroupUtils.getAllMemberships(session, model.getGroupsStream().toList())) {
                        if (values.stream().noneMatch(m -> m.getValue().equals(membership.group().getId()))) {
                            remove.add(membership.group());
                        }
                    }

                    for (GroupMembership membership : values) {
                        GroupModel group = session.groups().getGroupById(realm, membership.getValue());

                        if (group == null) {
                            throw new ModelValidationException("Group with id " + membership.getValue() + " not found");
                        }

                        model.joinGroup(group);
                    }

                    for (GroupModel group : remove) {
                        model.leaveGroup(group);
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

                    for (GroupMembership membership : values) {
                        GroupModel group = session.groups().getGroupById(realm, membership.getValue());

                        if (group == null) {
                            throw new ModelValidationException("Group with id " + membership.getValue() + " not found");
                        }

                        model.leaveGroup(group);
                    }
                })
                .withModelAdder((TriConsumer<UserModel, String, Set<GroupMembership>>) (model, name, values) -> {
                    KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
                    RealmModel realm = session.getContext().getRealm();

                    for (GroupMembership membership : values) {
                        GroupModel group = session.groups().getGroupById(realm, membership.getValue());

                        if (group == null) {
                            throw new ModelValidationException("Group with id " + membership.getValue() + " not found");
                        }

                        model.joinGroup(group);
                    }
                })
                .build());

        return attributes.stream().collect(Collectors.toMap(Attribute::getName, Function.identity()));
    }
}
