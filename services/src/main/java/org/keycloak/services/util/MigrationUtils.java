package org.keycloak.services.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.ClaimMask;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.provider.ProviderFactory;

/**
 * Various common utils needed for migration from older version to newer
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrationUtils {

    private MigrationUtils() {}

    /**
     *
     * @param session
     * @param claimMask mask used on ClientModel in 1.1.0
     * @return set of 1.2.0.Beta1 protocol mappers corresponding to given claimMask
     */
    public static Collection<ProtocolMapperModel> getMappersForClaimMask(KeycloakSession session, Long claimMask) {
        Map<String, ProtocolMapperModel> allMappers = getAllDefaultMappers(session);

        if (claimMask == null) {
            return allMappers.values();
        }

        if (!ClaimMask.hasUsername(claimMask)) {
            allMappers.remove(OIDCLoginProtocolFactory.USERNAME);
        }
        if (!ClaimMask.hasEmail(claimMask)) {
            allMappers.remove(OIDCLoginProtocolFactory.EMAIL);
        }
        if (!ClaimMask.hasName(claimMask)) {
            allMappers.remove(OIDCLoginProtocolFactory.FAMILY_NAME);
            allMappers.remove(OIDCLoginProtocolFactory.FULL_NAME);
            allMappers.remove(OIDCLoginProtocolFactory.GIVEN_NAME);
        }

        return allMappers.values();
    }

    private static Map<String, ProtocolMapperModel> getAllDefaultMappers(KeycloakSession session) {
        Map<String, ProtocolMapperModel> allMappers = new HashMap<String, ProtocolMapperModel>();

        List<ProviderFactory> loginProtocolFactories = session.getKeycloakSessionFactory().getProviderFactories(LoginProtocol.class);

        for (ProviderFactory factory : loginProtocolFactories) {
            LoginProtocolFactory loginProtocolFactory = (LoginProtocolFactory) factory;
            List<ProtocolMapperModel> currentMappers = loginProtocolFactory.getDefaultBuiltinMappers();

            for (ProtocolMapperModel protocolMapper : currentMappers) {
                allMappers.put(protocolMapper.getName(), protocolMapper);
            }
        }

        return allMappers;
    }
}
