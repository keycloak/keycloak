package org.keycloak.models.cache;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.entities.CachedUser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultCacheUserProvider implements CacheUserProvider {
    protected UserCache cache;
    protected KeycloakSession session;
    protected UserProvider delegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;

    protected Map<String, String> userInvalidations = new HashMap<String, String>();
    protected Set<String> realmInvalidations = new HashSet<String>();
    protected Map<String, UserModel> managedUsers = new HashMap<String, UserModel>();

    protected boolean clearAll;

    public DefaultCacheUserProvider(UserCache cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;

        session.getTransaction().enlistAfterCompletion(getTransaction());
    }

    @Override
    public boolean isEnabled() {
        return cache.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        cache.setEnabled(enabled);
    }

    @Override
    public UserProvider getDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (delegate != null) return delegate;
        delegate = session.getProvider(UserProvider.class);
        return delegate;
    }

    @Override
    public void registerUserInvalidation(RealmModel realm, String id) {
        userInvalidations.put(id, realm.getId());
    }

    protected void runInvalidations() {
        for (Map.Entry<String, String> invalidation : userInvalidations.entrySet()) {
            cache.invalidateCachedUserById(invalidation.getValue(), invalidation.getKey());
        }
        for (String realmId : realmInvalidations) {
            cache.invalidateRealmUsers(realmId);
        }
    }

    private KeycloakTransaction getTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
                if (delegate == null) return;
                if (clearAll) {
                    cache.clear();
                }
                runInvalidations();
                transactionActive = false;
            }

            @Override
            public void rollback() {
                setRollbackOnly = true;
                runInvalidations();
                transactionActive = false;
            }

            @Override
            public void setRollbackOnly() {
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
    public UserModel getUserById(String id, RealmModel realm) {
        if (!cache.isEnabled()) return getDelegate().getUserById(id, realm);
        if (realmInvalidations.contains(realm.getId())) {
            return getDelegate().getUserById(id, realm);
        }
        if (userInvalidations.containsKey(id)) {
            return getDelegate().getUserById(id, realm);
        }

        CachedUser cached = cache.getCachedUser(realm.getId(), id);
        if (cached == null) {
            UserModel model = getDelegate().getUserById(id, realm);
            if (model == null) return null;
            if (userInvalidations.containsKey(id)) return model;
            cached = new CachedUser(realm, model);
            cache.addCachedUser(realm.getId(), cached);
        } else if (managedUsers.containsKey(id)) {
            return managedUsers.get(id);
        }
        UserAdapter adapter = new UserAdapter(cached, this, session, realm);
        managedUsers.put(id, adapter);
        return adapter;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        if (!cache.isEnabled()) return getDelegate().getUserByUsername(username, realm);
        if (realmInvalidations.contains(realm.getId())) {
            return getDelegate().getUserByUsername(username, realm);
        }
        CachedUser cached = cache.getCachedUserByUsername(realm.getId(), username);
        if (cached == null) {
            UserModel model = getDelegate().getUserByUsername(username, realm);
            if (model == null) return null;
            if (userInvalidations.containsKey(model.getId())) return model;
            cached = new CachedUser(realm, model);
            cache.addCachedUser(realm.getId(), cached);
        } else if (userInvalidations.containsKey(cached.getId())) {
            return getDelegate().getUserById(cached.getId(), realm);
        } else if (managedUsers.containsKey(cached.getId())) {
            return managedUsers.get(cached.getId());
        }
        UserAdapter adapter = new UserAdapter(cached, this, session, realm);
        managedUsers.put(cached.getId(), adapter);
        return adapter;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        if (!cache.isEnabled()) return getDelegate().getUserByEmail(email, realm);
        if (realmInvalidations.contains(realm.getId())) {
            return getDelegate().getUserByEmail(email, realm);
        }
        CachedUser cached = cache.getCachedUserByEmail(realm.getId(), email);
        if (cached == null) {
            UserModel model = getDelegate().getUserByEmail(email, realm);
            if (model == null) return null;
            if (userInvalidations.containsKey(model.getId())) return model;
            cached = new CachedUser(realm, model);
            cache.addCachedUser(realm.getId(), cached);
        } else if (userInvalidations.containsKey(cached.getId())) {
            return getDelegate().getUserByEmail(email, realm);
        } else if (managedUsers.containsKey(cached.getId())) {
            return managedUsers.get(cached.getId());
        }
        UserAdapter adapter = new UserAdapter(cached, this, session, realm);
        managedUsers.put(cached.getId(), adapter);
        return adapter;
    }

    @Override
    public void close() {
        if (delegate != null) delegate.close();
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink, RealmModel realm) {
        return getDelegate().getUserBySocialLink(socialLink, realm);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getDelegate().getUsers(realm);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return getDelegate().getUsersCount(realm);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return getDelegate().getUsers(realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return getDelegate().searchForUser(search, realm);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        return getDelegate().searchForUser(search, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return getDelegate().searchForUserByAttributes(attributes, realm);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        return getDelegate().searchForUserByAttributes(attributes, realm, firstResult, maxResults);
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user, RealmModel realm) {
        return getDelegate().getSocialLinks(user, realm);
    }

    @Override
    public SocialLinkModel getSocialLink(UserModel user, String socialProvider, RealmModel realm) {
        return getDelegate().getSocialLink(user, socialProvider, realm);
    }

    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles) {
        return getDelegate().addUser(realm, id, username, addDefaultRoles);
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return getDelegate().addUser(realm, username);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        if (!cache.isEnabled()) return getDelegate().removeUser(realm, user);
        registerUserInvalidation(realm, user.getId());
        return getDelegate().removeUser(realm, user);
    }

    @Override
    public void addSocialLink(RealmModel realm, UserModel user, SocialLinkModel socialLink) {
        getDelegate().addSocialLink(realm, user, socialLink);
    }

    @Override
    public boolean removeSocialLink(RealmModel realm, UserModel user, String socialProvider) {
        return getDelegate().removeSocialLink(realm, user, socialProvider);
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        return getDelegate().validCredentials(realm, user, input);
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
        return getDelegate().validCredentials(realm, user, input);
    }

    @Override
    public void preRemove(RealmModel realm) {
        realmInvalidations.add(realm.getId());
        getDelegate().preRemove(realm);
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        getDelegate().preRemove(realm, role);
    }

    @Override
    public void preRemove(RealmModel realm, UserFederationProviderModel link) {
        realmInvalidations.add(realm.getId()); // easier to just invalidate whole realm
        getDelegate().preRemove(realm, link);
    }
}
