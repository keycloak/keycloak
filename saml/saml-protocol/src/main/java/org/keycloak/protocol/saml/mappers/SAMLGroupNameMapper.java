package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.GroupModel;
import org.keycloak.models.ProtocolMapperModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SAMLGroupNameMapper {
    public String mapName(ProtocolMapperModel model, GroupModel group);
}
