package org.keycloak.services.models.picketlink;

import org.jboss.resteasy.spi.NotImplementedYetException;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakTransaction;
import org.keycloak.services.models.RealmModel;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.SimpleAgent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PicketlinkKeycloakSession implements KeycloakSession {
    protected IdentitySession session;

    private static AtomicLong counter = new AtomicLong(1);
    public static String generateId() {
        return counter.getAndIncrement() + "-" + System.currentTimeMillis();
    }

    public PicketlinkKeycloakSession(IdentitySession session) {
        this.session = session;
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return new PicketlinkKeycloakTransaction(session.getTransaction());
    }

    @Override
    public RealmAdapter createRealm(String name) {
        return createRealm(generateId(), name);
    }

    @Override
    public RealmAdapter createRealm(String id, String name) {
        Realm newRealm = session.createRealm(id);
        IdentityManager idm = session.createIdentityManager(newRealm);
        SimpleAgent agent = new SimpleAgent(RealmAdapter.REALM_AGENT_ID);
        idm.add(agent);
        RealmAdapter realm = new RealmAdapter(newRealm, session);
        return realm;
    }

    @Override
    public RealmAdapter getRealm(String id) {
        Realm existing = session.findRealm(id);
        if (existing == null) {
            return null;
        }
        return new RealmAdapter(existing, session);
    }

    @Override
    public void deleteRealm(RealmModel realm) {
        throw new NotImplementedYetException();

    }

    @Override
    public void close() {
        session.close();
    }
}
