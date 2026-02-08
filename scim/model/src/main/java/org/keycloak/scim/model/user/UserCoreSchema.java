package org.keycloak.scim.model.user;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.user.User;

public final class UserCoreSchema extends AbstractUserSchema {

    private static final Map<String, AttributeMapper<UserModel, User>> ATTRIBUTE_MAPPERS = new HashMap<>();

    static {
        ATTRIBUTE_MAPPERS.put("userName", new UserAttributeMapper(User::setUserName));
        ATTRIBUTE_MAPPERS.put("emails[0].value", new UserAttributeMapper(User::setEmail));
        ATTRIBUTE_MAPPERS.put("name.givenName", new UserAttributeMapper(User::setFirstName));
        ATTRIBUTE_MAPPERS.put("name.familyName", new UserAttributeMapper(User::setLastName));
        ATTRIBUTE_MAPPERS.put("externalId", new UserAttributeMapper(User::setExternalId));
        ATTRIBUTE_MAPPERS.put("nickName", new UserAttributeMapper(User::setNickName));
        ATTRIBUTE_MAPPERS.put("active", new AttributeMapper<>(
                new ModelAttributeMapper<>((model, name) -> String.valueOf(model.isEnabled()), (model, name, value) -> model.setEnabled(Boolean.parseBoolean(value))),
                new ResourceTypeAttributeMapper<>((user, value) -> user.setActive(Boolean.parseBoolean(value)))));
    }


    public UserCoreSchema(KeycloakSession session) {
        super(session, ATTRIBUTE_MAPPERS);
    }
}
