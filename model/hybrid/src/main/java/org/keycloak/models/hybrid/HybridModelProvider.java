package org.keycloak.models.hybrid;

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
import org.keycloak.models.realms.RealmProvider;
import org.keycloak.models.sessions.SessionProvider;
import org.keycloak.models.users.UserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.Time;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class HybridModelProvider implements ModelProvider {

    private final Mappings mappings;

    private KeycloakSession session;

    private HybridKeycloakTransaction tx;

    public HybridModelProvider(KeycloakSession session) {
        this.session = session;
        this.mappings = new Mappings(this);
    }

    @Override
    public KeycloakTransaction getTransaction() {
        if (tx == null) {
            tx = new HybridKeycloakTransaction(realms().getTransaction(), users().getTransaction(), sessions().getTransaction());
        }

        return tx;
    }

    Mappings mappings() {
        return mappings;
    }

    SessionProvider sessions() {
        return session.getProvider(SessionProvider.class);
    }

    UserProvider users() {
        return session.getProvider(UserProvider.class);
    }

    RealmProvider realms() {
        return session.getProvider(RealmProvider.class);
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        return mappings.wrap(realms().createRealm(id, name));
    }

    @Override
    public RealmModel getRealm(String id) {
        return mappings.wrap(realms().getRealm(id));
    }

    @Override
    public RealmModel getRealmByName(String name) {
        return mappings.wrap(realms().getRealmByName(name));
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return mappings.wrap(realm, users().getUserById(id, realm.getId()));
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        return mappings.wrap(realm, users().getUserByUsername(username, realm.getId()));
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return mappings.wrap(realm, users().getUserByEmail(email, realm.getId()));
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink, RealmModel realm) {
        return mappings.wrap(realm, users().getUserByAttribute("keycloak.socialLink." + socialLink.getSocialProvider() + ".userId", socialLink.getSocialUserId(), realm.getId()));
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return mappings.wrapUsers(realm, users().getUsers(realm.getId()));
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return mappings.wrapUsers(realm, users().searchForUser(search, realm.getId()));
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return mappings.wrapUsers(realm, users().searchForUserByAttributes(attributes, realm.getId()));
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user, RealmModel realm) {
        Set<SocialLinkModel> links = new HashSet<SocialLinkModel>();

        for (Map.Entry<String, String> e : user.getAttributes().entrySet()) {
            if (e.getKey().matches("keycloak\\.socialLink\\..*\\.userId")) {
                String provider = e.getKey().split("\\.")[2];

                SocialLinkModel link = new SocialLinkModel();
                link.setSocialProvider(provider);
                link.setSocialUserId(e.getValue());
                link.setSocialUsername(user.getAttribute("keycloak.socialLink." + provider + ".username"));

                links.add(link);
            }

        }

        return links;
    }

    @Override
    public SocialLinkModel getSocialLink(UserModel user, String provider, RealmModel realm) {
        if (user.getAttribute("keycloak.socialLink." + provider + ".userId") != null) {
            SocialLinkModel link = new SocialLinkModel();
            link.setSocialProvider(provider);
            link.setSocialUserId(user.getAttribute("keycloak.socialLink." + provider + ".userId"));
            link.setSocialUsername(user.getAttribute("keycloak.socialLink." + provider + ".username"));
            return link;
        } else {
            return null;
        }
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        return mappings.wrap(realms().getRoleById(id, realm.getId()));
    }

    @Override
    public ApplicationModel getApplicationById(String id, RealmModel realm) {
        return mappings.wrap(realms().getApplicationById(id, realm.getId()));
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id, RealmModel realm) {
        return mappings.wrap(realms().getOAuthClientById(id, realm.getId()));
    }

    @Override
    public List<RealmModel> getRealms() {
        return mappings.wrap(realms().getRealms());
    }

    @Override
    public boolean removeRealm(String id) {
        if (realms().removeRealm(id)) {
            users().onRealmRemoved(id);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(String username, RealmModel realm) {
        return mappings.wrap(sessions().getUserLoginFailure(username, realm.getId()));
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(String username, RealmModel realm) {
        return mappings.wrap(sessions().addUserLoginFailure(username, realm.getId()));
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures(RealmModel realm) {
        return mappings.wrapLoginFailures(sessions().getAllUserLoginFailures(realm.getId()));
    }

    @Override
    public void close() {
    }

}
