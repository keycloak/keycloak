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

import org.apache.log4j.Level;
import org.jboss.logging.Logger;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

/**
 * This is executed when test is run from maven (maven-surefire-plugin), but not when it is run from IDE. That allows to run some actions, which should be
 * executed just for maven build (eg. disable logging)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class NonIDERunListener extends RunListener {

    private static final Logger log = Logger.getLogger(NonIDERunListener.class);

    private static final String KEYCLOAK_LOGGING_LEVEL_NAME = "keycloak.logging.level";

    @Override
    public void testRunStarted(Description description) throws Exception {
        disableKeycloakLogging();
    }

    private void disableKeycloakLogging() {
        String loggingLevel = System.getProperty(KEYCLOAK_LOGGING_LEVEL_NAME, "").toLowerCase();
        if (loggingLevel.isEmpty()) {

            log.infof("Setting %s to off. Keycloak server logging will be disabled", KEYCLOAK_LOGGING_LEVEL_NAME);
            System.setProperty(KEYCLOAK_LOGGING_LEVEL_NAME, "off");
            org.apache.log4j.Logger.getLogger("org.keycloak").setLevel(Level.OFF);
        } else {
            switch (loggingLevel) {
                case "debug":
                    org.apache.log4j.Logger.getLogger("org.keycloak").setLevel(Level.DEBUG);
                    break;
                case "trace":
                    org.apache.log4j.Logger.getLogger("org.keycloak").setLevel(Level.TRACE);
                    break;
                case "all":
                    org.apache.log4j.Logger.getLogger("org.keycloak").setLevel(Level.ALL);
                    break;
                default:
                    org.apache.log4j.Logger.getLogger("org.keycloak").setLevel(Level.INFO);
                    break;
            }
        }
    }

}
