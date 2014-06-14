package org.keycloak.models.jpa.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"realm", "name"})})
public abstract class ClientEntity {
    @Id
    private String id;
    @Column(name = "name")
    private String name;
    private boolean enabled;
    private String secret;
    private long allowedClaimsMask;
    private int notBefore;
    private boolean publicClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realm")
    protected RealmEntity realm;

    @ElementCollection
    @CollectionTable
    protected Set<String> webOrigins = new HashSet<String>();
    @ElementCollection
    @CollectionTable
    protected Set<String> redirectUris = new HashSet<String>();

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
}
