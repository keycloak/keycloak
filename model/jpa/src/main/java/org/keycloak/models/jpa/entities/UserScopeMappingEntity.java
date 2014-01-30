package org.keycloak.models.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="userHasScope", query="select m from UserScopeMappingEntity m where m.user = :user and m.role = :role"),
        @NamedQuery(name="userScopeMappings", query="select m from UserScopeMappingEntity m where m.user = :user")
})
@Entity
public class UserScopeMappingEntity extends AbstractRoleMappingEntity {

}
