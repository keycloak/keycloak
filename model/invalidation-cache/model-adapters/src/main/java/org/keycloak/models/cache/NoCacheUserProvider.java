package org.keycloak.models.cache;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class NoCacheUserProvider implements CacheUserProvider {
    protected KeycloakSession session;
    protected UserProvider delegate;

    public NoCacheUserProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
    }

    @Override
    public UserProvider getDelegate() {
        if (delegate != null) return delegate;
        delegate = session.getProvider(UserProvider.class);
        return delegate;
    }

    @Override
    public void registerUserInvalidation(RealmModel realm, String id) {
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
    public void close() {
        if (delegate != null) delegate.close();
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        return getDelegate().getUserByFederatedIdentity(socialLink, realm);
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
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
        return getDelegate().getFederatedIdentities(user, realm);
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        return getDelegate().getFederatedIdentity(user, socialProvider, realm);
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
        return getDelegate().removeUser(realm, user);
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink) {
        getDelegate().addFederatedIdentity(realm, user, socialLink);
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        return getDelegate().removeFederatedIdentity(realm, user, socialProvider);
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
        getDelegate().preRemove(realm);
    }

    @Override
    public void preRemove(RealmModel realm, UserFederationProviderModel link) {
        getDelegate().preRemove(realm, link);
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        getDelegate().preRemove(realm, role);
    }
}
