package org.keycloak.models.jpa.entities;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name="CLIENT", uniqueConstraints = {@UniqueConstraint(columnNames = {"REALM_ID", "NAME"})})
public abstract class ClientEntity {
    @Id
    @Column(name="ID", length = 36)
    private String id;
    @Column(name = "NAME")
    private String name;
    @Column(name="ENABLED")
    private boolean enabled;
    @Column(name="SECRET")
    private String secret;
    @Column(name="ALLOWED_CLAIMS_MASK")
    private long allowedClaimsMask;
    @Column(name="NOT_BEFORE")
    private int notBefore;
    @Column(name="PUBLIC_CLIENT")
    private boolean publicClient;
    @Column(name="PROTOCOL")
    private String protocol;
    @Column(name="FULL_SCOPE_ALLOWED")
    private boolean fullScopeAllowed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @ElementCollection
    @Column(name="VALUE")
    @CollectionTable(name = "WEB_ORIGINS", joinColumns={ @JoinColumn(name="CLIENT_ID") })
    protected Set<String> webOrigins = new HashSet<String>();

    @ElementCollection
    @Column(name="VALUE")
    @CollectionTable(name = "REDIRECT_URIS", joinColumns={ @JoinColumn(name="CLIENT_ID") })
    protected Set<String> redirectUris = new HashSet<String>();

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE", length = 2048)
    @CollectionTable(name="CLIENT_ATTRIBUTES", joinColumns={ @JoinColumn(name="CLIENT_ID") })
    protected Map<String, String> attributes = new HashMap<String, String>();

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAllowedClaimsMask() {
        return allowedClaimsMask;
    }

    public void setAllowedClaimsMask(long allowedClaimsMask) {
        this.allowedClaimsMask = allowedClaimsMask;
    }

    public Set<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(Set<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    public boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public void setFullScopeAllowed(boolean fullScopeAllowed) {
        this.fullScopeAllowed = fullScopeAllowed;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
