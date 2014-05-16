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

import org.keycloak.models.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.ProviderSession;
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
        KeycloakSession session = server.getKeycloakSessionFactory().createSession();
        ProviderSession providerSession = server.getProviderSessionFactory().createSession();
        session.getTransaction().begin();

        try {
            RealmManager manager = new RealmManager(session);

            RealmModel adminstrationRealm = manager.getRealm(Config.getAdminRealm());
            RealmModel appRealm = manager.getRealm("test");

            configurer.providerSession = providerSession;
            configurer.config(manager, adminstrationRealm, appRealm);

            session.getTransaction().commit();
        } finally {
            providerSession.close();
            session.close();
        }
    }

    public void removeUserSession(String sessionId) {
        KeycloakSession keycloakSession = startSession();
        RealmModel realm = keycloakSession.getRealm("test");
        UserSessionModel session = realm.getUserSession(sessionId);
        assertNotNull(session);
        realm.removeUserSession(session);
        stopSession(keycloakSession, true);
    }

    public abstract static class KeycloakSetup {

        protected ProviderSession providerSession;

        public abstract void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm);

    }

}
