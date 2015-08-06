package org.keycloak.testsuite.auth.page.login;

import org.jboss.arquillian.graphene.page.Page;

/**
 *
 * @author tkyjovsk
 */
public class UpdateAccount extends Authenticate {

    @Page
    private UpdateAccountFields updateForm;

    public UpdateAccountFields updateForm() {
        return updateForm;
    }

}
