package org.keycloak.testsuite.console.page.clients;

/**
 *
 * @author tkyjovsk
 */
public class ClientScopeMappings extends Client {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/scope-mappings";
    }

}
