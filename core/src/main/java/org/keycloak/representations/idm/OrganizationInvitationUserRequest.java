package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * Request body for inviting a new or existing user to an organization by email.
 */
public class OrganizationInvitationUserRequest {

    private String email;
    private String firstName;
    private String lastName;
    private Map<String, List<String>> attributes;

    public OrganizationInvitationUserRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }
}
