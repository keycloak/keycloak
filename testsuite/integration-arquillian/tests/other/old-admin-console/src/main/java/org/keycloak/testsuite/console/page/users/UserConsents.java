package org.keycloak.testsuite.console.page.users;

/**
 *
 * @author tkyjovsk
 */
public class UserConsents extends User {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/consents";
    }

}
