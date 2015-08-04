package org.keycloak.testsuite.console.page.clients;

/**
 *
 * @author tkyjovsk
 */
public class ClientSessions extends Client {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/sessions";
    }

}
