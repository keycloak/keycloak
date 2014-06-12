package org.keycloak.models.cache;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.entities.CachedRole;
import org.keycloak.provider.ProviderSession;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CacheKeycloakSession implements KeycloakSession {
    protected KeycloakCache cache;
    protected ProviderSession providerSession;
    protected KeycloakSession sessionDelegate;
    protected KeycloakTransaction transactionDelegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;

    protected Set<String> realmInvalidations = new HashSet<String>();
    protected boolean clearAll;

    protected KeycloakSession getDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (sessionDelegate != null) return sessionDelegate;
        sessionDelegate = providerSession.getProvider(KeycloakSession.class);
        transactionDelegate = sessionDelegate.getTransaction();
        if (!transactionDelegate.isActive()) {
            transactionDelegate.begin();
            if (setRollbackOnly) {
                transactionDelegate.setRollbackOnly();
            }
        }
        return sessionDelegate;
    }

    public void registerInvalidation(RealmAdapter realm) {
        realmInvalidations.add(realm.getId());
    }

    public void runInvalidations() {
        for (String id : realmInvalidations) {
            cache.invalidateCachedRealmById(id);
        }

    }

    @Override
    public KeycloakTransaction getTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
                if (sessionDelegate == null) return;
                try {
                    sessionDelegate.getTransaction().commit();
                } finally {
                    runInvalidations();
                }
            }

            @Override
            public void rollback() {
                setRollbackOnly = true;
                if (sessionDelegate == null) return;
                try {
                    sessionDelegate.getTransaction().commit();
                } finally {
                    runInvalidations();
                }
            }

            @Override
            public void setRollbackOnly() {
                setRollbackOnly = true;
                if (sessionDelegate == null) return;
                sessionDelegate.getTransaction().setRollbackOnly();
                setRollbackOnly = true;
            }

            @Override
            public boolean getRollbackOnly() {
                return setRollbackOnly;
            }

            @Override
            public boolean isActive() {
                return transactionActive;
            }
        };
    }

    @Override
    public RealmModel createRealm(String name) {
        return getDelegate().createRealm(name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        return getDelegate().createRealm(id, name);
    }

    @Override
    public RealmModel getRealm(String id) {
        CachedRealm cached = cache.getCachedRealm(id);
        if (cached == null) {
            RealmModel model = getDelegate().getRealm(id);
            if (model == null) return null;
            cached = new CachedRealm(cache, this, model);
        }
        return new RealmAdapter(cached, this);
    }

    @Override
    public RealmModel getRealmByName(String name) {
        CachedRealm cached = cache.getCachedRealmByName(name);
        if (cached == null) {
            RealmModel model = getDelegate().getRealmByName(name);
            if (model == null) return null;
            cached = new CachedRealm(cache, this, model);
        }
        return new RealmAdapter(cached, this);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<RealmModel> getRealms() {
        // we don't cache this for now
        return getDelegate().getRealms();
    }

    @Override
    public boolean removeRealm(String id) {
        cache.invalidateCachedRealmById(id);
        boolean didIt = getDelegate().removeRealm(id);
        realmInvalidations.add(id);

        return didIt;
    }

    @Override
    public void removeAllData() {
        cache.clear();
        getDelegate().removeAllData();
        clearAll = true;
    }

    @Override
    public void close() {
        if (sessionDelegate != null) sessionDelegate.close();
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SocialLinkModel getSocialLink(UserModel user, String socialProvider, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ApplicationModel getApplicationById(String id, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(String username, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(String username, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures(RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String ipAddress) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserSessionModel getUserSession(String id, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserSessionModel> getUserSessions(UserModel user, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserSession(UserSessionModel session) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
