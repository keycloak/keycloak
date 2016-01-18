package org.keycloak.testsuite.console.page.clients.scope;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.clients.Client;
import org.keycloak.testsuite.console.page.roles.RoleCompositeRoles;

/**
 *
 * @author tkyjovsk
 */
public class ClientScope extends Client {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/scope-mappings";
    }
    
    @Page
    private ClientScopeForm clientScopeForm;
    
    @Page
    private RoleCompositeRoles scopeRoleForm;

    public ClientScopeForm scopeForm() {
        return clientScopeForm;
    }
    
    public RoleCompositeRoles roleForm() {
        return scopeRoleForm;
    }
}
