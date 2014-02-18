package org.keycloak.models.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getAppRoleByName", query="select role from ApplicationRoleEntity role where role.name = :name and role.application = :application")
})
@Entity
public class ApplicationRoleEntity extends RoleEntity {
    @ManyToOne
    @JoinTable(name = "ApplicationRole")
    private ApplicationEntity application;

    public ApplicationEntity getApplication() {
        return application;
    }

    public void setApplication(ApplicationEntity application) {
        this.application = application;
    }
}
