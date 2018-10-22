package org.keycloak.performance.dataset.idm.authorization;

import java.util.Collection;
import java.util.HashSet;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class RolePolicyRoleDefinitionSet extends HashSet<RolePolicyRepresentation.RoleDefinition> {

    public RolePolicyRoleDefinitionSet(Collection<RolePolicyRoleDefinition> roleDefinitions) {
        roleDefinitions.forEach(rd -> add(
                new RolePolicyRepresentation.RoleDefinition(
                        rd.getRepresentation().getId(),
                        rd.getRepresentation().isRequired()
                )));
    }

}
