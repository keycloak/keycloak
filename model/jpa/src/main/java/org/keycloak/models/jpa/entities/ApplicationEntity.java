package org.keycloak.models.jpa.entities;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
public class ApplicationEntity extends ClientEntity {

    @Column(name="SURROGATE_AUTH_REQUIRED")
    private boolean surrogateAuthRequired;

    @Column(name="BASE_URL")
    private String baseUrl;

    @Column(name="MANAGEMENT_URL")
    private String managementUrl;

    @Column(name="BEARER_ONLY")
    private boolean bearerOnly;

    @Column(name="NODE_REREG_TIMEOUT")
    private int nodeReRegistrationTimeout;

    @OneToMany(fetch = FetchType.EAGER, cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "application")
    Collection<RoleEntity> roles = new ArrayList<RoleEntity>();

    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name="APPLICATION_DEFAULT_ROLES", joinColumns = { @JoinColumn(name="APPLICATION_ID")}, inverseJoinColumns = { @JoinColumn(name="ROLE_ID")})
    Collection<RoleEntity> defaultRoles = new ArrayList<RoleEntity>();

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE")
    @CollectionTable(name="APP_NODE_REGISTRATIONS", joinColumns={ @JoinColumn(name="APPLICATION_ID") })
    Map<String, Integer> registeredNodes = new HashMap<String, Integer>();

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getManagementUrl() {
        return managementUrl;
    }

    public void setManagementUrl(String managementUrl) {
        this.managementUrl = managementUrl;
    }

    public Collection<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Collection<RoleEntity> roles) {
        this.roles = roles;
    }

    public Collection<RoleEntity> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(Collection<RoleEntity> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(boolean bearerOnly) {
        this.bearerOnly = bearerOnly;
    }

    public int getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    public void setNodeReRegistrationTimeout(int nodeReRegistrationTimeout) {
        this.nodeReRegistrationTimeout = nodeReRegistrationTimeout;
    }

    public Map<String, Integer> getRegisteredNodes() {
        return registeredNodes;
    }

    public void setRegisteredNodes(Map<String, Integer> registeredNodes) {
        this.registeredNodes = registeredNodes;
    }
}
