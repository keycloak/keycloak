package org.keycloak.models.jpa.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="hasScope", query="select m from ScopeMappingEntity m where m.client = :client and m.role = :role"),
        @NamedQuery(name="clientScopeMappings", query="select m from ScopeMappingEntity m where m.client = :client"),
        @NamedQuery(name="clientScopeMappingIds", query="select m.role.id from ScopeMappingEntity m where m.client = :client")
})
@Entity
public class ScopeMappingEntity {
    @Id
    @Column(length = 36)
    @GenericGenerator(name="keycloak_generator", strategy="org.keycloak.models.jpa.utils.JpaIdGenerator")
    @GeneratedValue(generator = "keycloak_generator")
    protected String id;
    @ManyToOne(fetch= FetchType.LAZY)
    protected ClientEntity client;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="roleId")
    protected RoleEntity role;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ClientEntity getClient() {
        return client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

}
