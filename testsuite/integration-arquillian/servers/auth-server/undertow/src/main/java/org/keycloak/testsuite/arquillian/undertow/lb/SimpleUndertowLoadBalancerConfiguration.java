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

package org.keycloak.testsuite.arquillian.undertow.lb;

import org.arquillian.undertow.UndertowContainerConfiguration;
import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SimpleUndertowLoadBalancerConfiguration extends UndertowContainerConfiguration {

    public static final int DEFAULT_HTTPS_PORT = Integer.valueOf(System.getProperty("auth.server.https.port", "8543"));

    protected static final Logger log = Logger.getLogger(SimpleUndertowLoadBalancerConfiguration.class);

    private String nodes = SimpleUndertowLoadBalancer.DEFAULT_NODES_HTTP;
    private int bindHttpPortOffset = 0;
    private int bindHttpsPortOffset = 0;
    private int bindHttpsPort = DEFAULT_HTTPS_PORT;

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
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

    @Override
    public void validate() throws ConfigurationException {
        super.validate();

        try {
            SimpleUndertowLoadBalancer.parseNodes(nodes);
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }

        setBindHttpPort(getBindHttpPort() + bindHttpPortOffset);
        setBindHttpsPort(getBindHttpsPort() + bindHttpsPortOffset);
        log.info("SimpleUndertowLoadBalancer will listen on ports: " + getBindHttpPort() + " " + getBindHttpsPort());

    }
}
