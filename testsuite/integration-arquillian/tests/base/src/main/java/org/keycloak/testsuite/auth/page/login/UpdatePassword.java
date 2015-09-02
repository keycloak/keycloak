package org.keycloak.testsuite.auth.page.login;

import org.keycloak.testsuite.auth.page.account.PasswordFields;
import org.jboss.arquillian.graphene.page.Page;

/**
 *
 * @author tkyjovsk
 */
public class UpdatePassword extends Authenticate {

    @Page
    private PasswordFields passwordFields;

    public void updatePasswords(String newPassword, String confirmPassword) {
        passwordFields.setNewPassword(newPassword);
        passwordFields.setConfirmPassword(confirmPassword);
        submit();
    }

}
