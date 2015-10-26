package org.keycloak.models.jpa.assembler;

import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.UserEntity;

import javax.persistence.EntityManager;
import java.util.Date;

public class JPAUserAssembler {

    private EntityManager entityManager;

    private JPAUserAssembler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public static JPAUserAssembler defaultAssembler(EntityManager entityManager) {
        entityManager.getTransaction().begin();
        return new JPAUserAssembler(entityManager);
    }

    public JPAUserAssembler withRealm(String realmId) {
        if(entityManager.find(RealmEntity.class, realmId) == null) {
            RealmEntity realmEntity = new RealmEntity();
            realmEntity.setId(realmId);
            realmEntity.setName(realmId);
            entityManager.persist(realmEntity);
        }
        return this;
    }

    public JPAUserAssembler withExpiredUser(String userName, String realmId, Date createdDate) {
        withRealm(realmId);

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(userName);
        userEntity.setId(userName);
        userEntity.setRealmId(realmId);
        userEntity.setEmailVerified(false);
        userEntity.setCreatedTimestamp(createdDate.getTime());
        entityManager.persist(userEntity);

        return this;
    }

    public JPAUserAssembler withVerifiedUser(String userName, String realmId, Date createdDate) {
        withRealm(realmId);

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(userName);
        userEntity.setId(userName);
        userEntity.setRealmId(realmId);
        userEntity.setEmailVerified(true);
        userEntity.setCreatedTimestamp(createdDate.getTime());
        entityManager.persist(userEntity);

        return this;
    }

    public void commit() {
        entityManager.getTransaction().commit();
    }

}
