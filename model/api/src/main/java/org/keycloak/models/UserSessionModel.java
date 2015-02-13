package org.keycloak.models;

import org.keycloak.util.MultivaluedHashMap;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface UserSessionModel {

    String getId();

    UserModel getUser();

    String getLoginUsername();

    String getIpAddress();

    String getAuthMethod();

    public MultivaluedHashMap<String, String> getClaims();

    boolean isRememberMe();

    int getStarted();

    int getLastSessionRefresh();

    void setLastSessionRefresh(int seconds);

    List<ClientSessionModel> getClientSessions();

    public String getNote(String name);
    public void setNote(String name, String value);
    public void removeNote(String name);

    State getState();
    void setState(State state);

    public static enum State {
        LOGGING_IN,
        LOGGED_IN,
        LOGGING_OUT,
        LOGGED_OUT
    }

}
