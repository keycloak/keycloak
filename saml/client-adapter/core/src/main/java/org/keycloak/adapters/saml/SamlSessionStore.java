package org.keycloak.adapters.saml;

import org.keycloak.adapters.spi.AdapterSessionStore;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusType;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SamlSessionStore extends AdapterSessionStore {
    public static final String CURRENT_ACTION = "SAML_CURRENT_ACTION";
    public static final String SAML_LOGIN_ERROR_STATUS = "SAML_LOGIN_ERROR_STATUS";
    public static final String SAML_LOGOUT_ERROR_STATUS = "SAML_LOGOUT_ERROR_STATUS";

    enum CurrentAction {
        NONE,
        LOGGING_IN,
        LOGGING_OUT
    }
    void setCurrentAction(CurrentAction action);
    boolean isLoggingIn();
    boolean isLoggingOut();

    boolean isLoggedIn();
    SamlSession getAccount();
    void saveAccount(SamlSession account);
    String getRedirectUri();
    void logoutAccount();
    void logoutByPrincipal(String principal);
    void logoutBySsoId(List<String> ssoIds);

}
