package org.keycloak.examples.authn.delegate.federation;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthenticatedUserInfo {
    @JsonProperty("username")
    protected String username;

    // To accommodate any kind of user info defined in external authentication server,
    // all fields of user info are put into attributes except username as unique key for user info.
    
    @JsonProperty("attributes")
    protected Map<String, List<String>> attributes;
    
    public String getUserName() {
        return this.username;
    }
    
    public void setUserName(String username) {
        this.username = username;
    }
    
    public Map<String, List<String>> getAttributes() {
        return this.attributes;
    }
    
    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }      
 
}