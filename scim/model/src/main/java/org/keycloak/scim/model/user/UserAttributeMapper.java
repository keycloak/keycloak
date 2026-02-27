package org.keycloak.scim.model.user;

import java.util.function.BiConsumer;

import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.schema.attribute.AttributeMapper;
import org.keycloak.scim.resource.user.User;

public class UserAttributeMapper extends AttributeMapper<UserModel, User> {

    public UserAttributeMapper(BiConsumer<User, String> setter) {
        super(UserModel::setSingleAttribute, setter);
    }
}
