package org.keycloak.testsuite.console.page;

import javax.ws.rs.core.UriBuilder;
import static org.keycloak.testsuite.console.page.AdminConsoleRealm.CONSOLE_REALM;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 *
 * @author tkyjovsk
 */
public class AdminConsoleCreate extends AdminConsole {

    public static final String ENTITY = "entity";

    public AdminConsoleCreate() {
        setUriParameter(CONSOLE_REALM, TEST);
    }
    
    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("/");
    }
    
    @Override
    public String getUriFragment() {
        return "/create/{" + ENTITY + "}/{" + CONSOLE_REALM + "}";
    }

    public AdminConsoleCreate setEntity(String entity) {
        setUriParameter(ENTITY, entity);
        return this;
    }

    public String getEntity() {
        return getUriParameter(ENTITY).toString();
    }

    public AdminConsoleCreate setConsoleRealm(String consoleRealm) {
        setUriParameter(CONSOLE_REALM, consoleRealm);
        return this;
    }

    public String getConsoleRealm() {
        return getUriParameter(CONSOLE_REALM).toString();
    }

}
