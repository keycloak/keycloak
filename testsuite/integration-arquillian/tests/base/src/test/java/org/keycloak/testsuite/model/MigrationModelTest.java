package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.Version;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.jpa.entities.MigrationModelEntity;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;

import javax.persistence.EntityManager;
import java.util.List;
import org.jboss.logging.Logger;

import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

@AuthServerContainerExclude(REMOTE)
public class MigrationModelTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void test() {
        testingClient.server().run(session -> {
            String currentVersion = Version.VERSION_KEYCLOAK.split("-")[0];

            JpaConnectionProvider p = session.getProvider(JpaConnectionProvider.class);
            EntityManager em = p.getEntityManager();

            List<MigrationModelEntity> l = em.createQuery("select m from MigrationModelEntity m ORDER BY m.updatedTime DESC", MigrationModelEntity.class).getResultList();
            Assert.assertEquals(1, l.size());
            Assert.assertTrue(l.get(0).getId().matches("[\\da-z]{5}"));
            Assert.assertEquals(currentVersion, l.get(0).getVersion());

            MigrationModel m = session.realms().getMigrationModel();
            Assert.assertEquals(currentVersion, m.getStoredVersion());
            Assert.assertEquals(m.getResourcesTag(), l.get(0).getId());

            Time.setOffset(-60000);

            session.realms().getMigrationModel().setStoredVersion("6.0.0");
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

            m = session.realms().getMigrationModel();
            Assert.assertEquals(l.get(0).getId(), m.getResourcesTag());
            Assert.assertEquals(currentVersion, m.getStoredVersion());

            em.remove(l.get(1));
        });
    }

}
