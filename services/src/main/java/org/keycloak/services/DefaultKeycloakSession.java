package org.keycloak.services;

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
import org.keycloak.models.cache.CacheModelProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakSession implements KeycloakSession {

    private DefaultKeycloakSessionFactory factory;
    private Map<Integer, Provider> providers = new HashMap<Integer, Provider>();
    private final ModelProvider model;

    public DefaultKeycloakSession(DefaultKeycloakSessionFactory factory) {
        this.factory = factory;

        if (factory.getDefaultProvider(CacheModelProvider.class) != null) {
            model = getProvider(CacheModelProvider.class);
        } else {
            model = getProvider(ModelProvider.class);
        }
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return model.getTransaction();
    }

    public <T extends Provider> T getProvider(Class<T> clazz) {
        Integer hash = clazz.hashCode();
        T provider = (T) providers.get(hash);
        if (provider == null) {
            ProviderFactory<T> providerFactory = factory.getProviderFactory(clazz);
            if (providerFactory != null) {
                provider = providerFactory.create(this);
                providers.put(hash, provider);
            }
        }
        return provider;
    }

    public <T extends Provider> T getProvider(Class<T> clazz, String id) {
        Integer hash = clazz.hashCode() + id.hashCode();
        T provider = (T) providers.get(hash);
        if (provider == null) {
            ProviderFactory<T> providerFactory = factory.getProviderFactory(clazz, id);
            if (providerFactory != null) {
                provider = providerFactory.create(this);
                providers.put(hash, provider);
            }
        }
        return provider;
    }

    public <T extends Provider> Set<String> listProviderIds(Class<T> clazz) {
        return factory.getAllProviderIds(clazz);
    }

    @Override
    public <T extends Provider> Set<T> getAllProviders(Class<T> clazz) {
        Set<T> providers = new HashSet<T>();
        for (String id : listProviderIds(clazz)) {
            providers.add(getProvider(clazz, id));
        }
        return providers;
    }

    @Override
    public RealmModel createRealm(String name) {
        return model.createRealm(name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        return model.createRealm(id, name);
    }

    @Override
    public RealmModel getRealm(String id) {
        return model.getRealm(id);
    }

    @Override
    public RealmModel getRealmByName(String name) {
        return model.getRealmByName(name);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return model.getUserById(id, realm);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        return model.getUserByUsername(username, realm);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return model.getUserByEmail(email, realm);
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink, RealmModel realm) {
        return model.getUserBySocialLink(socialLink, realm);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return model.getUsers(realm);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return model.searchForUser(search, realm);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return model.searchForUserByAttributes(attributes, realm);
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user, RealmModel realm) {
        return model.getSocialLinks(user, realm);
    }

    @Override
    public SocialLinkModel getSocialLink(UserModel user, String socialProvider, RealmModel realm) {
        return model.getSocialLink(user, socialProvider, realm);
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        return model.getRoleById(id, realm);
    }

    @Override
    public ApplicationModel getApplicationById(String id, RealmModel realm) {
        return model.getApplicationById(id, realm);
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id, RealmModel realm) {
        return model.getOAuthClientById(id, realm);
    }

    @Override
    public List<RealmModel> getRealms() {
        return model.getRealms();
    }

    @Override
    public boolean removeRealm(String id) {
        return model.removeRealm(id);
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(String username, RealmModel realm) {
        return model.getUserLoginFailure(username, realm);
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(String username, RealmModel realm) {
        return model.addUserLoginFailure(username, realm);
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures(RealmModel realm) {
        return model.getAllUserLoginFailures(realm);
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String ipAddress) {
        return model.createUserSession(realm, user, ipAddress);
    }

    @Override
    public UserSessionModel getUserSession(String id, RealmModel realm) {
        return model.getUserSession(id, realm);
    }

    @Override
    public List<UserSessionModel> getUserSessions(UserModel user, RealmModel realm) {
        return model.getUserSessions(user, realm);
    }

    @Override
    public Set<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        return model.getUserSessions(realm, client);
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        return model.getActiveUserSessions(realm, client);
    }

    @Override
    public void removeUserSession(UserSessionModel session) {
        model.removeUserSession(session);
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        model.removeUserSessions(realm, user);
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        model.removeExpiredUserSessions(realm);
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        model.removeUserSessions(realm);
    }

    public void close() {
        for (Provider p : providers.values()) {
            p.close();
        }
    }

}
