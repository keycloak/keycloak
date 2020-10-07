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

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.keycloak.testsuite.arquillian.LoadBalancerController;

/**
 * Arquillian container over {@link SimpleUndertowLoadBalancer}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SimpleUndertowLoadBalancerContainer implements DeployableContainer<SimpleUndertowLoadBalancerConfiguration>, LoadBalancerController {

    private static final Logger log = Logger.getLogger(SimpleUndertowLoadBalancerContainer.class);

    private SimpleUndertowLoadBalancerConfiguration configuration;
    private SimpleUndertowLoadBalancer container;

    @Override
    public Class<SimpleUndertowLoadBalancerConfiguration> getConfigurationClass() {
        return SimpleUndertowLoadBalancerConfiguration.class;
    }

    @Override
    public void setup(SimpleUndertowLoadBalancerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws LifecycleException {
        this.container = new SimpleUndertowLoadBalancer(configuration.getBindAddress(), configuration.getBindHttpPort(), configuration.getBindHttpsPort(), configuration.getNodes());
        this.container.start();
    }

    @Override
    public void stop() throws LifecycleException {
        log.info("Going to stop loadbalancer");
        this.container.stop();
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.1");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void enableAllBackendNodes() {
        this.container.enableAllBackendNodes();
    }

    @Override
    public void disableAllBackendNodes() {
        this.container.disableAllBackendNodes();
    }

    @Override
    public void enableBackendNodeByName(String nodeName) {
        this.container.enableBackendNodeByName(nodeName);
    }

    @Override
    public void disableBackendNodeByName(String nodeName) {
        this.container.disableBackendNodeByName(nodeName);
    }
}
