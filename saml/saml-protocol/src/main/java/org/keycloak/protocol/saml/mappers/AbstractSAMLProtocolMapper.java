package org.keycloak.protocol.saml.mappers;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.saml.SamlProtocol;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractSAMLProtocolMapper implements ProtocolMapper {


    @Override
    public String getProtocol() {
        return SamlProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public void close() {

    }

    @Override
    public final ProtocolMapper create(KeycloakSession session) {
        throw new RuntimeException("UNSUPPORTED METHOD");
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }
}
