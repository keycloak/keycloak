/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.arquillian;

import java.lang.annotation.Annotation;
import org.jboss.arquillian.container.spi.event.StartSuiteContainers;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.keycloak.testsuite.util.InfinispanContainer;


public class HotRodContainerProvider implements ResourceProvider {

    public static final String HOT_ROD_STORE_HOST_PROPERTY = "keycloak.connectionsHotRod.host";

    public static final boolean HOT_ROD_START_CONTAINER = Boolean.parseBoolean(System.getProperty("keycloak.testsuite.start-hotrod-container", "false"));

    private static InfinispanContainer infinispanContainer;

    public void beforeContainerStarted(@Observes(precedence = 1) StartSuiteContainers event) {
        if (!HOT_ROD_START_CONTAINER) return;
        infinispanContainer = new InfinispanContainer();
        infinispanContainer.start();

        // Add env variable, so it can be picked up by Keycloak
        System.setProperty(HOT_ROD_STORE_HOST_PROPERTY, infinispanContainer.getHost());
    }

    public void afterSuite(@Observes(precedence = 4) AfterSuite event) {
        if (!HOT_ROD_START_CONTAINER) return;
        if (infinispanContainer != null) infinispanContainer.stop();
    }

    public InfinispanContainer getContainer() {
        return infinispanContainer;
    }

    @Override
    public boolean canProvide(Class<?> type) {
        return type.equals(HotRodContainerProvider.class);
    }

    @Override
    public Object lookup(ArquillianResource ar, Annotation... antns) {
        return this;
    }
}
