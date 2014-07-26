package org.keycloak.models.sessions;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface LoginFailure {

    String getUsername();

    int getFailedLoginNotBefore();

    void setFailedLoginNotBefore(int notBefore);

    int getNumFailures();

    void incrementFailures();

    void clearFailures();

    long getLastFailure();

    void setLastFailure(long lastFailure);

    String getLastIPFailure();

    void setLastIPFailure(String ip);

}
