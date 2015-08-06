package org.keycloak.testsuite.auth.page.login;

import org.jboss.arquillian.graphene.page.Page;

/**
 *
 * @author tkyjovsk
 */
public class UpdatePassword extends Authenticate {

    @Page
    private UpdatePasswordFields updateForm;

    public UpdatePasswordFields updateForm() {
        return updateForm;
    }

    @Override
    public LoginForm loginForm() {
        throw new UnsupportedOperationException("UpdatePassword page doesn't contain LoginForm. Use UpdatePassword.updateForm() instead.");
    }
    
    public void updatePassword(String password) {
        updateForm.setNewPassword(password);
        updateForm.setConfirmPassword(password);
        submit();
    }

}
