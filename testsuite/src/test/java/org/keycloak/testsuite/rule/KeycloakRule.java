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

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.Servlet;

import org.jboss.resteasy.jwt.JsonSerialization;
import org.junit.rules.ExternalResource;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.ApplicationServlet;
import org.keycloak.testutils.KeycloakServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakRule extends ExternalResource {

    private KeycloakServer server;

    private KeycloakSetup setup;

    public KeycloakRule() {
    }

    public KeycloakRule(KeycloakSetup setup) {
        this.setup = setup;
    }

    protected void before() throws Throwable {
        server = new KeycloakServer();
        server.start();

        server.importRealm(getClass().getResourceAsStream("/testrealm.json"));

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
        server.getServer().deploy(deploymentInfo);
    }

    @Override
    protected void after() {
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
        return JsonSerialization.fromBytes(RealmRepresentation.class, bytes);
    }

    public void configure(KeycloakSetup configurer) {
        KeycloakSession session = server.getKeycloakSessionFactory().createSession();
        session.getTransaction().begin();

        try {
            RealmManager manager = new RealmManager(session);

            RealmModel defaultRealm = manager.getRealm(RealmModel.DEFAULT_REALM);
            RealmModel appRealm = manager.getRealm("test");

            configurer.config(manager, defaultRealm, appRealm);

            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    public interface KeycloakSetup {

        void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm);

    }

}
