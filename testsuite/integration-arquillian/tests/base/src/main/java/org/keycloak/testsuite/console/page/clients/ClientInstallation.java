package org.keycloak.testsuite.console.page.clients;

/**
 *
 * @author tkyjovsk
 */
public class ClientInstallation extends Client {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/installation";
    }

}
