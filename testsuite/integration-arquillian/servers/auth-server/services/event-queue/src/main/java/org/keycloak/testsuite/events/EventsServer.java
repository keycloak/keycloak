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

package org.keycloak.testsuite.events;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletContainer.Factory;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class EventsServer {

    private static final Logger log = Logger.getLogger(EventsServer.class.getName());

    private String rootPath = "/";
    private int port;
    private Undertow server;

    public EventsServer() {
        int eventsPort = Integer.parseInt(System.getProperty("auth.server.events.http.port", "8089"));
        int portOffset = Integer.parseInt(System.getProperty("auth.server.port.offset", "0"));
        int jbossPortOffset = Integer.parseInt(System.getProperty("jboss.socket.binding.port-offset", "-1"));

        log.fine("Configuration:");
        log.fine("  auth.server.events.http.port: " + eventsPort);
        log.fine("  auth.server.port.offset: " + portOffset);
        log.fine("  jboss.socket.binding.port-offset: " + jbossPortOffset);
        port = eventsPort + (jbossPortOffset != -1 ? jbossPortOffset : portOffset);
    }

    public void start() {

        PathHandler root = new PathHandler();
        this.server = Undertow.builder().addHttpListener(port, "localhost").setHandler(root).build();
        this.server.start();

        ServletContainer container = Factory.newInstance();

        DeploymentInfo di = new DeploymentInfo();
        di.setClassLoader(getClass().getClassLoader());
        di.setContextPath(rootPath);
        di.setDeploymentName("testing-event-queue");

        FilterInfo filter = Servlets.filter("EventsFilter", AssertEventsServletFilter.class);
        di.addFilter(filter);
        di.addFilterUrlMapping("EventsFilter", "/event-queue", DispatcherType.REQUEST);
        di.addFilterUrlMapping("EventsFilter", "/clear-event-queue", DispatcherType.REQUEST);

        DeploymentManager manager = container.addDeployment(di);
        manager.deploy();

        try {
            root.addPrefixPath(rootPath, manager.start());
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        log.info("Started EventsServer on port: " + port);
    }

    public void stop() {
        this.server.stop();
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
