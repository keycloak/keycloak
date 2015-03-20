package org.keycloak.models.jpa.entities;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.Map;

/**
 * @author Pedro Igor
 */
@Entity
@Table(name="IDENTITY_PROVIDER")
@NamedQueries({
        @NamedQuery(name="findIdentityProviderByAlias", query="select identityProvider from IdentityProviderEntity identityProvider where identityProvider.alias = :alias")
})
public class IdentityProviderEntity {

    @Id
    @Column(name="INTERNAL_ID", length = 36)
    protected String internalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Column(name="PROVIDER_ID")
    private String providerId;

    @Column(name="PROVIDER_ALIAS")
    private String alias;

    @Column(name="ENABLED")
    private boolean enabled;

    @Column(name="UPDATE_PROFILE_FIRST_LOGIN")
    private boolean updateProfileFirstLogin;

    @Column(name="STORE_TOKEN")
    private boolean storeToken;

    @Column(name="AUTHENTICATE_BY_DEFAULT")
    private boolean authenticateByDefault;

    @ElementCollection
    @MapKeyColumn(name="name")
    @Column(name="value", columnDefinition = "TEXT")
    @CollectionTable(name="IDENTITY_PROVIDER_CONFIG", joinColumns={ @JoinColumn(name="IDENTITY_PROVIDER_ID") })
    private Map<String, String> config;

    public String getInternalId() {
        return this.internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getProviderId() {
        return this.providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public RealmEntity getRealm() {
        return this.realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUpdateProfileFirstLogin() {
        return this.updateProfileFirstLogin;
    }

    public void setUpdateProfileFirstLogin(boolean updateProfileFirstLogin) {
        this.updateProfileFirstLogin = updateProfileFirstLogin;
    }

    public boolean isStoreToken() {
        return this.storeToken;
    }

    public void setStoreToken(boolean storeToken) {
        this.storeToken = storeToken;
    }

    public boolean isAuthenticateByDefault() {
        return authenticateByDefault;
    }

    public void setAuthenticateByDefault(boolean authenticateByDefault) {
        this.authenticateByDefault = authenticateByDefault;
    }

    public Map<String, String> getConfig() {
        return this.config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }


}