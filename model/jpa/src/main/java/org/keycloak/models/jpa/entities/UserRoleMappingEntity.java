package org.keycloak.models.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="userHasRole", query="select m from UserRoleMappingEntity m where m.user = :user and m.role = :role"),
        @NamedQuery(name="userRoleMappings", query="select m from UserRoleMappingEntity m where m.user = :user"),
        @NamedQuery(name="userRoleMappingIds", query="select m.role.id from UserRoleMappingEntity m where m.user = :user")
})
@Entity
public class UserRoleMappingEntity extends AbstractRoleMappingEntity {

}
