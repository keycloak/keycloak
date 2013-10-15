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
import io.undertow.server.handlers.resource.*;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;

import java.io.*;
import java.net.URL;

import javax.servlet.DispatcherType;

import org.jboss.resteasy.jwt.JsonSerialization;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.filters.KeycloakSessionServletFilter;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.SaasService;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakServer {

    private static final Logger log = Logger.getLogger(KeycloakServer.class);

    private boolean sysout = false;

    public static class KeycloakServerConfig {
        private String host = "localhost";
        private int port = 8081;
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
    }

    private static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            return JsonSerialization.fromBytes(type, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }

    public static void main(String[] args) throws Throwable {
        KeycloakServerConfig config = new KeycloakServerConfig();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-b")) {
                config.setHost(args[++i]);
            }

            if (args[i].equals("-p")) {
                config.setPort(Integer.valueOf(args[++i]));
            }
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
            if (!dir.isDirectory() || !new File(dir, "admin-ui").isDirectory()) {
                throw new RuntimeException("Invalid resources directory");
            }

            config.setResourcesHome(dir.getAbsolutePath());
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
    }

    private KeycloakServerConfig config;

    private KeycloakSessionFactory factory;

    private UndertowJaxrsServer server;

    public KeycloakServer() {
        this(new KeycloakServerConfig());
    }

    public KeycloakServer(KeycloakServerConfig config) {
        this.config = config;
    }

    public KeycloakSessionFactory getKeycloakSessionFactory() {
        return factory;
    }

    public UndertowJaxrsServer getServer() {
        return server;
    }

    public void importRealm(InputStream realm) {
        RealmRepresentation rep = loadJson(realm, RealmRepresentation.class);
        importRealm(rep);
    }

    public void importRealm(RealmRepresentation rep) {
        KeycloakSession session = factory.createSession();
        session.getTransaction().begin();

        try {
            RealmManager manager = new RealmManager(session);

            if (rep.getId() == null) {
                throw new RuntimeException("Realm id not specified");
            }

            if (manager.getRealm(rep.getId()) != null) {
                info("Not importing realm " + rep.getRealm() + " realm already exists");
                return;
            }

            RealmModel realm = manager.createRealm(rep.getId(), rep.getRealm());
            manager.importRealm(rep, realm);

            info("Imported realm " + realm.getName());

            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    protected void setupDefaultRealm() {
        KeycloakSession session = factory.createSession();
        session.getTransaction().begin();

        try {
            RealmManager manager = new RealmManager(session);

            if (manager.getRealm(RealmModel.DEFAULT_REALM) != null) {
                return;
            }

            RealmModel defaultRealm = manager.createRealm(RealmModel.DEFAULT_REALM, RealmModel.DEFAULT_REALM);
            defaultRealm.setName(RealmModel.DEFAULT_REALM);
            defaultRealm.setEnabled(true);
            defaultRealm.setTokenLifespan(300);
            defaultRealm.setAccessCodeLifespan(60);
            defaultRealm.setAccessCodeLifespanUserAction(600);
            defaultRealm.setSslNotRequired(true);
            defaultRealm.setCookieLoginAllowed(true);
            defaultRealm.setRegistrationAllowed(true);
            manager.generateRealmKeys(defaultRealm);
            defaultRealm.addRequiredCredential(CredentialRepresentation.PASSWORD);
            defaultRealm.addRole(SaasService.REALM_CREATOR_ROLE);
            defaultRealm.addDefaultRole(SaasService.REALM_CREATOR_ROLE);

            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    public void start() throws Throwable {
        long start = System.currentTimeMillis();

        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplicationClass(KeycloakApplication.class.getName());

        Builder builder = Undertow.builder().addListener(config.getPort(), config.getHost());

        server = new UndertowJaxrsServer().start(builder);

        DeploymentInfo di = server.undertowDeployment(deployment, "rest");
        di.setClassLoader(getClass().getClassLoader());
        di.setContextPath("/auth-server");
        di.setDeploymentName("Keycloak");
        di.setResourceManager(new KeycloakResourceManager(config.getResourcesHome()));

        FilterInfo filter = Servlets.filter("SessionFilter", KeycloakSessionServletFilter.class);
        di.addFilter(filter);
        di.addFilterUrlMapping("SessionFilter", "/rest/*", DispatcherType.REQUEST);

        server.deploy(di);

        factory = KeycloakApplication.buildSessionFactory();

        setupDefaultRealm();

        if (config.getResourcesHome() != null) {
            info("Loading resources from " + config.getResourcesHome());
        }

        info("Started Keycloak (http://" + config.getHost() + ":" + config.getPort() + "/auth-server) in "
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
        factory.close();
        server.stop();

        info("Stopped Keycloak");
    }

    public static class KeycloakResourceManager implements ResourceManager {

        private String resourcesHome;

        public KeycloakResourceManager(String resourcesHome) {
            this.resourcesHome = resourcesHome;
        }

        @Override
        public Resource getResource(String path) throws IOException {
            if (resourcesHome == null) {
                String realPath = "META-INF/resources" + path;

                if (realPath.endsWith("/admin/")) {
                    realPath += "index.html";
                }

                URL url = getClass().getClassLoader().getResource(realPath);

                return new URLResource(url, url.openConnection(), path);
            } else {
                File file;
                if (path.startsWith("/forms")) {
                    file = file(resourcesHome, "forms", "src", "main", "resources", "META-INF", "resources", path.replace('/', File.separatorChar));
                } else if (path.startsWith("/admin")) {
                    file = file(resourcesHome, "admin-ui", "src", "main", "resources", "META-INF", "resources", path.replace('/', File.separatorChar));
                    if (!file.isFile()) {
                        file = file(resourcesHome, "admin-ui-styles", "src", "main", "resources", "META-INF", "resources", path.replace('/', File.separatorChar));
                    }
                } else {
                    throw new IOException("Unknown resource " + path);
                }
                return new FileResource(file, new FileResourceManager(file.getParentFile(), 1), path);
            }
        }
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
