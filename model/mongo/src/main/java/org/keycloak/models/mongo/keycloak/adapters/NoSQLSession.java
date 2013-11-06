package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.NoSQL;
import org.keycloak.models.mongo.api.query.NoSQLQuery;
import org.keycloak.models.mongo.keycloak.data.RealmData;
import org.keycloak.models.utils.KeycloakSessionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class NoSQLSession implements KeycloakSession {

    private static final NoSQLTransaction PLACEHOLDER = new NoSQLTransaction();
    private final NoSQL noSQL;

    public NoSQLSession(NoSQL noSQL) {
        this.noSQL = noSQL;
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return PLACEHOLDER;
    }

    @Override
    public void close() {
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakSessionUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        if (getRealm(id) != null) {
            throw new IllegalStateException("Realm with id '" + id + "' already exists");
        }

        RealmData newRealm = new RealmData();
        newRealm.setId(id);
        newRealm.setName(name);

        noSQL.saveObject(newRealm);

        RealmAdapter realm = new RealmAdapter(newRealm, noSQL);
        return realm;
    }

    @Override
    public RealmModel getRealm(String id) {
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("id", id)
                .build();
        RealmData realmData = noSQL.loadSingleObject(RealmData.class, query);
        return realmData != null ? new RealmAdapter(realmData, noSQL) : null;
    }

    @Override
    public List<RealmModel> getRealms(UserModel admin) {
        String userId = ((UserAdapter)admin).getUser().getId();
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("realmAdmins", userId)
                .build();
        List<RealmData> realms = noSQL.loadObjects(RealmData.class, query);

        List<RealmModel> results = new ArrayList<RealmModel>();
        for (RealmData realmData : realms) {
            results.add(new RealmAdapter(realmData, noSQL));
        }
        return results;
    }

    @Override
    public void deleteRealm(RealmModel realm) {
        String oid = ((RealmAdapter)realm).getOid();
        noSQL.removeObject(RealmData.class, oid);
    }
}
