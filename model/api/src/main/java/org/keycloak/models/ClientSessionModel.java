package org.keycloak.models;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ClientSessionModel {

    public String getId();
    public RealmModel getRealm();
    public ClientModel getClient();

    public UserSessionModel getUserSession();
    public void setUserSession(UserSessionModel userSession);

    public String getRedirectUri();
    public void setRedirectUri(String uri);

    public int getTimestamp();

    public void setTimestamp(int timestamp);

    public String getAction();

    public void setAction(String action);

    public Set<String> getRoles();
    public void setRoles(Set<String> roles);

    public Set<String> getProtocolMappers();
    public void setProtocolMappers(Set<String> protocolMappers);

    public Map<String, ExecutionStatus> getExecutionStatus();
    public void setExecutionStatus(String authenticator, ExecutionStatus status);
    public void clearExecutionStatus();
    public UserModel getAuthenticatedUser();
    public void setAuthenticatedUser(UserModel user);



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
    public Map<String, String> getNotes();

    /**
     * These are notes you want applied to the UserSessionModel when the client session is attached to it.
     *
     * @param name
     * @param value
     */
    public void setUserSessionNote(String name, String value);

    /**
     * These are notes you want applied to the UserSessionModel when the client session is attached to it.
     *
     * @return
     */
    public Map<String, String> getUserSessionNotes();

    public void clearUserSessionNotes();

    public static enum Action {
        OAUTH_GRANT,
        CODE_TO_TOKEN,
        VERIFY_EMAIL,
        UPDATE_PROFILE,
        CONFIGURE_TOTP,
        UPDATE_PASSWORD,
        RECOVER_PASSWORD,
        AUTHENTICATE,
        SOCIAL_CALLBACK,
        LOGGED_OUT
    }

    public enum ExecutionStatus {
        FAILED,
        SUCCESS,
        SETUP_REQUIRED,
        ATTEMPTED,
        SKIPPED,
        CHALLENGED
    }
}
