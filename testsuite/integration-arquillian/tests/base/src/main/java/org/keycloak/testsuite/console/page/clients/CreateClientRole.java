package org.keycloak.testsuite.console.page.clients;

import static org.keycloak.testsuite.console.page.clients.Client.ID;
import org.keycloak.testsuite.console.page.roles.CreateRole;

/**
 *
 * @author tkyjovsk
 */
public class CreateClientRole extends CreateRole {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/clients/{" + ID + "}";
    }

    public void setClientId(String id) {
        setUriParameter(ID, id);
    }

    public String getClientId() {
        return getUriParameter(ID).toString();
    }

}
