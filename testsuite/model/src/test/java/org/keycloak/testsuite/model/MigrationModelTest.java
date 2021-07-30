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
import javax.persistence.EntityManager;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.Version;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.jpa.entities.MigrationModelEntity;
import org.keycloak.models.DeploymentStateProvider;

@RequireProvider(value=RealmProvider.class, only="jpa")
@RequireProvider(value=ClientProvider.class, only="jpa")
@RequireProvider(value=ClientScopeProvider.class, only="jpa")
public class MigrationModelTest extends KeycloakModelTest {

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("realm");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Test
    public void test() {
        inComittedTransaction(1, (session , i) -> {

            String currentVersion = Version.VERSION_KEYCLOAK.replaceAll("^(\\d+(?:\\.\\d+){0,2}).*$", "$1");

            JpaConnectionProvider p = session.getProvider(JpaConnectionProvider.class);
            EntityManager em = p.getEntityManager();

            List<MigrationModelEntity> l = em.createQuery("select m from MigrationModelEntity m ORDER BY m.updatedTime DESC", MigrationModelEntity.class).getResultList();
            Assert.assertEquals(1, l.size());
            Assert.assertTrue(l.get(0).getId().matches("[\\da-z]{5}"));
            Assert.assertEquals(currentVersion, l.get(0).getVersion());

            MigrationModel m = session.getProvider(DeploymentStateProvider.class).getMigrationModel();
            Assert.assertEquals(currentVersion, m.getStoredVersion());
            Assert.assertEquals(m.getResourcesTag(), l.get(0).getId());

            Time.setOffset(-60000);

            session.getProvider(DeploymentStateProvider.class).getMigrationModel().setStoredVersion("6.0.0");
            em.flush();

            Time.setOffset(0);

            l = em.createQuery("select m from MigrationModelEntity m ORDER BY m.updatedTime DESC", MigrationModelEntity.class).getResultList();
            Assert.assertEquals(2, l.size());
            Logger.getLogger(MigrationModelTest.class).info("MigrationModelEntity entries: ");
            Logger.getLogger(MigrationModelTest.class).info("--id: " + l.get(0).getId() + "; " + l.get(0).getVersion() + "; " + l.get(0).getUpdateTime());
            Logger.getLogger(MigrationModelTest.class).info("--id: " + l.get(1).getId() + "; " + l.get(1).getVersion() + "; " + l.get(1).getUpdateTime());
            Assert.assertTrue(l.get(0).getId().matches("[\\da-z]{5}"));
            Assert.assertEquals(currentVersion, l.get(0).getVersion());
            Assert.assertTrue(l.get(1).getId().matches("[\\da-z]{5}"));
            Assert.assertEquals("6.0.0", l.get(1).getVersion());

            m = session.getProvider(DeploymentStateProvider.class).getMigrationModel();
            Assert.assertEquals(l.get(0).getId(), m.getResourcesTag());
            Assert.assertEquals(currentVersion, m.getStoredVersion());

            em.remove(l.get(1));

            return null;
        });
    }
}
