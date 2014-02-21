package org.keycloak.social;

public class SocialUser {
    
    private String id;
    private String firstName;
    private String lastName;
    private String email;

    public SocialUser(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setName(String name) {
        int i = name.lastIndexOf(' ');
        if (i != -1) {
            firstName  = name.substring(0, i);
            lastName = name.substring(i + 1);
        } else {
            firstName = name;
        }
    }

    public void setName(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
