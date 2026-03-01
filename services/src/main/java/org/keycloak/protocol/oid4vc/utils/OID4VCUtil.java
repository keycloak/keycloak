package org.keycloak.protocol.oid4vc.utils;

import java.util.List;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import static org.keycloak.constants.OID4VCIConstants.OID4VC_PROTOCOL;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CONFIGURATION_ID;

public class OID4VCUtil {

    private static final Logger logger = Logger.getLogger(OID4VCUtil.class);

    private OID4VCUtil() {
    }

    /**
     * Find OID4VCI client scope by credential config ID
     *
     * @param session Keycloak session
     * @param realmModel realm
     * @param credentialConfigId credential configuration ID
     * @return Found OID4VCI client scope
     */
    public static ClientScopeModel getClientScopeByCredentialConfigId(KeycloakSession session, RealmModel realmModel, String credentialConfigId) {
        if (StringUtil.isBlank(credentialConfigId)) {
            return null;
        }

        List<ClientScopeModel> clientScopes = session.clientScopes()
                .getClientScopesByProtocol(realmModel, OID4VC_PROTOCOL)
                .filter(it -> credentialConfigId.equals(it.getAttribute(VC_CONFIGURATION_ID)))
                .toList();
        if (clientScopes.size() > 1) {
            List<String> clientScopeNames = clientScopes.stream()
                    .map(ClientScopeModel::getName)
                    .toList();
            logger.warnf("Multiple client scopes find with credential config ID '%s' in the realm '%s'. Please make sure that credential-config-id is unique across client scopes. Found client scopes: %s",
                    credentialConfigId, realmModel.getName(), clientScopeNames);
            return null;
        } else if (clientScopes.isEmpty()) {
            logger.warnf("No client scopes find with credential config ID '%s' in the realm '%s'",
                    credentialConfigId, realmModel.getName());
            return null;
        } else {
            return clientScopes.get(0);
        }
    }
}
