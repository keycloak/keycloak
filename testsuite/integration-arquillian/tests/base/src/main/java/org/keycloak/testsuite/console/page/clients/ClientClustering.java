package org.keycloak.testsuite.console.page.clients;

/**
 *
 * @author tkyjovsk
 */
public class ClientClustering extends Client {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/clustering";
    }

}
