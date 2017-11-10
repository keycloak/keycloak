package org.keycloak.performance;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class UserInfo {

    public final String username;
    public final String password;

    UserInfo(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
