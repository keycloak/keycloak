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
package org.keycloak.testsuite.rule;

import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;

import org.jboss.resteasy.jwt.JsonSerialization;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.rules.ExternalResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.filters.KeycloakSessionServletFilter;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakSessionFactory;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.SaasService;
import org.keycloak.testsuite.ApplicationServlet;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakRule extends ExternalResource {

    private String testRealm = "testrealm.json";

    private UndertowJaxrsServer server;
    private KeycloakSessionFactory factory;

    private KeycloakSetup setup;

    public KeycloakRule() {
    }

    public KeycloakRule(String testRealm) {
        this.testRealm = testRealm;
    }
    
    public KeycloakRule(KeycloakSetup setup) {
        this.setup = setup;
    }

    protected void before() throws Throwable {
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplicationClass(KeycloakApplication.class.getName());
        server = new UndertowJaxrsServer().start();

        DeploymentInfo di = server.undertowDeployment(deployment, "rest");
        di.setClassLoader(getClass().getClassLoader());
        di.setContextPath("/auth-server");
        di.setDeploymentName("Keycloak");
        di.setResourceManager(new ClassPathResourceManager(getClass().getClassLoader(), "META-INF/resources"));

        FilterInfo filter = Servlets.filter("SessionFilter", KeycloakSessionServletFilter.class);
        di.addFilter(filter);
        di.addFilterUrlMapping("SessionFilter", "/rest/*", DispatcherType.REQUEST);

        server.deploy(di);

        factory = KeycloakApplication.buildSessionFactory();

        setupDefaultRealm();

        importRealm(testRealm);

        if (setup != null) {
            configure(setup);
        }

        deployServlet("app", "/app", ApplicationServlet.class);
    }

    public void deployServlet(String name, String contextPath, Class<? extends Servlet> servletClass) {
        DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setClassLoader(getClass().getClassLoader());
        deploymentInfo.setDeploymentName(name);
        deploymentInfo.setContextPath(contextPath);

        ServletInfo servlet = new ServletInfo(servletClass.getSimpleName(), servletClass);
        servlet.addMapping("/*");

        deploymentInfo.addServlet(servlet);
        server.deploy(deploymentInfo);
    }

    @Override
    protected void after() {
        factory.close();
        server.stop();
    }

    public RealmRepresentation loadJson(String path) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) {
            os.write(c);
        }
        byte[] bytes = os.toByteArray();
        System.out.println(new String(bytes));

        return JsonSerialization.fromBytes(RealmRepresentation.class, bytes);
    }

    public void setupDefaultRealm() {
        KeycloakSession session = createSession();
        session.getTransaction().begin();

        RealmManager manager = new RealmManager(session);

        RealmModel defaultRealm = manager.createRealm(RealmModel.DEFAULT_REALM, RealmModel.DEFAULT_REALM);
        defaultRealm.setName(RealmModel.DEFAULT_REALM);
        defaultRealm.setEnabled(true);
        defaultRealm.setTokenLifespan(300);
        defaultRealm.setAccessCodeLifespan(60);
        defaultRealm.setAccessCodeLifespanUserAction(600);
        defaultRealm.setSslNotRequired(false);
        defaultRealm.setCookieLoginAllowed(true);
        defaultRealm.setRegistrationAllowed(true);
        defaultRealm.setAutomaticRegistrationAfterSocialLogin(false);
        manager.generateRealmKeys(defaultRealm);
        defaultRealm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        RoleModel role = defaultRealm.addRole(SaasService.REALM_CREATOR_ROLE);
        UserModel admin = defaultRealm.addUser("admin");
        defaultRealm.grantRole(admin, role);

        session.getTransaction().commit();
        session.close();
    }

    public void importRealm(String name) throws IOException {
        KeycloakSession session = createSession();
        session.getTransaction().begin();

        RealmManager manager = new RealmManager(session);

        RealmModel defaultRealm = manager.getRealm(RealmModel.DEFAULT_REALM);
        UserModel admin = defaultRealm.getUser("admin");

        RealmRepresentation rep = loadJson(name);
        RealmModel realm = manager.createRealm("test", rep.getRealm());
        manager.importRealm(rep, realm);
        realm.addRealmAdmin(admin);

        session.getTransaction().commit();
        session.close();
    }

    public void configure(KeycloakSetup configurer) {
        KeycloakSession session = createSession();
        session.getTransaction().begin();

        RealmManager manager = new RealmManager(session);

        RealmModel defaultRealm = manager.getRealm(RealmModel.DEFAULT_REALM);
        RealmModel appRealm = manager.getRealm("test");

        configurer.config(manager, defaultRealm, appRealm);

        session.getTransaction().commit();
        session.close();
    }

    public KeycloakSession createSession() {
        return factory.createSession();
    }
    
    public interface KeycloakSetup {
        
        void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm);
        
    }

}
