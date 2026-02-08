package org.keycloak.scim.model.user;

import java.util.function.BiConsumer;

import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.schema.AbstractScimSchema.AttributeMapper;
import org.keycloak.scim.resource.schema.AbstractScimSchema.ModelAttributeMapper;
import org.keycloak.scim.resource.schema.AbstractScimSchema.ResourceTypeAttributeMapper;
import org.keycloak.scim.resource.user.User;

public class UserAttributeMapper extends AttributeMapper<UserModel, User> {

    public UserAttributeMapper(BiConsumer<User, String> setter) {
        super(new ModelAttributeMapper<>(UserModel::getFirstAttribute, UserModel::setSingleAttribute), new ResourceTypeAttributeMapper<>(setter));
    }

    public UserAttributeMapper(ResourceTypeAttributeMapper<User> setter) {
        super(new ModelAttributeMapper<>(UserModel::getFirstAttribute, UserModel::setSingleAttribute), setter);
    }
}
