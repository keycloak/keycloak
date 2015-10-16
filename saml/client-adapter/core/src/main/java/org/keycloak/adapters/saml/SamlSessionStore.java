package org.keycloak.adapters.saml;

import org.keycloak.adapters.spi.AdapterSessionStore;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SamlSessionStore extends AdapterSessionStore {
    boolean isLoggedIn();
    SamlSession getAccount();
    void saveAccount(SamlSession account);
    String getRedirectUri();
    void logoutAccount();
    void logoutByPrincipal(String principal);
    void logoutBySsoId(List<String> ssoIds);

}
