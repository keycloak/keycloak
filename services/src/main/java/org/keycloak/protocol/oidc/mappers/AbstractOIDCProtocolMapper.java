package org.keycloak.protocol.oidc.mappers;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractOIDCProtocolMapper implements ProtocolMapper {

    public static final String TOKEN_MAPPER_CATEGORY = "Token mapper";

    @Override
    public String getProtocol() {
        return OIDCLoginProtocol.LOGIN_PROTOCOL;
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
