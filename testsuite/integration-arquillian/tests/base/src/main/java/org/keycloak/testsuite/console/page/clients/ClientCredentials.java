package org.keycloak.testsuite.console.page.clients;

/**
 *
 * @author tkyjovsk
 */
public class ClientCredentials extends Client {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/credentials";
    }

}
