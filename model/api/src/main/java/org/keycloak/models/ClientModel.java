package org.keycloak.models;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientModel {

    // COMMON ATTRIBUTES

    String PRIVATE_KEY = "privateKey";
    String PUBLIC_KEY = "publicKey";
    String X509CERTIFICATE = "X509Certificate";

    /**
     * Internal database key
     *
     * @return
     */
    String getId();

    /**
     * String exposed to outside world
     *
     * @return
     */
    String getClientId();

    long getAllowedClaimsMask();

    void setAllowedClaimsMask(long mask);

    Set<String> getWebOrigins();

    void setWebOrigins(Set<String> webOrigins);

    void addWebOrigin(String webOrigin);

    void removeWebOrigin(String webOrigin);

    Set<String> getRedirectUris();

    void setRedirectUris(Set<String> redirectUris);

    void addRedirectUri(String redirectUri);

    void removeRedirectUri(String redirectUri);


    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean validateSecret(String secret);
    String getSecret();
    public void setSecret(String secret);

    boolean isFullScopeAllowed();
    void setFullScopeAllowed(boolean value);

    String getProtocol();
    void setProtocol(String protocol);

    void setAttribute(String name, String value);
    void removeAttribute(String name);
    String getAttribute(String name);
    Map<String, String> getAttributes();

    boolean isFrontchannelLogout();
    void setFrontchannelLogout(boolean flag);


    boolean isPublicClient();
    void setPublicClient(boolean flag);

    boolean isDirectGrantsOnly();
    void setDirectGrantsOnly(boolean flag);

    Set<RoleModel> getScopeMappings();
    void addScopeMapping(RoleModel role);
    void deleteScopeMapping(RoleModel role);
    Set<RoleModel> getRealmScopeMappings();
    boolean hasScope(RoleModel role);


    RealmModel getRealm();

    /**
     * Time in seconds since epoc
     *
     * @return
     */
    int getNotBefore();

    void setNotBefore(int notBefore);

    void updateIdentityProviders(List<ClientIdentityProviderMappingModel> identityProviders);
    List<ClientIdentityProviderMappingModel> getIdentityProviders();
    boolean isAllowedRetrieveTokenFromIdentityProvider(String providerId);

    Set<ProtocolMapperModel> getProtocolMappers();
    ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model);
    void removeProtocolMapper(ProtocolMapperModel mapping);
    void updateProtocolMapper(ProtocolMapperModel mapping);
    public ProtocolMapperModel getProtocolMapperById(String id);
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name);
}
