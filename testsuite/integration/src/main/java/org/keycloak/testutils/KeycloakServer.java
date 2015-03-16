/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testutils;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DefaultServletConfig;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.filters.ClientConnectionFilter;
import org.keycloak.services.filters.KeycloakSessionServletFilter;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.util.JsonSerialization;

import javax.servlet.DispatcherType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakServer {

    static {
        try {
            File f = new File(System.getProperty("user.home"), ".keycloak-test.properties");
            if (f.isFile()) {
                Properties p = new Properties();
                p.load(new FileInputStream(f));
                System.getProperties().putAll(p);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Logger log = Logger.getLogger(KeycloakServer.class);

    private boolean sysout = false;

    public static class KeycloakServerConfig {
        private String host = "localhost";
        private int port = 8081;
        private int workerThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2) * 8;
        private String resourcesHome;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
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
        bootstrapKeycloakServer(args);
    }

    public static KeycloakServer bootstrapKeycloakServer(String[] args) throws Throwable {
        KeycloakServerConfig config = new KeycloakServerConfig();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-b")) {
                config.setHost(args[++i]);
            }

            if (args[i].equals("-p")) {
                config.setPort(Integer.valueOf(args[++i]));
            }
        }

        if (System.getenv("KEYCLOAK_DEV_PORT") != null) {
            config.setPort(Integer.valueOf(System.getenv("KEYCLOAK_DEV_PORT")));
        }

        if (System.getProperties().containsKey("resources")) {
            String resources = System.getProperty("resources");
            if (resources == null || resources.equals("") || resources.equals("true")) {
                if (System.getProperties().containsKey("maven.home")) {
                    resources = System.getProperty("user.dir").replaceFirst("testsuite.integration.*", "");
                } else {
                    for (String c : System.getProperty("java.class.path").split(File.pathSeparator)) {
                        if (c.contains(File.separator + "testsuite" + File.separator + "integration")) {
                            resources = c.replaceFirst("testsuite.integration.*", "");
                        }
                    }
                }
            }

            File dir = new File(resources).getAbsoluteFile();
            if (!dir.isDirectory() || !new File(dir, "forms").isDirectory()) {
                throw new RuntimeException("Invalid resources directory");
            }

            if (!System.getProperties().containsKey("keycloak.theme.dir")) {
                System.setProperty("keycloak.theme.dir", file(dir.getAbsolutePath(), "forms", "common-themes", "src", "main", "resources", "theme").getAbsolutePath());
            }

            if (!System.getProperties().containsKey("keycloak.theme.cacheTemplates")) {
                System.setProperty("keycloak.theme.cacheTemplates", "false");
            }

            config.setResourcesHome(dir.getAbsolutePath());
        }

        if (System.getProperties().containsKey("undertowWorkerThreads")) {
            int undertowWorkerThreads = Integer.parseInt(System.getProperty("undertowWorkerThreads"));
            config.setWorkerThreads(undertowWorkerThreads);
        }

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

        return keycloak;
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
        session.getTransaction().begin();

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
            manager.setContextPath("/auth");
            RealmModel realm = manager.importRealm(rep);

            info("Imported realm " + realm.getName());

            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    protected void setupDevConfig() {
        KeycloakSession session = sessionFactory.create();
        session.getTransaction().begin();

        try {
            RealmManager manager = new RealmManager(session);

            RealmModel adminRealm = manager.getKeycloakAdminstrationRealm();
            UserModel admin = session.users().getUserByUsername("admin", adminRealm);
            admin.removeRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

            session.getTransaction().commit();
        } finally {
            session.close();
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

        server = new UndertowJaxrsServer().start(builder);

        DeploymentInfo di = server.undertowDeployment(deployment, "");
        di.setClassLoader(getClass().getClassLoader());
        di.setContextPath("/auth");
        di.setDeploymentName("Keycloak");

        di.setDefaultServletConfig(new DefaultServletConfig(true));
        di.addWelcomePage("theme/welcome/keycloak/resources/index.html");

        FilterInfo filter = Servlets.filter("SessionFilter", KeycloakSessionServletFilter.class);
        di.addFilter(filter);
        di.addFilterUrlMapping("SessionFilter", "/*", DispatcherType.REQUEST);

        FilterInfo connectionFilter = Servlets.filter("ClientConnectionFilter", ClientConnectionFilter.class);
        di.addFilter(connectionFilter);
        di.addFilterUrlMapping("ClientConnectionFilter", "/*", DispatcherType.REQUEST);

        server.deploy(di);

        sessionFactory = ((KeycloakApplication) deployment.getApplication()).getSessionFactory();

        setupDevConfig();

        if (config.getResourcesHome() != null) {
            info("Loading resources from " + config.getResourcesHome());
        }

        info("Started Keycloak (http://" + config.getHost() + ":" + config.getPort() + "/auth) in "
                + (System.currentTimeMillis() - start) + " ms\n");
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

}
