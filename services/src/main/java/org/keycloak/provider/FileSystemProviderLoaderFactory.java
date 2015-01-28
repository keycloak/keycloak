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

    private static final Logger log = Logger.getLogger(FileSystemProviderLoaderFactory.class);

    @Override
    public boolean supports(String type) {
        return "classpath".equals(type);
    }

    @Override
    public ProviderLoader create(ClassLoader baseClassLoader, String resource) {
        return new DefaultProviderLoader(createClassLoader(baseClassLoader, resource.split(";")));
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

            log.debug("Loading providers from " + urls.toString());

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
