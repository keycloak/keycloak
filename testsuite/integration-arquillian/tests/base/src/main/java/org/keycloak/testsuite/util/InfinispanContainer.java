/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.jboss.logging.Logger;
import org.keycloak.testsuite.arquillian.HotRodStoreTestEnricher;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfinispanContainer extends GenericContainer<InfinispanContainer> {

    private final Logger LOG = Logger.getLogger(getClass());
    private static final String PORT = System.getProperty("keycloak.connectionsHotRod.port", "11222");
    private static String HOST = System.getProperty(HotRodStoreTestEnricher.HOT_ROD_STORE_HOST_PROPERTY);
    private static final String USERNAME = System.getProperty("keycloak.connectionsHotRod.username", "admin");
    private static final String PASSWORD = System.getProperty("keycloak.connectionsHotRod.password", "admin");

    private static final String ZERO_TO_255
            = "(\\d{1,2}|(0|1)\\"
            + "d{2}|2[0-4]\\d|25[0-5])";
    private static final String IP_ADDRESS_REGEX
            = ZERO_TO_255 + "\\."
            + ZERO_TO_255 + "\\."
            + ZERO_TO_255 + "\\."
            + ZERO_TO_255;

    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("listening on (" + IP_ADDRESS_REGEX + "):" + PORT);

    public InfinispanContainer() {
        super("quay.io/infinispan/server:" + System.getProperty("infinispan.version"));
        withEnv("USER", USERNAME);
        withEnv("PASS", PASSWORD);
        withNetworkMode("host");

        withStartupTimeout(Duration.ofMinutes(5));
        waitingFor(Wait.forLogMessage(".*Infinispan Server.*started in.*", 1));
    }

    public String getHost() {
        if (HOST == null && this.isRunning()) {
            Matcher matcher = IP_ADDRESS_PATTERN.matcher(getLogs());
            if (!matcher.find()) {
                LOG.errorf("Cannot find IP address of the infinispan server in log:\\n%s ", getLogs());
                throw new IllegalStateException("Cannot find IP address of the Infinispan server. See test log for Infinispan container log.");
            }
            HOST = matcher.group(1);
        }

        return HOST;
    }

    @Override
    public void start() {
        super.start();
    }

    public String getPort() {
        return PORT;
    }

    public String getUsername() {
        return USERNAME;
    }

    public String getPassword() {
        return PASSWORD;
    }
}
