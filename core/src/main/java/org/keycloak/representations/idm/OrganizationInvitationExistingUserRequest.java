package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * Request body for inviting an existing user to an organization by user ID.
 */
public class OrganizationInvitationExistingUserRequest {

    private String id;
    private Map<String, List<String>> attributes;

    public OrganizationInvitationExistingUserRequest() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }
}
