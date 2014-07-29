package org.keycloak.models;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface UserSessionModel {

    String getId();

    void setId(String id);

    UserModel getUser();

    void setUser(UserModel user);

    String getLoginUsername();

    void setLoginUsername(String loginUsername);

    String getIpAddress();

    void setIpAddress(String ipAddress);

    String getAuthMethod();

    void setAuthMethod(String authMethod);

    boolean isRememberMe();

    void setRememberMe(boolean rememberMe);

    int getStarted();

    void setStarted(int started);

    int getLastSessionRefresh();

    void setLastSessionRefresh(int seconds);

    List<ClientSessionModel> getClientSessions();

}
