package org.keycloak.models;

import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserProvider extends Provider {
    // Note: The reason there are so many query methods here is for layering a cache on top of an persistent KeycloakSession

    UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles);
    UserModel addUser(RealmModel realm, String username);
    boolean removeUser(RealmModel realm, UserModel user);

    public void addSocialLink(RealmModel realm, UserModel user, SocialLinkModel socialLink);
    public boolean removeSocialLink(RealmModel realm, UserModel user, String socialProvider);

    UserModel getUserById(String id, RealmModel realm);
    UserModel getUserByUsername(String username, RealmModel realm);
    UserModel getUserByEmail(String email, RealmModel realm);
    UserModel getUserBySocialLink(SocialLinkModel socialLink, RealmModel realm);
    List<UserModel> getUsers(RealmModel realm);
    int getUsersCount(RealmModel realm);
    List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults);
    List<UserModel> searchForUser(String search, RealmModel realm);
    List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults);
    List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm);
    List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults);
    Set<SocialLinkModel> getSocialLinks(UserModel user, RealmModel realm);
    SocialLinkModel getSocialLink(UserModel user, String socialProvider, RealmModel realm);

    void preRemove(RealmModel realm);

    void preRemove(RealmModel realm, UserFederationProviderModel link);

    void preRemove(RealmModel realm, RoleModel role);

    boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input);
    boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input);
    void close();
}
