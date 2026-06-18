package org.keycloak.tests.error;

import jakarta.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServerException;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
class JpaExceptionHandlingTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    /**
     * This tests {@link org.keycloak.connections.jpa.support.EntityManagerProxy#convert}
     */
    @Test
    public void convertTableUniqueConstraintsToModelExceptions() {
        String realmName = managedRealm.getName();

        Exception RunOnServerException = assertThrows(RunOnServerException.class, () ->
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

            // Some raw inserts to the table to avoid any model logic to capture that in any other way
            // before it is inserted to the table.

            UserEntity userEntity1 = new UserEntity();
            userEntity1.setId("1");
            userEntity1.setUsername("duplicate");
            userEntity1.setRealmId(realm.getId());
            em.persist(userEntity1);

            UserEntity userEntity2 = new UserEntity();
            userEntity2.setId("2");
            userEntity2.setUsername("duplicate");
            userEntity2.setRealmId(realm.getId());
            em.persist(userEntity2);

            // The flush to the database will trigger the constraint violation.
            em.flush();
        }));

        assertThat(RunOnServerException.getCause(), instanceOf(ModelDuplicateException.class));
    }

}
