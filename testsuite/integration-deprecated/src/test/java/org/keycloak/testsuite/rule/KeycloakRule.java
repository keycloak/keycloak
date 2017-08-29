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

import org.junit.Assert;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
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
        session.getTransactionManager().begin();

        try {
            RealmManager manager = new RealmManager(session);
            manager.setContextPath("/auth");

            RealmModel adminstrationRealm = manager.getRealm(Config.getAdminRealm());
            RealmModel appRealm = manager.getRealm("test");

            configurer.session = session;
            configurer.config(manager, adminstrationRealm, appRealm);

            session.getTransactionManager().commit();
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

    public abstract static class KeycloakSetup {

        protected KeycloakSession session;

        public abstract void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm);

    }

}
