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

    UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions);
    UserModel addUser(RealmModel realm, String username);
    boolean removeUser(RealmModel realm, UserModel user);

    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel socialLink);
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider);
    void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel);

    UserModel getUserById(String id, RealmModel realm);
    UserModel getUserByUsername(String username, RealmModel realm);
    UserModel getUserByEmail(String email, RealmModel realm);
    UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm);
    UserModel getUserByServiceAccountClient(ClientModel client);
    List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts);

    // Service account is included for counts
    int getUsersCount(RealmModel realm);
    List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, boolean includeServiceAccounts);
    List<UserModel> searchForUser(String search, RealmModel realm);
    List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults);
    List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm);
    List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults);

    // Searching by UserModel.attribute (not property)
    List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm);

    Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm);
    FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm);

    void preRemove(RealmModel realm);

    void preRemove(RealmModel realm, UserFederationProviderModel link);

    void preRemove(RealmModel realm, RoleModel role);

    void preRemove(RealmModel realm, ClientModel client);
    void preRemove(ClientModel realm, ProtocolMapperModel protocolMapper);

    boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input);
    boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input);
    CredentialValidationOutput validCredentials(RealmModel realm, UserCredentialModel... input);

    void close();
}
