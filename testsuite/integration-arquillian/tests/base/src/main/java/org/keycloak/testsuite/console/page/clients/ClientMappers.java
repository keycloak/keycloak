package org.keycloak.testsuite.console.page.clients;

/**
 *
 * @author tkyjovsk
 */
public class ClientMappers extends Client {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/mappers";
    }

}
