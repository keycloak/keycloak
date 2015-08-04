package org.keycloak.testsuite.console.page.federation;

import org.keycloak.testsuite.console.page.AdminConsoleCreate;

/**
 *
 * @author tkyjovsk
 */
public class CreateLdapUserProvider extends AdminConsoleCreate {

    public CreateLdapUserProvider() {
        setEntity("user-federation");
    }

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/providers/ldap";
    }

}
