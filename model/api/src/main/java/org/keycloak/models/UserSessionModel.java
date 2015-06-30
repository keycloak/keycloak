package org.keycloak.models;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface UserSessionModel {

    String getId();

    /**
     * If created via a broker external login, this is an identifier that can be
     * used to match external broker backchannel logout requests to a UserSession
     *
     * @return
     */
    String getBrokerSessionId();
    String getBrokerUserId();

    UserModel getUser();

    String getLoginUsername();

    String getIpAddress();

    String getAuthMethod();

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
