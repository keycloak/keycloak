package org.keycloak.testsuite.arquillian;

import java.util.HashMap;
import java.util.Map;
import static org.keycloak.testsuite.util.MailServerConfiguration.*;

/**
 *
 * @author tkyjovsk
 */
public final class SuiteContext {

    private final Map<String, String> smtpServer = new HashMap<>();
    
    public SuiteContext() {
        smtpServer.put("from", FROM);
        smtpServer.put("host", HOST);
        smtpServer.put("port", PORT);
    }

    public Map<String, String> getSmtpServer() {
        return smtpServer;
    }
}
