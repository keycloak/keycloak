package org.keycloak.scim.model.user;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.Scim;
import org.keycloak.scim.resource.common.Name;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.schema.attribute.AttributeMapper;
import org.keycloak.scim.resource.user.User;

public final class UserCoreModelSchema extends AbstractUserModelSchema {

    private static final List<Attribute<UserModel, User>> ATTRIBUTE_MAPPERS = new ArrayList<>();

    static {
        ATTRIBUTE_MAPPERS.add(new Attribute<>("userName", new UserAttributeMapper(User::setUserName)));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("emails[0].value", new UserAttributeMapper(User::setEmail)));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("name.givenName", new UserAttributeMapper(User::setFirstName)));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("name.familyName", new UserAttributeMapper(User::setLastName)));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("name.middleName", new UserNameAttributeMapper(Name::setMiddleName)));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("name.honorificPrefix", new UserNameAttributeMapper(Name::setHonorificPrefix)));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("name.honorificSuffix", new UserNameAttributeMapper(Name::setHonorificSuffix)));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("externalId", new UserAttributeMapper(User::setExternalId)));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("nickName", new UserAttributeMapper(User::setNickName)));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("locale", new UserAttributeMapper(User::setLocale)));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("active", new AttributeMapper<>(
                (model, value) -> model.setEnabled(Boolean.parseBoolean(value)),
                (user, value) -> user.setActive(Boolean.parseBoolean(value)))));
    }

    public UserCoreModelSchema(KeycloakSession session) {
        super(session, Scim.getCoreSchema(User.class), ATTRIBUTE_MAPPERS);
    }
}
