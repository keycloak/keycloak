package org.keycloak.scim.model.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.Scim;
import org.keycloak.scim.resource.common.Name;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.user.User;

public final class UserCoreModelSchema extends AbstractUserModelSchema {

    public UserCoreModelSchema(KeycloakSession session) {
        super(session, Scim.getCoreSchema(User.class));
    }

    @Override
    protected Map<String, Attribute<UserModel, User>> doGetAttributes() {
        List<Attribute<UserModel, User>> attributes = new ArrayList<>();

        attributes.addAll(Attribute.<UserModel, User>simple("userName")
                .primary()
                .withSetters(UserModel::setSingleAttribute)
                .modelAttributeResolver(this::createModelAttributeResolver)
                .build());
        attributes.addAll(Attribute.complex("emails", UserModel::setSingleAttribute, this::createModelAttributeResolver, true)
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
                .withSetters(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("title")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withSetters(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("externalId")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withSetters(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("userType")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withSetters(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("nickName")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withSetters(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("locale")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withSetters(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("timezone")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withSetters(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("preferredLanguage")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withSetters(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("profileUrl")
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withSetters(UserModel::setSingleAttribute)
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("active")
                .primary()
                .bool()
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withSetters(
                        (model, name, value) -> model.setEnabled(Boolean.parseBoolean(value))
                        , (user, value) -> user.setActive(Boolean.parseBoolean(value))
                )
                .build());
        attributes.addAll(Attribute.<UserModel, User>simple("meta.created")
                .primary()
                .timestamp()
                .immutable()
                .modelAttributeResolver(this::createModelAttributeResolver)
                .build());

        return attributes.stream().collect(Collectors.toMap(Attribute::getName, Function.identity()));
    }
}
