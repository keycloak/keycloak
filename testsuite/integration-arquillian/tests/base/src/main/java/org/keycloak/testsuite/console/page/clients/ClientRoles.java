package org.keycloak.testsuite.console.page.clients;

/**
 *
 * @author tkyjovsk
 */
public class ClientRoles extends Client {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/roles";
    }

}
