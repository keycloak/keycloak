package org.keycloak.testsuite.util;

import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author vramik
 * @author tkyjovsk
 */
public class LogChecker {

    private static final Logger log = Logger.getLogger(LogChecker.class);

    private static final String[] IGNORED = new String[] { ".*Jetty ALPN support not found.*" };

    public static void checkServerLog(File logFile) throws IOException {
        log.info(String.format("Checking server log: '%s'", logFile.getAbsolutePath()));
        String[] logContent = FileUtils.readFileToString(logFile).split("\n");

        for (String log : logContent) {
            boolean containsError = log.contains("ERROR") || log.contains("SEVERE") || log.contains("Exception ");
            //There is expected string "Exception" in server log: Adding provider
            //singleton org.keycloak.services.resources.ModelExceptionMapper
            if (containsError) {
                boolean ignore = false;
                for (String i : IGNORED) {
                    if (log.matches(i)) {
                        ignore = true;
                        break;
                    }
                }
                if (!ignore) {
                    throw new RuntimeException(String.format("Server log file contains ERROR: '%s'", log));
                }
            }
        }

    }

    public static void checkJBossServerLog(String jbossHome) throws IOException {
        checkServerLog(new File(jbossHome + "/standalone/log/server.log"));
    }

}
