package org.keycloak;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.provider.DefaultProviderLoader;
import org.keycloak.provider.KeycloakDeploymentInfo;
import org.keycloak.provider.ProviderLoader;

class UserProviderLoader {

    private static final Logger logger = Logger.getLogger(UserProviderLoader.class);

    static ProviderLoader create(KeycloakDeploymentInfo info, ClassLoader parentClassLoader) {
        return new DefaultProviderLoader(info, createClassLoader(parentClassLoader));
    }

    private static ClassLoader createClassLoader(ClassLoader parent) {
        String homeDir = System.getProperty("keycloak.home.dir");
        
        if (homeDir == null) {
            // don't load resources from classpath
            return new ClassLoader() {
                @Override
                public Enumeration<URL> getResources(String name) throws IOException {
                    return Collections.emptyEnumeration();
                }
            };
        }

        try {
            List<URL> urls = new LinkedList<URL>();
            File dir = new File(homeDir + File.separator + "providers");

            if (dir.isDirectory()) {
                for (File file : dir.listFiles(new JarFilter())) {
                    urls.add(file.toURI().toURL());
                }
            }

            logger.debug("Loading providers from " + urls.toString());

            return new URLClassLoader(urls.toArray(new URL[urls.size()]), parent) {
                @Override
                public Enumeration<URL> getResources(String name) throws IOException {
                    Enumeration<URL> resources = findResources(name);
                    List<URL> result = new ArrayList<>();
                    
                    while (resources.hasMoreElements()) {
                        URL url = resources.nextElement();
                        
                        if (url.toString().contains(dir.getAbsolutePath())) {
                            result.add(url);
                        }
                    }
                    
                    return Collections.enumeration(result);
                }
            };
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
