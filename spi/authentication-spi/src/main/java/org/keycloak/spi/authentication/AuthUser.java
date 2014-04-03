package org.keycloak.spi.authentication;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthUser {

    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;

    private String providerName;

    public AuthUser(String id, String username, String providerName) {
        this.id = id;
        this.username = username;
        this.providerName = providerName;
    }

    public String getId() {
        return id;
    }

    public AuthUser setId(String id) {
        this.id = id;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public AuthUser setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public AuthUser setName(String name) {
        int i = name.lastIndexOf(' ');
        if (i != -1) {
            firstName  = name.substring(0, i);
            lastName = name.substring(i + 1);
        } else {
            firstName = name;
        }

        return this;
    }

    public AuthUser setName(String firstName, String lastName) {
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

    public AuthUser setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getProviderName() {
        return providerName;
    }

    public AuthUser setProviderName(String providerName) {
        this.providerName = providerName;
        return this;
    }
}
