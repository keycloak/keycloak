package org.keycloak.services.models.nosql.adapters;

import java.util.List;

import org.jboss.resteasy.spi.NotImplementedYetException;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakTransaction;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.nosql.data.RealmData;
import org.keycloak.services.models.nosql.api.NoSQL;

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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RealmModel createRealm(String name) {
        RealmData newRealm = new RealmData();
        newRealm.setName(name);

        noSQL.saveObject(newRealm);

        RealmAdapter realm = new RealmAdapter(newRealm, noSQL);
        return realm;
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        // Ignore ID for now. It seems that it exists just for workaround picketlink
        return createRealm(name);
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmData realmData = noSQL.loadObject(RealmData.class, id);
        return new RealmAdapter(realmData, noSQL);
    }

    @Override
    public List<RealmModel> getRealms(UserModel admin) {
        throw new NotImplementedYetException();
    }

    @Override
    public void deleteRealm(RealmModel realm) {
        noSQL.removeObject(RealmData.class, realm.getId());
    }
}
