/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.arquillian.jetty;

import org.jboss.arquillian.container.jetty.embedded_9.JettyEmbeddedConfiguration;
import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

public class JettyAppServerConfiguration extends JettyEmbeddedConfiguration {

    private int bindHttpPortOffset = 0;
    private int bindHttpsPortOffset = 0;
    private int bindHttpPort = 8280;
    private int bindHttpsPort = 8643;
    private String bindAddress = "localhost";

    @Override
    public void validate() throws ConfigurationException {
        setBindHttpPort(bindHttpPort + bindHttpPortOffset);
        setBindHttpsPort(bindHttpsPort + bindHttpsPortOffset);
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

    public int getBindHttpPort() {
        return bindHttpPort;
    }

    public void setBindHttpPort(int bindHttpPort) {
        this.bindHttpPort = bindHttpPort;
    }

    public int getBindHttpsPort() {
        return bindHttpsPort;
    }

    public void setBindHttpsPort(int bindHttpsPort) {
        this.bindHttpsPort = bindHttpsPort;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }
}
