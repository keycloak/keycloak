package org.keycloak.scim.model.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.user.EnterpriseUser;
import org.keycloak.scim.resource.user.EnterpriseUser.Manager;
import org.keycloak.scim.resource.user.User;

import static org.keycloak.scim.resource.Scim.ENTERPRISE_USER_SCHEMA;

public final class UserEnterpriseModelSchema extends AbstractUserModelSchema {

    private static final List<Attribute<UserModel, User>> ATTRIBUTE_MAPPERS = new ArrayList<>();

    static {
        ATTRIBUTE_MAPPERS.add(new Attribute<>("employeeNumber", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper(EnterpriseUser::setEmployeeNumber))));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("costCenter", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper(EnterpriseUser::setCostCenter))));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("organization", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper(EnterpriseUser::setOrganization))));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("division", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper(EnterpriseUser::setDivision))));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("department", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper(EnterpriseUser::setDepartment))));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("manager.value", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper((user, value) -> {
            Manager manager = user.getManager();

            if (manager == null) {
                manager = new Manager();
                user.setManager(manager);
            }

            manager.setValue(value);
        }))));
        ATTRIBUTE_MAPPERS.add(new Attribute<>("manager.displayName", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper((user, value) -> {
            Manager manager = user.getManager();

            if (manager == null) {
                manager = new Manager();
                user.setManager(manager);
            }

            manager.setDisplayName(value);
        }))));
    }

    public UserEnterpriseModelSchema(KeycloakSession session) {
        super(session, ENTERPRISE_USER_SCHEMA, ATTRIBUTE_MAPPERS);
    }

    @Override
    public String getName() {
        return ENTERPRISE_USER_SCHEMA;
    }

    @Override
    public Map<String, Attribute<UserModel, User>> getAttributes() {
        return Map.of();
    }

    private static class EnterpriseUserResourceTypeAttributeMapper implements BiConsumer<User, String> {

        private final BiConsumer<EnterpriseUser, String> setter;

        public EnterpriseUserResourceTypeAttributeMapper(BiConsumer<EnterpriseUser, String> setter) {
            this.setter = setter;
        }

        @Override
        public void accept(User user, String value) {
            if (value == null) {
                return;
            }

            EnterpriseUser enterpriseUser = user.getEnterpriseUser();

            if (enterpriseUser == null) {
                enterpriseUser = new EnterpriseUser();
                user.setEnterpriseUser(enterpriseUser);
            }

            setter.accept(enterpriseUser, value);
        }
    }
}
