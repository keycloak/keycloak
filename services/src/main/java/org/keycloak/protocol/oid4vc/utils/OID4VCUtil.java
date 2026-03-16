package org.keycloak.protocol.oid4vc.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;

import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.CREDENTIAL_OFFER_PATH;

public class OID4VCUtil {

    private OID4VCUtil() {
    }

    /**
     * @param session Keycloak session
     * @param nonce nonce, which is part of the credential offer URI
     * @return Credential offer as URI, which can be shared with the wallet
     */
    public static String getOfferAsUri(KeycloakSession session, String nonce) {
        String offerUri = KeycloakUriBuilder.fromUri(
                OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()) + "/protocol/{protocol}/{credentialOfferPath}/{nonce}")
                .buildAsString(OID4VCLoginProtocolFactory.PROTOCOL_ID, CREDENTIAL_OFFER_PATH, nonce);
        return "openid-credential-offer://?credential_offer_uri=" + URLEncoder.encode(offerUri, StandardCharsets.UTF_8);
    }
}
