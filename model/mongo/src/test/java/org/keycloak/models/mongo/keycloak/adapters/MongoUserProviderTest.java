package org.keycloak.models.mongo.keycloak.adapters;

import com.github.fakemongo.Fongo;
import com.mongodb.DB;
import org.jongo.Jongo;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.connections.mongo.impl.MongoStoreImpl;
import org.keycloak.connections.mongo.impl.context.SimpleMongoStoreInvocationContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.keycloak.adapters.assembler.MongoUserAssembler;
import org.keycloak.models.mongo.keycloak.entities.MongoClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoMigrationModelEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoOfflineUserSessionEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoOnlineUserSessionEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserConsentEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserSessionEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MongoUserProviderTest {

    private DB mongoDB;
    private Jongo jongoFacade;
    private SimpleMongoStoreInvocationContext mongoStoreInvocationContext;

    @Before
    public void before() {
        mongoDB = new Fongo("Test").getDB("Database");
        jongoFacade = new Jongo(mongoDB);

        List<Class<?>> mongoManagedClasses = new ArrayList<>();
        mongoManagedClasses.add(MongoClientEntity.class);
        mongoManagedClasses.add(MongoMigrationModelEntity.class);
        mongoManagedClasses.add(MongoOfflineUserSessionEntity.class);
        mongoManagedClasses.add(MongoOnlineUserSessionEntity.class);
        mongoManagedClasses.add(MongoRealmEntity.class);
        mongoManagedClasses.add(MongoRoleEntity.class);
        mongoManagedClasses.add(MongoUserConsentEntity.class);
        mongoManagedClasses.add(MongoUserEntity .class);
        mongoManagedClasses.add(MongoUserSessionEntity.class);

        MongoStoreImpl mongoStore = new MongoStoreImpl(mongoDB, mongoManagedClasses.toArray(new Class<?>[0]));

        mongoStoreInvocationContext = new SimpleMongoStoreInvocationContext(mongoStore);
    }

    @Test
    public void shouldFindExpiredUsers() throws Exception {
        //given
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        MongoUserAssembler.defaultAssembler(mongoDB)
                .withRealm("test")
                .withExpiredUser("test", "test", yesterday.getTime());

        MongoRealmEntity realmEntity = jongoFacade.getCollection("realms").findOne("{id: #}", "test").as(MongoRealmEntity.class);
        RealmAdapter realm = new RealmAdapter(mock(KeycloakSession.class), realmEntity, mongoStoreInvocationContext);

        MongoUserProvider mongoUserProvider = new MongoUserProvider(mock(KeycloakSession.class), mongoStoreInvocationContext);

        //when
        List<UserModel> users = mongoUserProvider.searchForExpiredUsers(today.getTime(), realm);

        //then
        assertThat(users).hasSize(1);
    }

    @Test
    public void shouldNotFindVerifiedUsers() throws Exception {
        //given
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        MongoUserAssembler.defaultAssembler(mongoDB)
                .withRealm("test")
                .withVerifiedUser("test", "test", yesterday.getTime());

        MongoRealmEntity realmEntity = jongoFacade.getCollection("realms").findOne("{id: #}", "test").as(MongoRealmEntity.class);
        RealmAdapter realm = new RealmAdapter(mock(KeycloakSession.class), realmEntity, mongoStoreInvocationContext);

        MongoUserProvider mongoUserProvider = new MongoUserProvider(mock(KeycloakSession.class), mongoStoreInvocationContext);

        //when
        List<UserModel> users = mongoUserProvider.searchForExpiredUsers(today.getTime(), realm);

        //then
        assertThat(users).isEmpty();
    }

    @Test
    public void shouldNotFindNotExpiredUsers() throws Exception {
        //given
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        MongoUserAssembler.defaultAssembler(mongoDB)
                .withRealm("test")
                .withExpiredUser("test", "test", today.getTime());

        MongoRealmEntity realmEntity = jongoFacade.getCollection("realms").findOne("{id: #}", "test").as(MongoRealmEntity.class);
        RealmAdapter realm = new RealmAdapter(mock(KeycloakSession.class), realmEntity, mongoStoreInvocationContext);

        MongoUserProvider mongoUserProvider = new MongoUserProvider(mock(KeycloakSession.class), mongoStoreInvocationContext);

        //when
        List<UserModel> users = mongoUserProvider.searchForExpiredUsers(yesterday.getTime(), realm);

        //then
        assertThat(users).isEmpty();
    }

}