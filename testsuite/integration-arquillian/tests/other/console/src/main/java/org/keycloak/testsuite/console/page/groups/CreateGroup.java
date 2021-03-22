package org.keycloak.testsuite.console.page.groups;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.AdminConsoleCreate;

/**
 *
 * @author clementcur
 */
public class CreateGroup extends AdminConsoleCreate {

    public CreateGroup() {
        setEntity("group");
    }
    
    @Page
    private CreateGroupForm form;
    
    public CreateGroupForm form() {
        return form;
    }

}
