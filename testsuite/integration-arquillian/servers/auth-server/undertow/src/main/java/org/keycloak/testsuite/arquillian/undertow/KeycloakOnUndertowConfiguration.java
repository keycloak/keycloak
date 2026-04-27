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

package org.keycloak.testsuite.arquillian.undertow;

import org.arquillian.undertow.UndertowContainerConfiguration;
import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.logging.Logger;

public class KeycloakOnUndertowConfiguration extends UndertowContainerConfiguration {

    public static final int DEFAULT_HTTPS_PORT = Integer.valueOf(System.getProperty("auth.server.https.port", "8543"));

    protected static final Logger log = Logger.getLogger(KeycloakOnUndertowConfiguration.class);

    private int workerThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2) * 8;
    private String resourcesHome;
    private boolean remoteMode;
    private String route;
    private String keycloakConfigPropertyOverrides;

    private int bindHttpPortOffset = 0;
    private int bindHttpsPortOffset = 0;
    private int bindHttpsPort = DEFAULT_HTTPS_PORT;

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public String getResourcesHome() {
        return resourcesHome;
    }

    public void setResourcesHome(String resourcesHome) {
        this.resourcesHome = resourcesHome;
    }

    public int getBindHttpPortOffset() {
        return bindHttpPortOffset;
    }

    public void setBindHttpPortOffset(int bindHttpPortOffset) {
        this.bindHttpPortOffset = bindHttpPortOffset;
    }

    public int getBindHttpsPortOffset() {
        return bindHttpsPortOffset;
    }

    public void setBindHttpsPortOffset(int bindHttpsPortOffset) {
        this.bindHttpsPortOffset = bindHttpsPortOffset;
    }

    public int getBindHttpsPort() {
        return this.bindHttpsPort;
    }

    public void setBindHttpsPort(int bindHttpsPort) {
        this.bindHttpsPort = bindHttpsPort;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public boolean isRemoteMode() {
        return remoteMode;
    }

    public void setRemoteMode(boolean remoteMode) {
        this.remoteMode = remoteMode;
    }

    @Override
    public void validate() throws ConfigurationException {
        super.validate();

        int basePort = getBindHttpPort();
        int newPort = basePort + bindHttpPortOffset;
        setBindHttpPort(newPort);

        int baseHttpsPort = getBindHttpsPort();
        int newHttpsPort = baseHttpsPort + bindHttpsPortOffset;
        setBindHttpsPort(newHttpsPort);

        log.info("KeycloakOnUndertow will listen for http on port: " + newPort + " and for https on port: " + newHttpsPort);
        
        // TODO validate workerThreads
        
    }
    
}
