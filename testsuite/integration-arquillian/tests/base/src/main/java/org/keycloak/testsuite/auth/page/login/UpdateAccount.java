package org.keycloak.testsuite.auth.page.login;

import org.keycloak.testsuite.auth.page.account.AccountFields;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.UserRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class UpdateAccount extends Authenticate {

    @Page
    private AccountFields accountFields;

    public void updateAccount(UserRepresentation user) {
        updateAccount(user.getEmail(), user.getFirstName(), user.getLastName());
    }
    
    public void updateAccount(String email, String firstName, String lastName) {
        accountFields.setEmail(email);
        accountFields.setFirstName(firstName);
        accountFields.setLastName(lastName);
        submit();
    }

}
