package org.keycloak.models.jpa;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.assembler.JPAUserAssembler;
import org.keycloak.models.jpa.entities.RealmEntity;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Calendar;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class JpaUserProviderTest {

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    @Before
    public void before() {
        entityManagerFactory = Persistence.createEntityManagerFactory("keycloak-test");
        entityManager = entityManagerFactory.createEntityManager();
    }

    @After
    public void after() {
        entityManager.close();
        entityManagerFactory.close();
    }

    @Test
    public void shouldFindExpiredUsers() throws Exception {
        //given
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        JPAUserAssembler.defaultAssembler(entityManager)
                .withRealm("test")
                .withExpiredUser("test", "test", yesterday.getTime())
                .commit();

        JpaUserProvider jpaUserProvider = new JpaUserProvider(mock(KeycloakSession.class), entityManager);

        RealmAdapter realm = new RealmAdapter(mock(KeycloakSession.class), entityManager, entityManager.find(RealmEntity.class, "test"));

        //when
        List<UserModel> users = jpaUserProvider.searchForExpiredUsers(today.getTime(), realm);

        //then
        assertThat(users).hasSize(1);
    }

    @Test
    public void shouldNotFindVerifiedUsers() throws Exception {
        //given
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        JPAUserAssembler.defaultAssembler(entityManager)
                .withRealm("test")
                .withVerifiedUser("test", "test", yesterday.getTime())
                .commit();

        JpaUserProvider jpaUserProvider = new JpaUserProvider(mock(KeycloakSession.class), entityManager);

        RealmAdapter realm = new RealmAdapter(mock(KeycloakSession.class), entityManager, entityManager.find(RealmEntity.class, "test"));

        //when
        List<UserModel> users = jpaUserProvider.searchForExpiredUsers(today.getTime(), realm);

        //then
        assertThat(users).isEmpty();
    }

    @Test
    public void shouldNotFindNotExpiredUsers() throws Exception {
        //given
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        JPAUserAssembler.defaultAssembler(entityManager)
                .withRealm("test")
                .withExpiredUser("test", "test", today.getTime())
                .commit();

        JpaUserProvider jpaUserProvider = new JpaUserProvider(mock(KeycloakSession.class), entityManager);

        RealmAdapter realm = new RealmAdapter(mock(KeycloakSession.class), entityManager, entityManager.find(RealmEntity.class, "test"));

        //when
        List<UserModel> users = jpaUserProvider.searchForExpiredUsers(yesterday.getTime(), realm);

        //then
        assertThat(users).isEmpty();
    }

}