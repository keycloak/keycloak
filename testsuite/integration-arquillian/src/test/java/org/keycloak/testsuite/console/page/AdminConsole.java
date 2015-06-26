package org.keycloak.testsuite.console.page;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author tkyjovsk
 */
public class AdminConsole extends AdminRoot {

    public AdminConsole() {
        setTemplateValue("consoleRealm", Realm.MASTER);
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("{consoleRealm}/console");
    }

}
