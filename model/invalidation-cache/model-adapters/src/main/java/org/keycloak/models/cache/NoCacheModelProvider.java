package org.keycloak.models.cache;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UsernameLoginFailureModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class NoCacheModelProvider implements CacheModelProvider {
    protected KeycloakSession session;
    protected ModelProvider delegate;
    protected KeycloakTransaction transactionDelegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;

    public NoCacheModelProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public ModelProvider getDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (delegate != null) return delegate;
        delegate = session.getProvider(ModelProvider.class);
        transactionDelegate = delegate.getTransaction();
        if (!transactionDelegate.isActive()) {
            transactionDelegate.begin();
            if (setRollbackOnly) {
                transactionDelegate.setRollbackOnly();
            }
        }
        return delegate;
    }

    @Override
    public void registerRealmInvalidation(String id) {
    }

    @Override
    public void registerApplicationInvalidation(String id) {
    }

    @Override
    public void registerRoleInvalidation(String id) {
    }

    @Override
    public void registerOAuthClientInvalidation(String id) {
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
                if (delegate == null) return;
                try {
                    delegate.getTransaction().commit();
                } finally {
                }
            }

            @Override
            public void rollback() {
                setRollbackOnly = true;
                if (delegate == null) return;
                try {
                    delegate.getTransaction().rollback();
                } finally {
                }
            }

            @Override
            public void setRollbackOnly() {
                setRollbackOnly = true;
                if (delegate == null) return;
                delegate.getTransaction().setRollbackOnly();
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
        return getDelegate().getRealm(id);
    }

    @Override
    public RealmModel getRealmByName(String name) {
        return getDelegate().getRealmByName(name);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return getDelegate().getUserById(id, realm);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        return getDelegate().getUserByUsername(username, realm);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return getDelegate().getUserByEmail(email, realm);
    }

    @Override
    public List<RealmModel> getRealms() {
        // we don't cache this for now
        return getDelegate().getRealms();
    }

    @Override
    public boolean removeRealm(String id) {
        return getDelegate().removeRealm(id);
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
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return getDelegate().searchForUser(search, realm);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return getDelegate().searchForUserByAttributes(attributes, realm);
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
    public RoleModel getRoleById(String id, RealmModel realm) {
       return getDelegate().getRoleById(id, realm);
    }

    @Override
    public ApplicationModel getApplicationById(String id, RealmModel realm) {
        return getDelegate().getApplicationById(id, realm);
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id, RealmModel realm) {
        return getDelegate().getOAuthClientById(id, realm);
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(String username, RealmModel realm) {
        return getDelegate().getUserLoginFailure(username, realm);
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(String username, RealmModel realm) {
        return getDelegate().addUserLoginFailure(username, realm);
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures(RealmModel realm) {
        return getDelegate().getAllUserLoginFailures(realm);
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String ipAddress) {
        return getDelegate().createUserSession(realm, user, ipAddress);
    }

    @Override
    public UserSessionModel getUserSession(String id, RealmModel realm) {
        return getDelegate().getUserSession(id, realm);
    }

    @Override
    public List<UserSessionModel> getUserSessions(UserModel user, RealmModel realm) {
        return getDelegate().getUserSessions(user, realm);
    }

    @Override
    public Set<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        return getDelegate().getUserSessions(realm, client);
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        return getDelegate().getActiveUserSessions(realm, client);
    }

    @Override
    public void removeUserSession(UserSessionModel session) {
        getDelegate().removeUserSession(session);
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        getDelegate().removeUserSessions(realm, user);
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        getDelegate().removeExpiredUserSessions(realm);
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        getDelegate().removeUserSessions(realm);
    }

    @Override
    public void registerUserInvalidation(String id) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
