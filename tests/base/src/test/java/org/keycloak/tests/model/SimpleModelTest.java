/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.model;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class SimpleModelTest {

    private static final Logger log = Logger.getLogger(SimpleModelTest.class);

    @InjectRealm(attachTo = "master")
    ManagedRealm realm;

    @TestOnServer
    public void simpleModelTestWithNestedTransactions(KeycloakSession session) {
        log.debug("simpleModelTestWithNestedTransactions");

        // Transaction 1
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession session1) -> {

            RealmModel realm = session1.realms().createRealm("foo");
            realm.setDefaultRole(session1.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));

        });

        // Transaction 2 - should be able to see the created realm. Update it
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession session2) -> {

            RealmModel realm = session2.realms().getRealmByName("foo");
            Assertions.assertNotNull(realm);
            session2.getContext().setRealm(realm);

            realm.setAttribute("bar", "baz");

        });

        // Transaction 3 - Doublecheck update is visible. Then rollback transaction!
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession session3) -> {

            RealmModel realm = session3.realms().getRealmByName("foo");
            Assertions.assertNotNull(realm);
            session3.getContext().setRealm(realm);

            String attrValue = realm.getAttribute("bar");
            Assertions.assertEquals("baz", attrValue);

            realm.setAttribute("bar", "baz2");

            session3.getTransactionManager().setRollbackOnly();
        });

        // Transaction 4 - should still see the old value of attribute. Delete realm
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession session4) -> {

            RealmModel realm = session4.realms().getRealmByName("foo");
            Assertions.assertNotNull(realm);
            session4.getContext().setRealm(realm);

            String attrValue = realm.getAttribute("bar");
            Assertions.assertEquals("baz", attrValue);

            new RealmManager(session4).removeRealm(realm);
        });
    }

}
