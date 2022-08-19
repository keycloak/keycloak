package org.keycloak.testsuite.console.page.federation;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.AdminConsoleCreate;

/**
 *
 * @author pdrozd
 */
public class CreateKerberosUserProvider extends AdminConsoleCreate {

    @Page
    private KerberosUserProviderForm form;

    public CreateKerberosUserProvider() {
        setEntity("user-storage");
    }

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/providers/kerberos";
    }

    public KerberosUserProviderForm form() {
        return form;
    }

}
