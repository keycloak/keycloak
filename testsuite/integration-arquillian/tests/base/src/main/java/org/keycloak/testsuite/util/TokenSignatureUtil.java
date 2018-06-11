package org.keycloak.testsuite.util;

import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public class TokenSignatureUtil {
    private static Logger log = Logger.getLogger(TokenSignatureUtil.class);

    private static final String COMPONENT_SIGNATURE_ALGORITHM_KEY = "token.signed.response.alg";

    public static void changeRealmTokenSignatureProvider(Keycloak adminClient, String toSigAlgName) {
        RealmRepresentation rep = adminClient.realm("test").toRepresentation();
        Map<String, String> attributes = rep.getAttributes();
        log.tracef("change realm test signature algorithm from %s to %s", attributes.get(COMPONENT_SIGNATURE_ALGORITHM_KEY), toSigAlgName);
        attributes.put(COMPONENT_SIGNATURE_ALGORITHM_KEY, toSigAlgName);
        rep.setAttributes(attributes);
        adminClient.realm("test").update(rep);
    }

    public static void changeClientTokenSignatureProvider(ClientResource clientResource, Keycloak adminClient, String toSigAlgName) {
        ClientRepresentation clientRep = clientResource.toRepresentation();
        log.tracef("change client %s signature algorithm from %s to %s", clientRep.getClientId(), OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).getIdTokenSignedResponseAlg(), toSigAlgName);
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setIdTokenSignedResponseAlg(toSigAlgName);
        clientResource.update(clientRep);
    }
}
