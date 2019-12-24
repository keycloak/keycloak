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
package org.keycloak.testsuite;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DefaultServletConfig;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.keycloak.common.Version;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.filters.KeycloakSessionServletFilter;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.testsuite.util.cli.TestsuiteCLI;
import org.keycloak.util.JsonSerialization;
import org.xnio.Options;
import org.xnio.SslClientAuthMode;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.DispatcherType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakServer {

    private static final Logger log = Logger.getLogger(KeycloakServer.class);
    public static final String JBOSS_SERVER_DATA_DIR = "jboss.server.data.dir";

    private boolean sysout = false;

    public static class KeycloakServerConfig {
        private String host = "localhost";
        private int port = 8081;
        private int portHttps = -1;
        private int workerThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2) * 8;
        private String resourcesHome;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public int getPortHttps() {
            return portHttps;
        }

        public String getResourcesHome() {
            return resourcesHome;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public void setPortHttps(int portHttps) {
            this.portHttps = portHttps;
        }

        public void setResourcesHome(String resourcesHome) {
            this.resourcesHome = resourcesHome;
        }

        public int getWorkerThreads() {
            return workerThreads;
        }

        public void setWorkerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
        }
    }

    public static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }

    public static void main(String[] args) throws Throwable {
        if (!System.getenv().containsKey("MAVEN_CMD_LINE_ARGS")) {
            Version.BUILD_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        }

        bootstrapKeycloakServer(args);
    }

    public static KeycloakServer bootstrapKeycloakServer(String[] args) throws Throwable {
        File f = new File(System.getProperty("user.home"), ".keycloak-server.properties");
        if (f.isFile()) {
            Properties p = new Properties();
            p.load(new FileInputStream(f));
            System.getProperties().putAll(p);
        }

        KeycloakServerConfig config = new KeycloakServerConfig();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-b")) {
                config.setHost(args[++i]);
            }

            if (args[i].equals("-p")) {
                config.setPort(Integer.valueOf(args[++i]));
            }
        }

        if (System.getProperty("keycloak.port") != null) {
            config.setPort(Integer.valueOf(System.getProperty("keycloak.port")));
        }

        if (System.getProperty("keycloak.port.https") != null) {
            config.setPortHttps(Integer.valueOf(System.getProperty("keycloak.port.https")));
        }

        if (System.getProperty("keycloak.bind.address") != null) {
            config.setHost(System.getProperty("keycloak.bind.address"));
        }

        if (System.getenv("KEYCLOAK_DEV_PORT") != null) {
            config.setPort(Integer.valueOf(System.getenv("KEYCLOAK_DEV_PORT")));
        }

        if (System.getProperties().containsKey("resources")) {
            String resources = System.getProperty("resources");
            if (resources == null || resources.equals("") || resources.equals("true")) {
                if (System.getProperties().containsKey("maven.home")) {
                    resources = System.getProperty("user.dir").replaceFirst("testsuite.utils.*", "");
                } else {
                    for (String c : System.getProperty("java.class.path").split(File.pathSeparator)) {
                        if (c.contains(File.separator + "testsuite" + File.separator + "utils")) {
                            resources = c.replaceFirst("testsuite.utils.*", "");
                        }
                    }
                }
            }

            File dir = new File(resources).getAbsoluteFile();
            if (!dir.isDirectory()) {
                throw new RuntimeException("Invalid base resources directory");

            }
            if (!new File(dir, "themes").isDirectory()) {
                throw new RuntimeException("Invalid resources forms directory");
            }

            if (!System.getProperties().containsKey("keycloak.theme.dir")) {
                System.setProperty("keycloak.theme.dir", file(dir.getAbsolutePath(), "themes", "src", "main", "resources", "theme").getAbsolutePath());
            } else {
                String foo = System.getProperty("keycloak.theme.dir");
                System.out.println(foo);
            }

            if (!System.getProperties().containsKey("keycloak.theme.cacheTemplates")) {
                System.setProperty("keycloak.theme.cacheTemplates", "false");
            }

            if (!System.getProperties().containsKey("keycloak.theme.cacheThemes")) {
                System.setProperty("keycloak.theme.cacheThemes", "false");
            }

            if (!System.getProperties().containsKey("keycloak.theme.staticMaxAge")) {
                System.setProperty("keycloak.theme.staticMaxAge", "-1");
            }

            config.setResourcesHome(dir.getAbsolutePath());
        }

        if (System.getProperties().containsKey("undertowWorkerThreads")) {
            int undertowWorkerThreads = Integer.parseInt(System.getProperty("undertowWorkerThreads"));
            config.setWorkerThreads(undertowWorkerThreads);
        }

        configureDataDirectory();

        detectNodeName(config);

        final KeycloakServer keycloak = new KeycloakServer(config);
        keycloak.sysout = true;
        keycloak.start();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-import")) {
                keycloak.importRealm(new FileInputStream(args[++i]));
            }
        }

        if (System.getProperties().containsKey("import")) {
            keycloak.importRealm(new FileInputStream(System.getProperty("import")));
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                keycloak.stop();
            }
        });

        if (System.getProperties().containsKey("startTestsuiteCLI")) {
            new TestsuiteCLI(keycloak).start();
        }

        return keycloak;
    }

    public static void configureDataDirectory() {
        String dataPath = detectDataDirectory();
        System.setProperty(JBOSS_SERVER_DATA_DIR, dataPath);
        log.infof("Using %s %s", JBOSS_SERVER_DATA_DIR,  dataPath);
    }

  /**
   * Detects the {@code jboss.server.data.dir} to use.
   * If the System property {@code jboss.server.data.dir} is already set then the property value is used,
   * otherwise a temporary data dir is created that will be deleted on JVM exit.
   *
   * @return
   */
  public static String detectDataDirectory() {

        String dataPath = System.getProperty(JBOSS_SERVER_DATA_DIR);

        if (dataPath != null){
            // we assume jboss.server.data.dir is managed externally so just use it as is.
            File dataDir = new File(dataPath);
            if (!dataDir.exists() || !dataDir.isDirectory()) {
                throw new RuntimeException("Invalid " + JBOSS_SERVER_DATA_DIR + " resources directory: " + dataPath);
            }

            return dataPath;
        }

        // we generate a dynamic jboss.server.data.dir and remove it at the end.
        try {
          File tempKeycloakFolder = Files.createTempDirectory("keycloak-server-").toFile();
          File tmpDataDir = new File(tempKeycloakFolder, "/data");

          if (tmpDataDir.mkdirs()) {
            tmpDataDir.deleteOnExit();
          } else {
            throw new IOException("Could not create directory " + tmpDataDir);
          }

          dataPath = tmpDataDir.getAbsolutePath();
        } catch (IOException ioe){
          throw new RuntimeException("Could not create temporary " + JBOSS_SERVER_DATA_DIR, ioe);
        }

        return dataPath;
    }

    private KeycloakServerConfig config;

    private KeycloakSessionFactory sessionFactory;

    private UndertowJaxrsServer server;

    public KeycloakServer() {
        this(new KeycloakServerConfig());
    }

    public KeycloakServer(KeycloakServerConfig config) {
        this.config = config;
    }

    public KeycloakSessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public UndertowJaxrsServer getServer() {
        return server;
    }

    public KeycloakServerConfig getConfig() {
        return config;
    }

    public void importRealm(InputStream realm) {
        RealmRepresentation rep = loadJson(realm, RealmRepresentation.class);
        importRealm(rep);
    }

    public void importRealm(RealmRepresentation rep) {
        KeycloakSession session = sessionFactory.create();;
        session.getTransactionManager().begin();

        try {
            RealmManager manager = new RealmManager(session);

            if (rep.getId() != null && manager.getRealm(rep.getId()) != null) {
                info("Not importing realm " + rep.getRealm() + " realm already exists");
                return;
            }

            if (manager.getRealmByName(rep.getRealm()) != null) {
                info("Not importing realm " + rep.getRealm() + " realm already exists");
                return;
            }
            RealmModel realm = manager.importRealm(rep);

            info("Imported realm " + realm.getName());

            session.getTransactionManager().commit();
        } finally {
            session.close();
        }
    }

    protected void setupDevConfig() {
        if (System.getProperty("keycloak.createAdminUser", "true").equals("true")) {
            KeycloakSession session = sessionFactory.create();
            try {
                session.getTransactionManager().begin();
                if (new ApplianceBootstrap(session).isNoMasterUser()) {
                    new ApplianceBootstrap(session).createMasterRealmUser("admin", "admin");
                    log.info("Created master user with credentials admin:admin");
                }
                session.getTransactionManager().commit();
            } finally {
                session.close();
            }
        }
    }

    public void start() throws Throwable {
        long start = System.currentTimeMillis();

        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplicationClass(KeycloakApplication.class.getName());

        Builder builder = Undertow.builder()
                .addHttpListener(config.getPort(), config.getHost())
                .setWorkerThreads(config.getWorkerThreads())
                .setIoThreads(config.getWorkerThreads() / 8);

        if (config.getPortHttps() != -1) {
            builder = builder
                    .addHttpsListener(config.getPortHttps(), config.getHost(), createSSLContext())
                    .setSocketOption(Options.SSL_CLIENT_AUTH_MODE, SslClientAuthMode.REQUESTED);
        }

        server = new UndertowJaxrsServer();
        try {
            server.start(builder);

            DeploymentInfo di = server.undertowDeployment(deployment, "");
            di.setClassLoader(getClass().getClassLoader());
            di.setContextPath("/auth");
            di.setDeploymentName("Keycloak");
            di.setDefaultEncoding("UTF-8");

            di.setDefaultServletConfig(new DefaultServletConfig(true));

            ServletInfo restEasyDispatcher = Servlets.servlet("Keycloak REST Interface", HttpServlet30Dispatcher.class);

            restEasyDispatcher.addInitParam("resteasy.servlet.mapping.prefix", "/");
            restEasyDispatcher.setAsyncSupported(true);

            di.addServlet(restEasyDispatcher);

            FilterInfo filter = Servlets.filter("SessionFilter", TestKeycloakSessionServletFilter.class);

            filter.setAsyncSupported(true);

            di.addFilter(filter);
            di.addFilterUrlMapping("SessionFilter", "/*", DispatcherType.REQUEST);

            server.deploy(di);

            sessionFactory = ((KeycloakApplication) deployment.getApplication()).getSessionFactory();

            setupDevConfig();

            if (config.getResourcesHome() != null) {
                info("Loading resources from " + config.getResourcesHome());
            }

            info("Started Keycloak (http://" + config.getHost() + ":" + config.getPort() + "/auth"
                    + (config.getPortHttps() > 0 ? ", https://" + config.getHost() + ":" + config.getPortHttps()+ "/auth" : "")
                    + ") in "
                    + (System.currentTimeMillis() - start) + " ms\n");
        } catch (RuntimeException e) {
            server.stop();
            throw e;
        }
    }

    private void info(String message) {
        if (sysout) {
            System.out.println(message);
        } else {
            log.info(message);
        }
    }

    public void stop() {
        sessionFactory.close();
        server.stop();

        info("Stopped Keycloak");
    }

    private static File file(String... path) {
        StringBuilder s = new StringBuilder();
        for (String p : path) {
            s.append(File.separator);
            s.append(p);
        }
        return new File(s.toString());
    }


    private static void detectNodeName(KeycloakServerConfig config) {
        String nodeName = System.getProperty(InfinispanConnectionProvider.JBOSS_NODE_NAME);
        if (nodeName == null) {
            // Try to autodetect "jboss.node.name" from the port
            Map<Integer, String> nodesCfg = new HashMap<>();
            nodesCfg.put(8181, "node1");
            nodesCfg.put(8182, "node2");

            nodeName = nodesCfg.get(config.getPort());
            if (nodeName != null) {
                System.setProperty(InfinispanConnectionProvider.JBOSS_NODE_NAME, nodeName);
            }
        }

        if (nodeName != null) {
            log.infof("Node name: %s", nodeName);
        }
    }

    private SSLContext createSSLContext() throws Exception {
        KeyManager[] keyManagers = getKeyManagers();

        if (keyManagers == null) {
            return SSLContext.getDefault();
        }

        TrustManager[] trustManagers = getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);
        return sslContext;
    }


    private KeyManager[] getKeyManagers() throws Exception {
        String keyStorePath = System.getProperty("keycloak.tls.keystore.path");

        if (keyStorePath == null) {
            return null;
        }

        log.infof("Loading keystore from file: %s", keyStorePath);

        InputStream stream = Files.newInputStream(Paths.get(keyStorePath));

        if (stream == null) {
            throw new RuntimeException("Could not load keystore");
        }

        try (InputStream is = stream) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            char[] keyStorePassword = System.getProperty("keycloak.tls.keystore.password", "password").toCharArray();
            keyStore.load(is, keyStorePassword);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword);

            return keyManagerFactory.getKeyManagers();
        }
    }


    private TrustManager[] getTrustManagers() throws Exception {
        String trustStorePath = System.getProperty("keycloak.tls.truststore.path");

        if (trustStorePath == null) {
            return null;
        }

        log.infof("Loading truststore from file: %s", trustStorePath);

        InputStream stream = Files.newInputStream(Paths.get(trustStorePath));

        if (stream == null) {
            throw new RuntimeException("Could not load truststore");
        }

        try (InputStream is = stream) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            char[] keyStorePassword = System.getProperty("keycloak.tls.truststore.password", "password").toCharArray();
            keyStore.load(is, keyStorePassword);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            return trustManagerFactory.getTrustManagers();
        }
    }
}
