package org.keycloak.models;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientModel {
    String getId();

    UserModel getAgent();

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
}
