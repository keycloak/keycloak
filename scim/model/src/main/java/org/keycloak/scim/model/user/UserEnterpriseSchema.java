package org.keycloak.scim.model.user;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.user.EnterpriseUser;
import org.keycloak.scim.resource.user.EnterpriseUser.Manager;
import org.keycloak.scim.resource.user.User;

public final class UserEnterpriseSchema extends AbstractUserSchema {

    private static final Map<String, AttributeMapper<UserModel, User>> ATTRIBUTE_MAPPERS = new HashMap<>();

    static {
        ATTRIBUTE_MAPPERS.put("employeeNumber", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper(EnterpriseUser::setEmployeeNumber)));
        ATTRIBUTE_MAPPERS.put("costCenter", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper(EnterpriseUser::setCostCenter)));
        ATTRIBUTE_MAPPERS.put("organization", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper(EnterpriseUser::setOrganization)));
        ATTRIBUTE_MAPPERS.put("division", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper(EnterpriseUser::setDivision)));
        ATTRIBUTE_MAPPERS.put("department", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper(EnterpriseUser::setDepartment)));
        ATTRIBUTE_MAPPERS.put("manager.value", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper((user, value) -> {
            Manager manager = user.getManager();

            if (manager == null) {
                manager = new Manager();
                user.setManager(manager);
            }

            manager.setValue(value);
        })));
        ATTRIBUTE_MAPPERS.put("manager.displayName", new UserAttributeMapper(new EnterpriseUserResourceTypeAttributeMapper((user, value) -> {
            Manager manager = user.getManager();

            if (manager == null) {
                manager = new Manager();
                user.setManager(manager);
            }

            manager.setDisplayName(value);
        })));
    }

    public UserEnterpriseSchema(KeycloakSession session) {
        super(session, ATTRIBUTE_MAPPERS);
    }

    private static class EnterpriseUserResourceTypeAttributeMapper extends ResourceTypeAttributeMapper<User> {

        public EnterpriseUserResourceTypeAttributeMapper(BiConsumer<EnterpriseUser, String> setter) {
            super((user, value) -> {
                EnterpriseUser enterpriseUser = user.getEnterpriseUser();

                if (enterpriseUser == null) {
                    enterpriseUser = new EnterpriseUser();
                    user.setEnterpriseUser(enterpriseUser);
                }

                setter.accept(enterpriseUser, value);
            });
        }
    }
}
