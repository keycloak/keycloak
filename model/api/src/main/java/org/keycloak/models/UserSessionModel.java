package org.keycloak.models;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface UserSessionModel {

    String getId();

    void setId(String id);

    UserModel getUser();

    void setUser(UserModel user);

    String getIpAddress();

    void setIpAddress(String ipAddress);

    int getStarted();

    void setStarted(int started);

    int getExpires();

    void setExpires(int expires);

}
