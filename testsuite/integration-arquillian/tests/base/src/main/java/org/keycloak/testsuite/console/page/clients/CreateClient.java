package org.keycloak.testsuite.console.page.clients;

import org.jboss.arquillian.graphene.page.Page;

/**
 *
 * @author tkyjovsk
 */
public class CreateClient {
    
    @Page
    private ClientForm form;
    
    public ClientForm form() {
        return form;
    }
    
}
