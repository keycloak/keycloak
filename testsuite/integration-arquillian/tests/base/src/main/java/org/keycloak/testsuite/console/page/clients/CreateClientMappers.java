package org.keycloak.testsuite.console.page.clients;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.AdminConsoleCreate;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class CreateClientMappers extends AdminConsoleCreate {

    @Page
    private CreateClientMappersForm form;

    public CreateClientMappers() {
        setEntity("mappers");
    }

    public CreateClientMappersForm form() {
        return form;
    }
}
