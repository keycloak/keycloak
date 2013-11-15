package org.keycloak.models.jpa.entities;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
public class ApplicationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    private String name;
    private boolean enabled;
    private boolean surrogateAuthRequired;
    private String managementUrl;

    @OneToOne(fetch = FetchType.EAGER)
    private UserEntity applicationUser;

    @OneToMany(fetch = FetchType.EAGER, cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name="APPLICATION_ROLES")
    Collection<RoleEntity> roles = new ArrayList<RoleEntity>();

    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name="APPLICATION_DEFAULT_ROLES")
    Collection<RoleEntity> defaultRoles = new ArrayList<RoleEntity>();

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    public String getManagementUrl() {
        return managementUrl;
    }

    public void setManagementUrl(String managementUrl) {
        this.managementUrl = managementUrl;
    }

    public UserEntity getApplicationUser() {
        return applicationUser;
    }

    public void setApplicationUser(UserEntity applicationUser) {
        this.applicationUser = applicationUser;
    }

    public Collection<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Collection<RoleEntity> roles) {
        this.roles = roles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<RoleEntity> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(Collection<RoleEntity> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }
}
