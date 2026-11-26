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

package org.keycloak.tests.transactions;

import java.util.Map;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class TransactionsTest {

    private static final String ATTRIBUTE = "transactionsTest";

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    @DatabaseTest
    public void testCommitPersistsChanges() {
        String realmName = realm.getName();
        realm.cleanup().add(TransactionsTest::removeTestAttribute);

        runOnServer.run(session -> {
            Assertions.assertTrue(session.getTransactionManager().isActive());

            RealmModel realmModel = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realmModel);
            realmModel.setAttribute(ATTRIBUTE, "test");

            session.getTransactionManager().commit();
            Assertions.assertFalse(session.getTransactionManager().isActive());
        });

        assertThat(getTestAttribute(), is("test"));
    }

    @Test
    @DatabaseTest
    public void testRollbackDiscardsChanges() {
        String realmName = realm.getName();
        realm.cleanup().add(TransactionsTest::removeTestAttribute);

        runOnServer.run(session -> {
            Assertions.assertTrue(session.getTransactionManager().isActive());

            RealmModel realmModel = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realmModel);
            realmModel.setAttribute(ATTRIBUTE, "test-rollback");

            session.getTransactionManager().rollback();
            Assertions.assertFalse(session.getTransactionManager().isActive());
        });

        assertThat(getTestAttribute(), nullValue());
    }

    private String getTestAttribute() {
        Map<String, String> attributes = realm.admin().toRepresentation().getAttributes();
        return attributes != null ? attributes.get(ATTRIBUTE) : null;
    }

    private static void removeTestAttribute(RealmResource realmResource) {
        RealmRepresentation rep = realmResource.toRepresentation();
        if (rep.getAttributes() != null && rep.getAttributes().remove(ATTRIBUTE) != null) {
            realmResource.update(rep);
        }
    }

}
