/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.quarkus.deployment;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.jboss.logging.Logger;
import org.keycloak.util.Environment;

public class BuildClassLoader extends URLClassLoader {

    private static final Logger logger = Logger.getLogger(BuildClassLoader.class);

    public BuildClassLoader() {
        super(new URL[] {}, Thread.currentThread().getContextClassLoader());
        String homeDir = Environment.getHomeDir();

        if (homeDir == null) {
            return;
        }

        File providersDir = new File(homeDir + File.separator + "providers");

        if (providersDir.isDirectory()) {
            for (File file : providersDir.listFiles(new JarFilter())) {
                try {
                    addURL(file.toURI().toURL());
                    logger.debug("Loading providers from " + file.getAbsolutePath());
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Failed to add provider JAR at " + file.getAbsolutePath());
                }
            }
        }
    }

    class JarFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".jar");
        }

    }
}
