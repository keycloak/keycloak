package org.keycloak.testsuite.console.page.users;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.admin.client.resource.RoleMappingResource;

/**
 *
 * @author tkyjovsk
 */
public class UserRoleMappings extends User {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "role-mappings";
    }

    @Page
    private UserRoleMappingsForm form;

    public UserRoleMappingsForm form() {
        return form;
    }

}
