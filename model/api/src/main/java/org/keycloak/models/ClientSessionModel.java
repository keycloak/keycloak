package org.keycloak.models;

import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ClientSessionModel {

    public String getId();

    public ClientModel getClient();

    public String getState();

    public UserSessionModel getUserSession();

    public String getRedirectUri();

    public int getTimestamp();

    public void setTimestamp(int timestamp);

    public Action getAction();

    public void setAction(Action action);

    public Set<String> getRoles();

    /**
     * Authentication request type, i.e. OAUTH, SAML 2.0, SAML 1.1, etc.
     *
     * @return
     */
    public String getAuthMethod();
    public void setAuthMethod(String method);

    public String getNote(String name);
    public void setNote(String name, String value);
    public void removeNote(String name);

    public static enum Action {
        OAUTH_GRANT,
        CODE_TO_TOKEN,
        VERIFY_EMAIL,
        UPDATE_PROFILE,
        CONFIGURE_TOTP,
        UPDATE_PASSWORD
    }

}
