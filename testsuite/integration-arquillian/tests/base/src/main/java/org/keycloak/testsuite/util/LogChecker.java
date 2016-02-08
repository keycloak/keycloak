package org.keycloak.testsuite.util;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;

/**
 *
 * @author vramik
 * @author tkyjovsk
 */
public class LogChecker {

    private static final Logger log = Logger.getLogger(LogChecker.class);

    public static void checkServerLog(File logFile) throws IOException {
        log.info(String.format("Checking server log: '%s'", logFile.getAbsolutePath()));
        String logContent = FileUtils.readFileToString(logFile);

        boolean containsError
                = logContent.contains("ERROR")
                || logContent.contains("SEVERE")
                || logContent.contains("Exception ");

        //There is expected string "Exception" in server log: Adding provider 
        //singleton org.keycloak.services.resources.ModelExceptionMapper
        if (containsError) {
            throw new RuntimeException(String.format("Server log file contains ERROR: '%s'", logFile.getPath()));
        }
    }

    public static void checkJBossServerLog(String jbossHome) throws IOException {
        checkServerLog(new File(jbossHome + "/standalone/log/server.log"));
    }

}
