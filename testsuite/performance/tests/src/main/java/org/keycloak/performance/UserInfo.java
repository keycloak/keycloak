package org.keycloak.performance;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class UserInfo {

    public final String username;
    public final String password;
    public final String firstName;
    public final String lastName;
    public final String email;

    UserInfo(String username, String password, String firstName, String lastName, String email) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}
