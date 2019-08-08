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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 *
 * @author hmlnarik
 */
public class LogChecker {

    public static String[] getJBossServerLogFiles(String jbossHome) {
        boolean domain = System.getProperty("auth.server.config.property.name", "standalone").contains("domain");
        if (domain) {
            return new String[] {
              jbossHome + "/domain/log/process-controller.log",
              jbossHome + "/domain/log/host-controller.log",
              jbossHome + "/domain/servers/load-balancer/log/server.log",
              jbossHome + "/domain/servers/server-one/log/server.log"
            };
        } else {
            return new String[] {
                jbossHome + "/standalone/log/server.log"
            };
        }
    }

    public static TextFileChecker getJBossServerLogsChecker(String jbossHome) throws IOException {
        String[] pathsToCheck = getJBossServerLogFiles(jbossHome);
        Path[] pathsArray = Arrays.stream(pathsToCheck).map(File::new).map(File::toPath).toArray(Path[]::new);

        return new TextFileChecker(pathsArray);
    }

}