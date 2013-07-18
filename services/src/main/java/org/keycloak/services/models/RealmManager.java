package org.keycloak.services.models;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.SimpleAgent;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmManager {
    private static AtomicLong counter = new AtomicLong(1);

    public static String generateId() {
        return counter.getAndIncrement() + "-" + System.currentTimeMillis();
    }

    protected IdentitySession identitySession;

    public RealmManager(IdentitySession IdentitySession) {
        this.identitySession = IdentitySession;
    }

    public RealmModel defaultRealm() {
        return getRealm(Realm.DEFAULT_REALM);
    }

    public RealmModel getRealm(String id) {
        Realm existing = identitySession.findRealm(id);
        if (existing == null) {
            return null;
        }
        return new RealmModel(existing, identitySession);
    }

    public RealmModel createRealm(String name) {
        return createRealm(generateId(), name);
    }

    public RealmModel createRealm(String id, String name) {
        Realm newRealm = identitySession.createRealm(id);
        IdentityManager idm = identitySession.createIdentityManager(newRealm);
        SimpleAgent agent = new SimpleAgent(RealmModel.REALM_AGENT_ID);
        idm.add(agent);
        RealmModel realm = new RealmModel(newRealm, identitySession);
        return realm;
    }

    public void generateRealmKeys(RealmModel realm) {
        KeyPair keyPair = null;
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        realm.setPrivateKey(keyPair.getPrivate());
        realm.setPublicKey(keyPair.getPublic());
        realm.updateRealm();
    }
}
