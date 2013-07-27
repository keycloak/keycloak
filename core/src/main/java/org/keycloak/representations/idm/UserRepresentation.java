package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserRepresentation {

    protected String self; // link
    protected String username;
    protected boolean enabled;
    protected Map<String, String> attributes;
    protected List<CredentialRepresentation> credentials;

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public UserRepresentation attribute(String name, String value) {
        if (this.attributes == null) attributes = new HashMap<String, String>();
        attributes.put(name, value);
        return this;
    }

    public List<CredentialRepresentation> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<CredentialRepresentation> credentials) {
        this.credentials = credentials;
    }

    public UserRepresentation credential(String type, String value, boolean hashed) {
        if (this.credentials == null) credentials = new ArrayList<CredentialRepresentation>();
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(type);
        cred.setValue(value);
        cred.setHashed(hashed);
        credentials.add(cred);
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
