package org.keycloak.testsuite.console.page.clients.credentials;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.clients.Client;

/**
 *
 * @author tkyjovsk
 */
public class ClientCredentials extends Client {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/credentials";
    }
    
    @Page
    private ClientCredentialsForm form;

    public ClientCredentialsForm form() {
        return form;
    }

}
