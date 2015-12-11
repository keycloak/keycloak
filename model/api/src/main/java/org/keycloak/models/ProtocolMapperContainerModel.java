package org.keycloak.models;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ProtocolMapperContainerModel {
    Set<ProtocolMapperModel> getProtocolMappers();

    ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model);

    void removeProtocolMapper(ProtocolMapperModel mapping);

    void updateProtocolMapper(ProtocolMapperModel mapping);

    ProtocolMapperModel getProtocolMapperById(String id);

    ProtocolMapperModel getProtocolMapperByName(String protocol, String name);
}
