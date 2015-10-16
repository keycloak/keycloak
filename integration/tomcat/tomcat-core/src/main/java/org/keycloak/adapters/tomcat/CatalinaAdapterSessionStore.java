package org.keycloak.adapters.tomcat;

import org.apache.catalina.connector.Request;
import org.keycloak.adapters.spi.AdapterSessionStore;

import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CatalinaAdapterSessionStore implements AdapterSessionStore {
    protected Request request;
    protected AbstractKeycloakAuthenticatorValve valve;

    public CatalinaAdapterSessionStore(Request request, AbstractKeycloakAuthenticatorValve valve) {
        this.request = request;
        this.valve = valve;
    }

    public void saveRequest() {
        try {
            valve.keycloakSaveRequest(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean restoreRequest() {
        return valve.keycloakRestoreRequest(request);
    }
}
