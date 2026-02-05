package org.keycloak.scim.model.user;

import java.util.function.BiConsumer;

import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.common.Name;
import org.keycloak.scim.resource.schema.attribute.AttributeMapper;
import org.keycloak.scim.resource.user.User;

public class UserNameAttributeMapper extends AttributeMapper<UserModel, User> {

    public UserNameAttributeMapper(BiConsumer<Name, String> setter) {
        super(UserModel::setSingleAttribute, (user, value) -> {
            if (value == null) {
                return;
            }

            Name name = user.getName();

            if (name == null) {
                name = new Name();
                user.setName(name);
            }

            setter.accept(name, value);
        });
    }
}
