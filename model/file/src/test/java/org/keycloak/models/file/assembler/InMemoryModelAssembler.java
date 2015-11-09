package org.keycloak.models.file.assembler;

import org.keycloak.connections.file.InMemoryModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.entities.RealmEntity;
import org.keycloak.models.entities.UserEntity;
import org.keycloak.models.file.adapter.RealmAdapter;
import org.keycloak.models.file.adapter.UserAdapter;

import java.util.Date;

public class InMemoryModelAssembler {

    private InMemoryModel inMemoryModel;

    private InMemoryModelAssembler() {
        inMemoryModel = new InMemoryModel();
    }

    public static InMemoryModelAssembler emptyModel() {
        return new InMemoryModelAssembler();
    }

    public InMemoryModelAssembler withRealm(String id) {
        if(inMemoryModel.getRealm(id) == null) {
            RealmEntity realmEntity = new RealmEntity();
            realmEntity.setId(id);
            RealmAdapter realmAdapter = new RealmAdapter(null, realmEntity, inMemoryModel);
            inMemoryModel.putRealm(id, realmAdapter);
        }
        return this;
    }

    public InMemoryModelAssembler withExpiredUser(String realmId, String userId, Date created) {
        withRealm(realmId);

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setCreatedTimestamp(created.getTime());
        user.setEmailVerified(false);
        addUser(user, inMemoryModel.getRealm(realmId));

        return this;
    }

    public InMemoryModelAssembler withVerifiedUser(String realmId, String userId, Date created) {
        withRealm(realmId);

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setCreatedTimestamp(created.getTime());
        user.setEmailVerified(true);
        addUser(user, inMemoryModel.getRealm(realmId));

        return this;
    }

    private void addUser(UserEntity userEntity, RealmModel realm) {
        UserAdapter userAdapter = new UserAdapter(realm, userEntity, inMemoryModel);
        inMemoryModel.putUser(realm.getId(), userEntity.getId(), userAdapter);
    }

    public InMemoryModel assemble() {
        return inMemoryModel;
    }
}
