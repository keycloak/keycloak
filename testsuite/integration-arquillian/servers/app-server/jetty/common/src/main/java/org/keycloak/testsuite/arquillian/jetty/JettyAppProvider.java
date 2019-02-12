package org.keycloak.testsuite.arquillian.jetty;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.AppProvider;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.util.FileID;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

/**
 * Most of the bits of this class have been copied from <code>org.jboss.arquillian.container.jetty.embedded_9.ArquillianAppProvider</code>.
 *
 * Unfortunately Arquillian extension for Jetty 8 and 9 uses totally different mechanism, so we need to implement
 * all the bits ourselves.
 *
 * @author Sebastian Łaskawiec
 */
class JettyAppProvider extends AbstractLifeCycle implements AppProvider {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The prefix assigned to the temporary file where the archive is exported
     */
    private static final String EXPORT_FILE_PREFIX = "export";

    /**
     * Directory into which we'll extract export the war files
     */
    private static final File EXPORT_DIR;

    static {
        /*
         * Use of java.io.tmpdir Should be a last-resort fallback for temp directory.
         *
         * Use of java.io.tmpdir on CI systems is dangerous (overwrite possibility is extremely high)
         *
         * Use of java.io.tmpdir on Unix systems is unreliable (due to common /tmp dir cleanup processes)
         */
        File systemDefaultTmpDir = new File(AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("java.io.tmpdir");
            }
        }));

        // If running under maven + surefire, use information provided by surefire.
        String baseDirVal = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("basedir");
            }
        });

        File mavenTmpDir = null;
        if (baseDirVal != null) {
            File baseDir = new File(baseDirVal);
            if (baseDir.exists() && baseDir.isDirectory()) {
                File targetDir = new File(baseDir, "target");
                if (targetDir.exists() && targetDir.isDirectory()) {
                    mavenTmpDir = new File(targetDir, "arquillian-jetty-temp");
                    mavenTmpDir.mkdirs();
                }
            }
        }

        if ((mavenTmpDir != null) && mavenTmpDir.exists() && mavenTmpDir.isDirectory()) {
            EXPORT_DIR = mavenTmpDir;
        } else {
            EXPORT_DIR = systemDefaultTmpDir;
        }

        // If the temp location doesn't exist or isn't a directory
        if (!EXPORT_DIR.exists() || !EXPORT_DIR.isDirectory()) {
            throw new IllegalStateException("Could not obtain export directory \"" + EXPORT_DIR.getAbsolutePath() + "\"");
        }
    }

    private final JettyAppServerConfiguration config;
    private DeploymentManager deploymentManager;

    public JettyAppProvider(JettyAppServerConfiguration config) {
        this.config = config;
    }

    protected KeycloakAdapterApp createApp(final Archive<?> archive) {
        String name = archive.getName();
        int extOff = name.lastIndexOf('.');
        if (extOff <= 0) {
            throw new RuntimeException("Not a valid Web Archive filename: " + name);
        }
        String ext = name.substring(extOff).toLowerCase();
        if (!ext.equals(".war")) {
            throw new RuntimeException("Not a recognized Web Archive: " + name);
        }
        name = name.substring(0, extOff);

        final File exported;
        try {
            // If this method returns successfully then it is guaranteed that:
            // 1. The file denoted by the returned abstract pathname did not exist before this method was invoked, and
            // 2. Neither this method nor any of its variants will return the same abstract pathname again in the current invocation of the virtual machine.
            exported = File.createTempFile(EXPORT_FILE_PREFIX, archive.getName(), EXPORT_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Could not create temporary File in " + EXPORT_DIR + " to write exported archive", e);
        }
        // We are overwriting the temporary file placeholder reserved by File#createTemplateFile()
        archive.as(ZipExporter.class).exportTo(exported, true);

        // Mark to delete when we come down
        // exported.deleteOnExit();

        // Add the context
        URI uri = exported.toURI();
        log.debug("Webapp archive location: " + uri.toASCIIString());

        return new KeycloakAdapterApp(new App(deploymentManager, this, uri.toASCIIString()), archive);
    }

    @Override
    public ContextHandler createContextHandler(final App app) throws Exception {
        Resource resource = Resource.newResource(app.getOriginId());
        File file = resource.getFile();
        if (!resource.exists())
            throw new IllegalStateException("App resouce does not exist " + resource);

        String context = file.getName();

        if (FileID.isWebArchiveFile(file)) {
            // Context Path is the same as the archive.
            context = context.substring(0, context.length() - 4);
        } else {
            throw new IllegalStateException("unable to create ContextHandler for " + app);
        }

        // Ensure "/" is Not Trailing in context paths.
        if (context.endsWith("/") && context.length() > 0) {
            context = context.substring(0, context.length() - 1);
        }

        // Start building the webapplication
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setDisplayName(context);
        webAppContext.setLogUrlOnStart(true);

        // special case of archive (or dir) named "root" is / context
        if (context.equalsIgnoreCase("root")) {
            context = URIUtil.SLASH;
        } else if (context.toLowerCase(Locale.ENGLISH).startsWith("root-")) {
            int dash = context.toLowerCase(Locale.ENGLISH).indexOf('-');
            String virtual = context.substring(dash + 1);
            webAppContext.setVirtualHosts(new String[]{virtual});
            context = URIUtil.SLASH;
        }

        // Ensure "/" is Prepended to all context paths.
        if (context.charAt(0) != '/') {
            context = "/" + context;
        }

        webAppContext.setContextPath(context);
        webAppContext.setWar(file.getAbsolutePath());
        webAppContext.setExtractWAR(true);
        webAppContext.setParentLoaderPriority(true);

        return webAppContext;
    }

    @Override
    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }
}
