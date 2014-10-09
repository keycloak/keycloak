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

import org.junit.Assert;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.ApplicationServlet;

import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakRule extends AbstractKeycloakRule {

    private KeycloakSetup setup;

    public KeycloakRule() {
    }

    public KeycloakRule(KeycloakSetup setup) {
        this.setup = setup;
    }

    @Override
    protected void setupKeycloak() {
        importRealm();

        if (setup != null) {
            configure(setup);
        }

        deployServlet("app", "/app", ApplicationServlet.class);
    }

    protected void importRealm() {
        server.importRealm(getClass().getResourceAsStream("/testrealm.json"));
    }

    public void configure(KeycloakSetup configurer) {
        KeycloakSession session = server.getSessionFactory().create();
        session.getTransaction().begin();

        try {
            RealmManager manager = new RealmManager(session);

            RealmModel adminstrationRealm = manager.getRealm(Config.getAdminRealm());
            RealmModel appRealm = manager.getRealm("test");

            configurer.session = session;
            configurer.config(manager, adminstrationRealm, appRealm);

            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    public void update(KeycloakSetup configurer) {
        update(configurer, "test");
    }


    public void removeUserSession(String sessionId) {
        KeycloakSession session = startSession();
        RealmModel realm = session.realms().getRealm("test");
        UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
        assertNotNull(userSession);
        session.sessions().removeUserSession(realm, userSession);
        stopSession(session, true);
    }

    public ClientSessionCode verifyCode(String code) {
        KeycloakSession session = startSession();
        try {
            RealmModel realm = session.realms().getRealm("test");
            try {
                ClientSessionCode accessCode = ClientSessionCode.parse(code, session, realm);
                if (accessCode == null) {
                    Assert.fail("Invalid code");
                }
                return accessCode;
            } catch (Throwable t) {
                throw new AssertionError("Failed to parse code", t);
            }
        } finally {
            stopSession(session, false);
        }
    }

    public abstract static class KeycloakSetup {

        protected KeycloakSession session;

        public abstract void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm);

    }

}
