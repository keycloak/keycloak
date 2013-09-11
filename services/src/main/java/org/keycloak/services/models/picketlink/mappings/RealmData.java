package org.keycloak.services.models.picketlink.mappings;

import org.picketlink.idm.model.AbstractPartition;
import org.picketlink.idm.model.annotation.AttributeProperty;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmData extends AbstractPartition {
    private String realmName;
    private boolean enabled;
    private boolean sslNotRequired;
    private boolean cookieLoginAllowed;
    private boolean registrationAllowed;
    private boolean social;
    private int tokenLifespan;
    private int accessCodeLifespan;
    private String publicKeyPem;
    private String privateKeyPem;
    private String[] defaultRoles;

    public RealmData() {
        super(null);
    }
    public RealmData(String name) {
        super(name);
    }

    @AttributeProperty
    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    @AttributeProperty
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @AttributeProperty
    public boolean isSocial() {
        return social;
    }

    public void setSocial(boolean social) {
        this.social = social;
    }

    @AttributeProperty
    public boolean isSslNotRequired() {
        return sslNotRequired;
    }

    public void setSslNotRequired(boolean sslNotRequired) {
        this.sslNotRequired = sslNotRequired;
    }

    @AttributeProperty
    public boolean isCookieLoginAllowed() {
        return cookieLoginAllowed;
    }

    public void setCookieLoginAllowed(boolean cookieLoginAllowed) {
        this.cookieLoginAllowed = cookieLoginAllowed;
    }

    @AttributeProperty
    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    @AttributeProperty
    public int getTokenLifespan() {
        return tokenLifespan;
    }

    public void setTokenLifespan(int tokenLifespan) {
        this.tokenLifespan = tokenLifespan;
    }

    @AttributeProperty
    public int getAccessCodeLifespan() {
        return accessCodeLifespan;
    }

    public void setAccessCodeLifespan(int accessCodeLifespan) {
        this.accessCodeLifespan = accessCodeLifespan;
    }

    @AttributeProperty
    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    @AttributeProperty
    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    @AttributeProperty
    public String[] getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(String[] defaultRoles) {
        this.defaultRoles = defaultRoles;
    }
}
