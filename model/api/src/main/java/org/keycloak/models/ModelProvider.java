package org.keycloak.models;

import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ModelProvider extends Provider {
    // Note: The reason there are so many query methods here is for layering a cache on top of an persistent KeycloakSession

    KeycloakTransaction getTransaction();

    RealmModel createRealm(String name);
    RealmModel createRealm(String id, String name);
    RealmModel getRealm(String id);
    RealmModel getRealmByName(String name);

    UserModel getUserById(String id, RealmModel realm);
    UserModel getUserByUsername(String username, RealmModel realm);
    UserModel getUserByEmail(String email, RealmModel realm);
    UserModel getUserBySocialLink(SocialLinkModel socialLink, RealmModel realm);
    List<UserModel> getUsers(RealmModel realm);
    List<UserModel> searchForUser(String search, RealmModel realm);
    List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm);

    Set<SocialLinkModel> getSocialLinks(UserModel user, RealmModel realm);
    SocialLinkModel getSocialLink(UserModel user, String socialProvider, RealmModel realm);

    RoleModel getRoleById(String id, RealmModel realm);
    ApplicationModel getApplicationById(String id, RealmModel realm);
    OAuthClientModel getOAuthClientById(String id, RealmModel realm);
    List<RealmModel> getRealms();
    boolean removeRealm(String id);

    void close();
}
