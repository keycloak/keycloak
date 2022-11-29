package org.keycloak.testsuite.console.page.clients.installation;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.clients.Client;

/**
 *
 * @author tkyjovsk
 */
public class ClientInstallation extends Client {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/installation";
    }
    
    @Page
    private ClientInstallationForm form;
    
    public ClientInstallationForm form() {
        return form;
    }

}
