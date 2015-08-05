package org.keycloak.testsuite.console.page.clients;

import static org.keycloak.testsuite.console.page.clients.Client.CLIENT_ID;
import org.keycloak.testsuite.console.page.roles.CreateRole;

/**
 *
 * @author tkyjovsk
 */
public class CreateClientRole extends CreateRole {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/clients/{" + CLIENT_ID + "}";
    }

    public void setClientId(String id) {
        setUriParameter(CLIENT_ID, id);
    }

    public String getClientId() {
        return getUriParameter(CLIENT_ID).toString();
    }

}
