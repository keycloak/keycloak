package org.keycloak.services.models.nosql.keycloak.adapters;

import java.util.ArrayList;
import java.util.List;

import org.jboss.resteasy.spi.NotImplementedYetException;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakTransaction;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.nosql.api.query.NoSQLQuery;
import org.keycloak.services.models.nosql.api.query.NoSQLQueryBuilder;
import org.keycloak.services.models.nosql.impl.MongoDBQueryBuilder;
import org.keycloak.services.models.nosql.keycloak.data.RealmData;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.picketlink.PicketlinkKeycloakSession;

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
        return createRealm(PicketlinkKeycloakSession.generateId(), name);
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
