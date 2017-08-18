/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.arquillian.jmx;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

/**
 *
 * @author hmlnarik
 */
public class JmxConnectorRegistryCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<JmxConnectorRegistry> connectorRegistry;

    public void configureJmxConnectorRegistry(@Observes BeforeSuite event) {
        if (connectorRegistry.get() == null) {
            connectorRegistry.set(new JmxConnectorRegistry() {

                private volatile ConcurrentMap<JMXServiceURL, JMXConnector> connectors = new ConcurrentHashMap<>();

                @Override
                public JMXConnector getConnection(JMXServiceURL url) {
                    JMXConnector res = connectors.get(url);
                    if (res == null) {
                        try {
                            final JMXConnector conn = JMXConnectorFactory.newJMXConnector(url, null);
                            res = connectors.putIfAbsent(url, conn);
                            if (res == null) {
                                res = conn;
                            }
                            res.connect();
                        } catch (IOException ex) {
                            throw new RuntimeException("Could not instantiate JMX connector for " + url, ex);
                        }
                    }
                    return res;
                }

                @Override
                public void closeAll() {
                    connectors.values().forEach(c -> { try { c.close(); } catch (IOException e) {} });
                    connectors.clear();
                }
            });
        }
    }
}
