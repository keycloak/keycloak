/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.transaction;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.testsuite.model.util.TransactionController;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RequireProvider(RealmProvider.class)
public class StorageTransactionTest extends KeycloakModelTest {

    private String realmId;

    @Override
    protected void createEnvironment(KeycloakSession s) {
        RealmModel r = s.realms().createRealm("1");
        s.getContext().setRealm(r);
        r.setDefaultRole(s.roles().addRealmRole(r, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + r.getName()));
        r.setAttribute("k1", "v1");
        r.setSsoSessionIdleTimeout(1000);
        r.setSsoSessionMaxLifespan(2000);

        realmId = r.getId();
    }

    @Override
    protected void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Override
    protected boolean isUseSameKeycloakSessionFactoryForAllThreads() {
        return true;
    }

    @Test
    public void testTwoTransactionsSequentially() throws Exception {
        try (TransactionController tx1 = new TransactionController(getFactory());
             TransactionController tx2 = new TransactionController(getFactory())) {
            tx1.begin();
            assertThat(
                    tx1.runStep(session -> {
                        session.realms().getRealm(realmId).setAttribute("k2", "v1");
                        return session.realms().getRealm(realmId).getAttribute("k2");
                    }), equalTo("v1"));
            tx1.commit();

            tx2.begin();
            assertThat(
                    tx2.runStep(session -> session.realms().getRealm(realmId).getAttribute("k2")),
                    equalTo("v1"));
            tx2.commit();
        }
    }

    @Test
    public void testRepeatableRead() throws Exception {
        try (TransactionController tx1 = new TransactionController(getFactory());
             TransactionController tx2 = new TransactionController(getFactory());
             TransactionController tx3 = new TransactionController(getFactory())) {

            tx1.begin();
            tx2.begin();
            tx3.begin();

            // Read original value in tx1
            assertThat(
                    tx1.runStep(session -> session.realms().getRealm(realmId).getAttribute("k1")),
                    equalTo("v1"));

            // change value to new in tx2
            tx2.runStep(session -> {
                session.realms().getRealm(realmId).setAttribute("k1", "v2");
                return null;
            });
            tx2.commit();

            // tx1 should still return the value that already read
            assertThat(
                    tx1.runStep(session -> session.realms().getRealm(realmId).getAttribute("k1")),
                    equalTo("v1"));

            // tx3 should return the new value
            assertThat(
                    tx3.runStep(session -> session.realms().getRealm(realmId).getAttribute("k1")),
                    equalTo("v2"));
            tx1.commit();
            tx3.commit();
        }
    }
}
