/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

        for (String logText : logContent) {
            boolean containsError = logText.contains("ERROR") || logText.contains("SEVERE") || logText.contains("Exception ");
            //There is expected string "Exception" in server log: Adding provider
            //singleton org.keycloak.services.resources.ModelExceptionMapper
            if (containsError) {
                boolean ignore = false;
                for (String i : IGNORED) {
                    if (logText.matches(i)) {
                        ignore = true;
                        break;
                    }
                }
                if (!ignore) {
                    throw new RuntimeException(String.format("Server log file contains ERROR: '%s'", logText));
                }
            }
        }

    }

    public static void checkJBossServerLog(String jbossHome) throws IOException {
        boolean domain = System.getProperty("auth.server.config.property.name", "standalone").contains("domain");
        if (domain) {
            checkServerLog(new File(jbossHome + "/domain/log/process-controller.log"));
            checkServerLog(new File(jbossHome + "/domain/log/host-controller.log"));
            checkServerLog(new File(jbossHome + "/domain/servers/load-balancer/log/server.log"));
            checkServerLog(new File(jbossHome + "/domain/servers/server-one/log/server.log"));
        } else {
            checkServerLog(new File(jbossHome + "/standalone/log/server.log"));
        }
    }

}