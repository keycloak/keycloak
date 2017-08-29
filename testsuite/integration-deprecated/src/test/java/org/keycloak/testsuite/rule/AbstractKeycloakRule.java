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

package org.keycloak.testsuite.rule;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ErrorPage;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.SecurityInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.WebResourceCollection;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.keycloak.Config;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.servlet.KeycloakOIDCFilter;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.testsuite.Retry;
import org.keycloak.util.JsonSerialization;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.ws.rs.core.Application;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractKeycloakRule extends ExternalResource {

    protected TemporaryFolder temporaryFolder;

    protected KeycloakServer server;

    protected void before() throws Throwable {
        temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();
        System.setProperty("keycloak.tmp.dir", temporaryFolder.newFolder().getAbsolutePath());

        server = new KeycloakServer();

        configureServer(server);

        server.start();

        removeTestRealms();

        setupKeycloak();
    }

    protected void configureServer(KeycloakServer server) {

    }

    public UserRepresentation getUser(String realm, String name) {
        KeycloakSession session = server.getSessionFactory().create();
        session.getTransactionManager().begin();
        try {
            RealmModel realmByName = session.realms().getRealmByName(realm);
            UserModel user = session.users().getUserByUsername(name, realmByName);
            UserRepresentation userRep = user != null ? ModelToRepresentation.toRepresentation(session, realmByName, user) : null;
            session.getTransactionManager().commit();
            return userRep;
        } finally {
            session.close();
        }
    }

    public UserRepresentation getUserById(String realm, String id) {
        KeycloakSession session = server.getSessionFactory().create();
        session.getTransactionManager().begin();
        try {
            RealmModel realmByName = session.realms().getRealmByName(realm);
            UserRepresentation userRep = ModelToRepresentation.toRepresentation(session, realmByName, session.users().getUserById(id, realmByName));
            session.getTransactionManager().commit();
            return userRep;
        } finally {
            session.close();
        }
    }

    protected void setupKeycloak() {
        KeycloakSession session = server.getSessionFactory().create();
        session.getTransactionManager().begin();

        try {
            RealmManager manager = new RealmManager(session);

            RealmModel adminstrationRealm = manager.getRealm(Config.getAdminRealm());

            configure(session, manager, adminstrationRealm);

            session.getTransactionManager().commit();
        } finally {
            session.close();
        }
    }

    public void update(KeycloakRule.KeycloakSetup configurer, String realmId) {
        KeycloakSession session = server.getSessionFactory().create();
        session.getTransactionManager().begin();

        try {
            RealmManager manager = new RealmManager(session);
            manager.setContextPath("/auth");

            RealmModel adminstrationRealm = manager.getRealm(Config.getAdminRealm());
            RealmModel appRealm = manager.getRealm(realmId);

            configurer.session = session;
            configurer.config(manager, adminstrationRealm, appRealm);

            session.getTransactionManager().commit();
        } finally {
            session.close();
        }
    }

    protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {

    }

    public void deployServlet(String name, String contextPath, Class<? extends Servlet> servletClass) {
        DeploymentInfo deploymentInfo = createDeploymentInfo(name, contextPath, servletClass);
        server.getServer().deploy(deploymentInfo);
    }


    public DeploymentInfo createDeploymentInfo(String name, String contextPath, Class<? extends Servlet> servletClass) {
        DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setClassLoader(getClass().getClassLoader());
        deploymentInfo.setDeploymentName(name);
        deploymentInfo.setContextPath(contextPath);

        ServletInfo servlet = new ServletInfo(servletClass.getSimpleName(), servletClass);
        servlet.addMapping("/*");

        deploymentInfo.addServlet(servlet);
        return deploymentInfo;
    }


    public DeploymentBuilder createApplicationDeployment() {
        return new DeploymentBuilder();
    }

    public void addErrorPage(String errorPage, DeploymentInfo di) {
        ServletInfo servlet = new ServletInfo("Error Page", ErrorServlet.class);
        servlet.addMapping("/error.html");
        SecurityConstraint constraint = new SecurityConstraint();
        WebResourceCollection collection = new WebResourceCollection();
        collection.addUrlPattern("/error.html");
        constraint.addWebResourceCollection(collection);
        constraint.setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.PERMIT);
        di.addSecurityConstraint(constraint);
        di.addServlet(servlet);
        di
                .addErrorPage(new ErrorPage(errorPage, 400))
                .addErrorPage(new ErrorPage(errorPage, 401))
                .addErrorPage(new ErrorPage(errorPage, 403))
                .addErrorPage(new ErrorPage(errorPage, 500));
    }

    public void deployJaxrsApplication(String name, String contextPath, Class<? extends Application> applicationClass, Map<String,String> initParams) {
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplicationClass(applicationClass.getName());

        DeploymentInfo di = server.getServer().undertowDeployment(deployment, "");
        di.setClassLoader(getClass().getClassLoader());
        di.setContextPath(contextPath);
        di.setDeploymentName(name);

        for (Map.Entry<String,String> param : initParams.entrySet()) {
            di.addInitParameter(param.getKey(), param.getValue());
        }

        server.getServer().deploy(di);
    }

    @Override
    protected void after() {
        removeTestRealms();
        stopServer();
        Time.setOffset(0);

        temporaryFolder.delete();
        System.getProperties().remove("keycloak.tmp.dir");
    }

    protected void removeTestRealms() {
        KeycloakSession session = server.getSessionFactory().create();
        try {
            session.getTransactionManager().begin();
            RealmManager realmManager = new RealmManager(session);
            for (String realmName : getTestRealms()) {
                RealmModel realm = realmManager.getRealmByName(realmName);
                if (realm != null) {
                    realmManager.removeRealm(realm);
                }
            }
            session.getTransactionManager().commit();
        } finally {
            session.close();
        }
    }

    public RealmRepresentation loadJson(String path) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) {
            os.write(c);
        }
        byte[] bytes = os.toByteArray();
        return JsonSerialization.readValue(bytes, RealmRepresentation.class);
    }

    public KeycloakSession startSession() {
        KeycloakSession session = server.getSessionFactory().create();
        session.getTransactionManager().begin();
        return session;
    }

    public void stopSession(KeycloakSession session, boolean commit) {
        KeycloakTransaction transaction = session.getTransactionManager();
        if (commit && !transaction.getRollbackOnly()) {
            transaction.commit();
        } else {
            transaction.rollback();
        }
        session.close();
    }

    public void restartServer() {
        try {
            stopServer();
            server.start();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void stopServer() {
        server.stop();

        // Add some variable delay (Some windows envs have issues as server is not stopped immediately after server.stop)
        try {
            Retry.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        Socket s = new Socket(server.getConfig().getHost(), server.getConfig().getPort());
                        s.close();
                        throw new IllegalStateException("Server still running");
                    } catch (IOException expected) {
                    }
                }

            }, 10, 500);
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    protected String[] getTestRealms() {
        return new String[]{"test", "demo"};
    }

    public class DeploymentBuilder {

        private String name;
        private String contextPath;
        private Class<? extends Servlet> servletClass;
        private String adapterConfigPath;
        private String role;
        private boolean isConstrained = true;
        private Class<? extends KeycloakConfigResolver> keycloakConfigResolver;
        private String constraintUrl = "/*";
        private String errorPage = "/error.html";

        public DeploymentBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DeploymentBuilder contextPath(String contextPath) {
            this.contextPath = contextPath;
            return this;
        }

        public DeploymentBuilder servletClass(Class<? extends Servlet> servletClass) {
            this.servletClass = servletClass;
            return this;
        }

        public DeploymentBuilder adapterConfigPath(String adapterConfigPath) {
            this.adapterConfigPath = adapterConfigPath;
            return this;
        }

        public DeploymentBuilder role(String role) {
            this.role = role;
            return this;
        }

        public DeploymentBuilder isConstrained(boolean isConstrained) {
            this.isConstrained = isConstrained;
            return this;
        }

        public DeploymentBuilder keycloakConfigResolver(Class<? extends KeycloakConfigResolver> keycloakConfigResolver) {
            this.keycloakConfigResolver = keycloakConfigResolver;
            return this;
        }

        public DeploymentBuilder constraintUrl(String constraintUrl) {
            this.constraintUrl = constraintUrl;
            return this;
        }

        public DeploymentBuilder errorPage(String errorPage) {
            this.errorPage = errorPage;
            return this;
        }

        public void deployApplication() {
            DeploymentInfo di = createDeploymentInfo(name, contextPath, servletClass);
            if (null == keycloakConfigResolver) {
                di.addInitParameter("keycloak.config.file", adapterConfigPath);
            } else {
                di.addInitParameter("keycloak.config.resolver", keycloakConfigResolver.getCanonicalName());
            }
            if (isConstrained) {
                SecurityConstraint constraint = new SecurityConstraint();
                WebResourceCollection collection = new WebResourceCollection();
                collection.addUrlPattern(constraintUrl);
                constraint.addWebResourceCollection(collection);
                constraint.addRoleAllowed(role);
                di.addSecurityConstraint(constraint);
            }
            LoginConfig loginConfig = new LoginConfig("KEYCLOAK", "demo", null, null);
            di.setLoginConfig(loginConfig);
            addErrorPage(errorPage, di);

            server.getServer().deploy(di);
        }

        public void deployApplicationWithFilter() {
            DeploymentInfo di = createDeploymentInfo(name, contextPath, servletClass);
            FilterInfo filter = new FilterInfo("keycloak-filter", KeycloakOIDCFilter.class);
            if (null == keycloakConfigResolver) {
                filter.addInitParam("keycloak.config.file", adapterConfigPath);
            } else {
                filter.addInitParam("keycloak.config.resolver", keycloakConfigResolver.getCanonicalName());
            }
            di.addFilter(filter);
            di.addFilterUrlMapping("keycloak-filter", constraintUrl, DispatcherType.REQUEST);
            server.getServer().deploy(di);



        }

    }

}
