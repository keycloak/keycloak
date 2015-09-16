package org.keycloak.testsuite.console.page.clients;

/**
 *
 * @author tkyjovsk
 */
public class ClientRevocation extends Client {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/revocation";
    }

}
