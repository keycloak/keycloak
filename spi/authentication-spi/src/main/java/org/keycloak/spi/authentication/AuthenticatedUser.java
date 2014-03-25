package org.keycloak.spi.authentication;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticatedUser {

    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;

    public AuthenticatedUser(String id, String username) {
        this.id = id;
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public AuthenticatedUser setId(String id) {
        this.id = id;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public AuthenticatedUser setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public AuthenticatedUser setName(String name) {
        int i = name.lastIndexOf(' ');
        if (i != -1) {
            firstName  = name.substring(0, i);
            lastName = name.substring(i + 1);
        } else {
            firstName = name;
        }

        return this;
    }

    public AuthenticatedUser setName(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public AuthenticatedUser setEmail(String email) {
        this.email = email;
        return this;
    }
}
