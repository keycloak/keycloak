package org.keycloak.testsuite.arquillian;

import java.util.HashMap;
import java.util.Map;
import static org.keycloak.testsuite.util.MailServerConfiguration.*;

/**
 *
 * @author tkyjovsk
 */
public final class SuiteContext {

    private boolean adminPasswordUpdated;
    private final Map<String, String> smtpServer = new HashMap<>();
    
    public SuiteContext() {
        this.adminPasswordUpdated = false;
        smtpServer.put("from", FROM);
        smtpServer.put("host", HOST);
        smtpServer.put("port", PORT);
    }

    public boolean isAdminPasswordUpdated() {
        return adminPasswordUpdated;
    }

    public void setAdminPasswordUpdated(boolean adminPasswordUpdated) {
        this.adminPasswordUpdated = adminPasswordUpdated;
    }

    public Map<String, String> getSmtpServer() {
        return smtpServer;
    }
}
