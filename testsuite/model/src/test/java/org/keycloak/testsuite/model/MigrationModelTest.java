/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.keycloak.common.Version;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.migration.MigrationModel;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.DeploymentStateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.jpa.entities.MigrationModelEntity;

import org.jboss.logging.Logger;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RequireProvider(value=RealmProvider.class, only="jpa")
@RequireProvider(value=ClientProvider.class, only="jpa")
@RequireProvider(value=ClientScopeProvider.class, only="jpa")
public class MigrationModelTest extends KeycloakModelTest {

    private static final Logger logger = Logger.getLogger(MigrationModelTest.class);

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "realm");
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Test
    public void multipleEntities() {
        inComittedTransaction(1, (session , i) -> {
            String currentVersion = new ModelVersion(Version.VERSION).toString();
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

            List<MigrationModelEntity> entities = getMigrationEntities(em);
            assertThat(entities.size(), is(1));
            assertMigrationModelEntity(entities.get(0), currentVersion);

            MigrationModel m = session.getProvider(DeploymentStateProvider.class).getMigrationModel();
            assertThat(m.getStoredVersion(), is(currentVersion));
            assertThat(entities.get(0).getId(), is(m.getResourcesTag()));

            setTimeOffset(-60000);

            try {
                session.getProvider(DeploymentStateProvider.class).getMigrationModel().setStoredVersion("6.0.0");
                em.flush();

                setTimeOffset(0);

                entities = getMigrationEntities(em);
                assertThat(entities.size(), is(2));

                logger.info("MigrationModelEntity entries: ");
                entities.forEach(entity -> log.infof("--id: %s; %s; %s", entity.getId(), entity.getVersion(), entity.getUpdateTime()));

                assertMigrationModelEntity(entities.get(0), currentVersion);
                assertMigrationModelEntity(entities.get(1), "6.0.0");

                m = session.getProvider(DeploymentStateProvider.class).getMigrationModel();
                assertThat(m.getStoredVersion(), is(currentVersion));
                assertThat(entities.get(0).getId(), is(m.getResourcesTag()));
            } finally {
                em.remove(entities.get(1));
            }

            return null;
        });
    }

    @Test
    public void duplicates() {
        inComittedTransaction(1, (session, i) -> {
            String currentVersion = new ModelVersion(Version.VERSION).toString();
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

            List<MigrationModelEntity> entities = getMigrationEntities(em);
            assertThat(entities.size(), is(1));
            assertMigrationModelEntity(entities.get(0), currentVersion);

            MigrationModel m = session.getProvider(DeploymentStateProvider.class).getMigrationModel();
            assertThat(m.getStoredVersion(), is(currentVersion));
            assertThat(entities.get(0).getId(), is(m.getResourcesTag()));

            try {
                setTimeOffset(-60000);
                session.getProvider(DeploymentStateProvider.class).getMigrationModel().setStoredVersion("26.2.4");
                em.flush();

                setTimeOffset(-30000);
                session.getProvider(DeploymentStateProvider.class).getMigrationModel().setStoredVersion("26.2.5");
                em.flush();

                setTimeOffset(0);

                entities = getMigrationEntities(em);
                assertThat(entities.size(), is(3));

                logger.info("MigrationModelEntity entries: ");
                entities.forEach(entity -> log.infof("--id: %s; %s; %s", entity.getId(), entity.getVersion(), entity.getUpdateTime()));

                assertMigrationModelEntity(entities.get(0), currentVersion);
                assertMigrationModelEntity(entities.get(1), "26.2.5");
                assertMigrationModelEntity(entities.get(2), "26.2.4");

                setTimeOffset(-29999);
                session.getProvider(DeploymentStateProvider.class).getMigrationModel().setStoredVersion("26.2.5");
                assertThrows(ModelDuplicateException.class, em::flush);

                entities = getMigrationEntities(em);
                assertThat(entities.size(), is(3));

                assertThat(m.getStoredVersion(), is(currentVersion));
                assertThat(entities.get(0).getId(), is(m.getResourcesTag()));
            } finally {
                em.remove(entities.get(1));
                em.remove(entities.get(2));
            }

            return null;
        });
    }

    @Test
    public void duplicatedUpdateTime() {
        inComittedTransaction(1, (session, i) -> {
            String currentVersion = new ModelVersion(Version.VERSION).toString();
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

            List<MigrationModelEntity> entities = getMigrationEntities(em);
            assertThat(entities.size(), is(1));
            assertMigrationModelEntity(entities.get(0), currentVersion);

            MigrationModel m = session.getProvider(DeploymentStateProvider.class).getMigrationModel();
            assertThat(m.getStoredVersion(), is(currentVersion));
            assertThat(entities.get(0).getId(), is(m.getResourcesTag()));

            try {
                MigrationModelEntity mm1 = new MigrationModelEntity();
                mm1.setId("a");
                mm1.setUpdatedTime(0);
                mm1.setVersion("26.0.0");
                em.persist(mm1);

                em.flush();

                // Same time, everything different - testing for the constraint to be present
                MigrationModelEntity mm2 = new MigrationModelEntity();
                mm2.setId("b");
                mm2.setUpdatedTime(0);
                mm2.setVersion("26.0.1");
                em.persist(mm2);

                // added at the same time - exception thrown by the unique constraint
                assertThrows(ModelDuplicateException.class, em::flush);

            } finally {
                em.remove(em.find(MigrationModelEntity.class, "a"));
            }

            return null;
        });
    }

    private void assertMigrationModelEntity(MigrationModelEntity model, String expectedVersion) {
        assertThat(model, notNullValue());
        assertTrue(model.getId().matches("[\\da-z]{5}"));
        assertThat(model.getVersion(), is(expectedVersion));
    }

    private List<MigrationModelEntity> getMigrationEntities(EntityManager em) {
        return em.createQuery("select m from MigrationModelEntity m ORDER BY m.updatedTime DESC", MigrationModelEntity.class).getResultList();
    }
}
