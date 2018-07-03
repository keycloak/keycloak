/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.spi.Validate;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.logging.Logger;

import org.keycloak.testsuite.crossdc.DC;

/**
 *
 * @author vramik
 */
public class CacheServerTestEnricher {

    protected static final Logger log = Logger.getLogger(CacheServerTestEnricher.class);
    private static SuiteContext suiteContext;

    @Inject 
    private Instance<ContainerController> containerController;

    static void initializeSuiteContext(SuiteContext suiteContext) {
        Validate.notNull(suiteContext, "Suite context cannot be null.");
        CacheServerTestEnricher.suiteContext = suiteContext;
    }

    public void afterClass(@Observes(precedence = 4) AfterClass event) {
        if (!suiteContext.getCacheServersInfo().isEmpty()) {
            stopCacheServer(suiteContext.getCacheServersInfo().get(DC.FIRST.ordinal()));
            stopCacheServer(suiteContext.getCacheServersInfo().get(DC.SECOND.ordinal()));
        }
    }

    private void stopCacheServer(ContainerInfo cacheServer) {
        if (containerController.get().isStarted(cacheServer.getQualifier())) {
            log.infof("Stopping %s", cacheServer.getQualifier());

            containerController.get().stop(cacheServer.getQualifier());

            // Workaround for possible arquillian bug. Needs to cleanup dir manually
            String setupCleanServerBaseDir = getContainerProperty(cacheServer, "setupCleanServerBaseDir");
            String cleanServerBaseDir = getContainerProperty(cacheServer, "cleanServerBaseDir");

            if (Boolean.parseBoolean(setupCleanServerBaseDir)) {
                log.infof("Going to clean directory: %s", cleanServerBaseDir);

                File dir = new File(cleanServerBaseDir);
                if (dir.exists()) {
                    try {
                        dir.renameTo(new File(dir.getParentFile(), dir.getName() + "--" + System.currentTimeMillis()));

                        File deploymentsDir = new File(dir, "deployments");
                        FileUtils.forceMkdir(deploymentsDir);
                    } catch (IOException ioe) {
                        throw new RuntimeException("Failed to clean directory: " + cleanServerBaseDir, ioe);
                    }
                }
            }

            log.infof("Stopped %s", cacheServer.getQualifier());
        }
    }

    private String getContainerProperty(ContainerInfo cacheServer, String propertyName) {
        return cacheServer.getArquillianContainer().getContainerConfiguration().getContainerProperties().get(propertyName);
    }
}
