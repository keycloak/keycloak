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
package org.keycloak.provider;

import org.jboss.logging.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FileSystemProviderLoaderFactory implements ProviderLoaderFactory {

    private static final Logger logger = Logger.getLogger(FileSystemProviderLoaderFactory.class);

    @Override
    public boolean supports(String type) {
        return "classpath".equals(type);
    }

    @Override
    public ProviderLoader create(KeycloakDeploymentInfo info, ClassLoader baseClassLoader, String resource) {
        return new DefaultProviderLoader(info, createClassLoader(baseClassLoader, resource.split(";")));
    }

    private static URLClassLoader createClassLoader(ClassLoader parent, String... files) {
        try {
            List<URL> urls = new LinkedList<URL>();

            for (String f : files) {
                if (f.endsWith("*")) {
                    File dir = new File(f.substring(0, f.length() - 1));
                    if (dir.isDirectory()) {
                        for (File file : dir.listFiles(new JarFilter())) {
                            urls.add(file.toURI().toURL());
                        }
                    }
                } else {
                    urls.add(new File(f).toURI().toURL());
                }
            }

            logger.debug("Loading providers from " + urls.toString());

            return new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class JarFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".jar");
        }

    }

}
