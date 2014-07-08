package org.keycloak.models.sessions;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface Session {

    String getId();

    void setId(String id);

    String getUser();

    void setUser(String user);

    String getIpAddress();

    void setIpAddress(String ipAddress);

    int getStarted();

    void setStarted(int started);

    int getLastSessionRefresh();

    void setLastSessionRefresh(int seconds);

    void associateClient(String client);

    List<String> getClientAssociations();

    void removeAssociatedClient(String client);

}
