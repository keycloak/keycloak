package org.keycloak.representations;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginHintToken extends JsonWebToken {

    @JsonProperty("email")
    protected String email;

    @JsonProperty("preferred_username")
    protected String preferredUsername;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }
}
