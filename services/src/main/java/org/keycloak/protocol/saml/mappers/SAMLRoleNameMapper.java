package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SAMLRoleNameMapper {
    public String mapName(ProtocolMapperModel model, RoleModel role);
}
