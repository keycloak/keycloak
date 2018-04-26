package org.keycloak.performance.dataset.idm.authorization;

import org.keycloak.performance.dataset.NestedEntity;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class RolePolicyRoleDefinition extends NestedEntity<RolePolicy, RolePolicyRepresentation.RoleDefinition> {

    public RolePolicyRoleDefinition(RolePolicy parentEntity, int index, RolePolicyRepresentation.RoleDefinition representation) {
        super(parentEntity, index);
        setRepresentation(representation);
    }

    @Override
    public RolePolicyRepresentation.RoleDefinition newRepresentation() {
        return new RolePolicyRepresentation.RoleDefinition();
    }

}
