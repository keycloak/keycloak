package org.keycloak.testsuite.console.page.federation;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.AdminConsoleCreate;

/**
 *
 * @author tkyjovsk
 */
public class CreateLdapUserProvider extends AdminConsoleCreate {

    @Page
    private LdapUserProviderForm form;

    public CreateLdapUserProvider() {
        setEntity("user-storage");
    }

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/providers/ldap";
    }

    public LdapUserProviderForm form() {
        return form;
    }
}
