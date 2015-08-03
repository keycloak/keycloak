package org.keycloak.testsuite.console.page.clients;

import org.jboss.arquillian.graphene.page.Page;

/**
 *
 * @author tkyjovsk
 */
public class ClientSettings extends Client {

    @Page
    private ClientForm form;

    public ClientForm form() {
        return form;
    }

}
