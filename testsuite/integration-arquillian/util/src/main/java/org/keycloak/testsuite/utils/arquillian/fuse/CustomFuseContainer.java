package org.keycloak.testsuite.utils.arquillian.fuse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.arquillian.container.osgi.jmx.JMXDeployableContainer;
import org.jboss.arquillian.container.osgi.jmx.ObjectNameFactory;
import org.jboss.arquillian.container.osgi.karaf.managed.KarafManagedContainerConfiguration;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.logging.Logger;
import org.osgi.jmx.framework.BundleStateMBean;
import org.osgi.jmx.framework.FrameworkMBean;
import org.osgi.jmx.framework.ServiceStateMBean;

/**
 * KarafManagedDeployableContainer
 * 
 * @author thomas.diesler@jboss.com
 */
public class CustomFuseContainer<T extends KarafManagedContainerConfiguration> extends JMXDeployableContainer<T> {

    private static final Logger log = Logger.getLogger(CustomFuseContainer.class);

    private KarafManagedContainerConfiguration config;
    private Process process;

    @Override
    public Class<T> getConfigurationClass() {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) KarafManagedContainerConfiguration.class;
        return clazz;
    }

    @Override
    public void setup(T config) {
        super.setup(config);
        this.config = config;
    }

    @Override
    public void start() throws LifecycleException {

        // Try to connect to an already running server
        MBeanServerConnection mbeanServer = null;
        try {
            mbeanServer = getMBeanServerConnection(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ignore) {
        }

        if (mbeanServer != null && !config.isAllowConnectingToRunningServer()) {
            throw new LifecycleException(
                    "The server is already running! Managed containers does not support connecting to running server instances due to the " +
                    "possible harmful effect of connecting to the wrong server. Please stop server before running or change to another type of container.\n" +
                    "To disable this check and allow Arquillian to connect to a running server, set allowConnectingToRunningServer to true in the container configuration");
        }

        // Start the Karaf process
        if (mbeanServer == null) {
            String karafHome = config.getKarafHome();
            if (karafHome == null)
                throw new IllegalStateException("karafHome cannot be null");

            File karafHomeDir = new File(karafHome).getAbsoluteFile();
            if (!karafHomeDir.isDirectory())
                throw new IllegalStateException("Not a valid Karaf home dir: " + karafHomeDir);

            String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            log.infof("Using java: %s", java);

            List<String> cmd = new ArrayList<>();
            cmd.add(java);

            // JavaVM args
            String javaArgs = config.getJavaVmArguments();
            if (!javaArgs.contains("-Xmx")) {
                javaArgs = KarafManagedContainerConfiguration.DEFAULT_JAVAVM_ARGUMENTS + " " + javaArgs;
            }
            cmd.addAll(Arrays.asList(javaArgs.split("\\s")));

            // Karaf properties
            cmd.add("-Dkaraf.home=" + karafHomeDir);
            cmd.add("-Dkaraf.base=" + karafHomeDir);
            cmd.add("-Dkaraf.etc=" + karafHomeDir + "/etc");
            cmd.add("-Dkaraf.data=" + karafHomeDir + "/data");
            cmd.add("-Dkaraf.instances=" + karafHomeDir + "/instances");
            cmd.add("-Dkaraf.restart.jvm.supported=true");
            cmd.add("-Dkaraf.startLocalConsole=false");
            cmd.add("-Dkaraf.startRemoteShell=true");

            // Java properties
            cmd.add("-Djava.io.tmpdir=" + new File(karafHomeDir, "data/tmp"));
            cmd.add("-Djava.util.logging.config.file=" + new File(karafHomeDir, "etc/java.util.logging.properties"));
            cmd.add("-Djava.endorsed.dirs=" + new File(karafHomeDir, "lib/endorsed"));

            // Classpath
            StringBuilder classPath = new StringBuilder();
            boolean fuse7 = new File(karafHomeDir, "lib/boot/").exists();
            if (fuse7) {
                log.info("Adding karaf4 libraries to classpath.");
                String[] libDirs  = { "lib/boot/", "lib/ext/" };
                for (String libDir : libDirs) {
                    File karafLibBootDir = new File(karafHomeDir, libDir);
                    String[] libs = karafLibBootDir.list((File dir, String name) -> name.endsWith(".jar"));
                    for (String lib : libs) {
                        String separator = classPath.length() > 0 ? File.pathSeparator : "";
                        classPath.append(separator).append(new File(karafLibBootDir, lib));
                    }
                }

            } else { //fuse6
                log.info("Adding karaf3 libraries to classpath.");
                File karafLibDir = new File(karafHomeDir, "lib");
                String[] libs = karafLibDir.list((File dir, String name) -> name.startsWith("karaf"));
                for (String lib : libs) {
                    String separator = classPath.length() > 0 ? File.pathSeparator : "";
                    classPath.append(separator).append(new File(karafHomeDir, "lib/" + lib));
                }
            }
            cmd.add("-classpath");
            cmd.add(classPath.toString());

            // Main class
            cmd.add("org.apache.karaf.main.Main");

            // Output the startup command
            StringBuffer cmdstr = new StringBuffer();
            for (String tok : cmd) {
                cmdstr.append(tok).append(" ");
            }
            log.debugv("Starting Karaf with: {0}", cmdstr);

            try {
                ProcessBuilder processBuilder = new ProcessBuilder(cmd);
                processBuilder.directory(karafHomeDir);
                processBuilder.redirectErrorStream(true);
                process = processBuilder.start();
                new Thread(new ConsoleConsumer()).start();
            } catch (IOException ex) {
                throw new LifecycleException("Cannot start managed Karaf container", ex);
            }

            // Get the MBeanServerConnection
            try {
                log.debug("Geting the MBeanServerConnection");
                mbeanServer = getMBeanServerConnection(120, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                destroyKarafProcess();
                throw new LifecycleException("Cannot obtain MBean server connection", ex);
            }
        }

        mbeanServerInstance.set(mbeanServer);

        try {
            // Get the FrameworkMBean
            ObjectName oname = ObjectNameFactory.create("osgi.core:type=framework,*");
            frameworkMBean = getMBeanProxy(mbeanServer, oname, FrameworkMBean.class, 30, TimeUnit.SECONDS);

            // Get the BundleStateMBean
            oname = ObjectNameFactory.create("osgi.core:type=bundleState,*");
            bundleStateMBean = getMBeanProxy(mbeanServer, oname, BundleStateMBean.class, 30, TimeUnit.SECONDS);

            // Get the BundleStateMBean
            oname = ObjectNameFactory.create("osgi.core:type=serviceState,*");
            serviceStateMBean = getMBeanProxy(mbeanServer, oname, ServiceStateMBean.class, 30, TimeUnit.SECONDS);

            // Install the arquillian bundle to become active
            installArquillianBundle();

            // Await the arquillian bundle to become active
            awaitArquillianBundleActive(30, TimeUnit.SECONDS);

            // Await the beginning start level
            Integer beginningStartLevel = config.getKarafBeginningStartLevel();
            if (beginningStartLevel != null)
                awaitBeginningStartLevel(beginningStartLevel, 30, TimeUnit.SECONDS);

            // Await bootsrap complete services
            awaitBootstrapCompleteServices();

        } catch (RuntimeException rte) {
            destroyKarafProcess();
            throw rte;
        } catch (IOException | InterruptedException | TimeoutException | LifecycleException ex) {
            destroyKarafProcess();
            throw new LifecycleException("Cannot start Karaf container", ex);
        }
    }

    @Override
    public void stop() throws LifecycleException {
        try {
            super.stop();
        } catch (LifecycleException ex) {
            log.info("Couldn't uninstall arquillian bundle. This should be non-blocking issue, proceeding with destroying karaf process.");
        }

        destroyKarafProcess();
    }

    private void destroyKarafProcess() throws LifecycleException {
        if (process != null) {
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                throw new LifecycleException("Cannot start Karaf container", e);
            }
        }
    }

    /**
     * Runnable that consumes the output of the process. If nothing consumes the output the AS will hang on some platforms
     *
     * @author Stuart Douglas
     */
    private class ConsoleConsumer implements Runnable {

        @Override
        public void run() {
            final InputStream stream = process.getInputStream();
            final boolean writeOutput = config.isOutputToConsole();
            try {
                byte[] buf = new byte[32];
                int num;
                // Do not try reading a line cos it considers '\r' end of line
                while ((num = stream.read(buf)) != -1) {
                    if (writeOutput)
                        System.out.write(buf, 0, num);
                }
            } catch (IOException ignored) {
            }
        }
    }


}
