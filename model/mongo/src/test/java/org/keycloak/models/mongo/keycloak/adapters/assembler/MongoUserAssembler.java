package org.keycloak.models.mongo.keycloak.adapters.assembler;

import com.mongodb.DB;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;

import java.util.Date;

public class MongoUserAssembler {

    private DB mongo;
    private Jongo jongo;

    private MongoUserAssembler(DB mongo) {
        this.mongo = mongo;
        this.jongo = new Jongo(mongo);
    }

    public static MongoUserAssembler defaultAssembler(DB mongo) {
        return new MongoUserAssembler(mongo);
    }

    public MongoUserAssembler withRealm(String realmId) {
        MongoCollection realmsCollection = jongo.getCollection("realms");
        if(realmsCollection.count("{id: #}", realmId) == 0) {
            MongoRealmEntity realmEntity = new MongoRealmEntity();
            realmEntity.setId(realmId);
            realmsCollection.insert(realmEntity);
        }
        return this;
    }

    public MongoUserAssembler withExpiredUser(String userName, String realmId, Date createdDate) {
        withRealm(realmId);

        MongoCollection realmsCollection = jongo.getCollection("users");
        MongoUserEntity userEntity = new MongoUserEntity();
        userEntity.setUsername(userName);
        userEntity.setId(userName);
        userEntity.setRealmId(realmId);
        userEntity.setEmailVerified(false);
        userEntity.setCreatedTimestamp(createdDate.getTime());
        realmsCollection.insert(userEntity);

        return this;
    }

    public MongoUserAssembler withVerifiedUser(String userName, String realmId, Date createdDate) {
        withRealm(realmId);

        MongoCollection realmsCollection = jongo.getCollection("users");
        MongoUserEntity userEntity = new MongoUserEntity();
        userEntity.setUsername(userName);
        userEntity.setId(userName);
        userEntity.setRealmId(realmId);
        userEntity.setEmailVerified(true);
        userEntity.setCreatedTimestamp(createdDate.getTime());
        realmsCollection.insert(userEntity);

        return this;
    }
}
