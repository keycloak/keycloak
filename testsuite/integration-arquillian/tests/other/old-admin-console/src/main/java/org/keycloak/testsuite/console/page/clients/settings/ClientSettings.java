package org.keycloak.testsuite.console.page.clients.settings;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.clients.Client;

/**
 *
 * @author tkyjovsk
 */
public class ClientSettings extends Client {

    @Page
    private ClientSettingsForm form;

    public ClientSettingsForm form() {
        return form;
    }

}
