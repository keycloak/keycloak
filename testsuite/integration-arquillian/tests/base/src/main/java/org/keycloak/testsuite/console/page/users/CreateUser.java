package org.keycloak.testsuite.console.page.users;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.AdminConsole;
import static org.keycloak.testsuite.console.page.AdminConsoleRealm.CONSOLE_REALM;

/**
 *
 * @author tkyjovsk
 */
public class CreateUser extends AdminConsole {

    @Override
    public String getUriFragment() {
        return "/create/user/{" + CONSOLE_REALM + "}";
    }
    
    @Page
    private UserAttributesForm form;
    
    public UserAttributesForm form() {
        return form;
    }

}
