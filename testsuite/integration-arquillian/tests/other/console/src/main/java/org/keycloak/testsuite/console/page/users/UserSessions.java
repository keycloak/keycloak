package org.keycloak.testsuite.console.page.users;

/**
 *
 * @author tkyjovsk
 */
public class UserSessions extends User {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/sessions";
    }

}
