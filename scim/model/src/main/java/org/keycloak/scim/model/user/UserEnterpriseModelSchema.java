package org.keycloak.scim.model.user;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.user.EnterpriseUser;
import org.keycloak.scim.resource.user.User;

import static org.keycloak.scim.resource.Scim.ENTERPRISE_USER_SCHEMA;

public final class UserEnterpriseModelSchema extends AbstractUserModelSchema {

    public UserEnterpriseModelSchema(KeycloakSession session) {
        super(session, ENTERPRISE_USER_SCHEMA);
    }

    @Override
    public String getName() {
        return ENTERPRISE_USER_SCHEMA;
    }

    @Override
    public boolean isCore() {
        return false;
    }

    @Override
    protected Map<String, Attribute<UserModel, User>> doGetAttributes() {
        return new ArrayList<>(Attribute.<UserModel, User>complex("enterpriseUser", EnterpriseUser.class)
                .modelAttributeResolver(this::createModelAttributeResolver)
                .withAttribute("employeeNumber", UserModel::setSingleAttribute)
                .withAttribute("costCenter", UserModel::setSingleAttribute)
                .withAttribute("organization", UserModel::setSingleAttribute)
                .withAttribute("division", UserModel::setSingleAttribute)
                .withAttribute("department", UserModel::setSingleAttribute)
                .withAttribute("manager.value", "manager", UserModel::setSingleAttribute)
                .withAttribute("manager.displayName", UserModel::setSingleAttribute)
                .build()).stream().collect(Collectors.toMap(Attribute::getName, Function.identity()));
    }
}
