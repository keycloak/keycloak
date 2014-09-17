package org.keycloak.models;

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

    boolean isRememberMe();

    int getStarted();

    int getLastSessionRefresh();

    void setLastSessionRefresh(int seconds);

    List<ClientSessionModel> getClientSessions();

}
