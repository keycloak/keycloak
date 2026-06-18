package org.keycloak.protocol.oid4vc.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IssuedVerifiableCredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserVerifiableCredentialModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
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

    /**
     * @param session Keycloak session
     * @param user user
     * @param credentialScope credential scope
     * @return true if particular user has verifiable credential set on his account
     */
    public static boolean hasVerifiableCredential(KeycloakSession session, UserModel user, CredentialScopeModel credentialScope) {
        return session.users().getVerifiableCredentialsByUser(user.getId())
                .anyMatch(credential -> credential.getClientScopeId().equals(credentialScope.getId()));
    }

    /**
     * Check issued-credential present on the user with expected ID and expected issued-credential-id and credential-scope
     *
     * @param session kc session
     * @param user user
     * @param issuedCredentialId issued credential ID
     * @param expectedCredentialScope expected credential scope
     * @param expectedClient expected client
     * @throws IllegalStateException in case that issued-credential not present or does not match with user, client or clientScope
     */
    public static void checkIssuedVerifiableCredential(KeycloakSession session, UserModel user, String issuedCredentialId, CredentialScopeModel expectedCredentialScope, ClientModel expectedClient) {
        if (issuedCredentialId == null) {
            throw new IllegalStateException("Issued credential ID not present");
        }

        // TODO: For performance, it will be good to lookup issued-credential by ID directly
        Optional<IssuedVerifiableCredentialModel> issuedCred = session.users().getIssuedVerifiableCredentialsStreamByUser(user.getId())
                .filter(issuedCredential -> issuedCredential.getId().equals(issuedCredentialId))
                .findFirst();
        if (issuedCred.isEmpty()) {
            throw new IllegalStateException("Verifiable credential not found");
        }
        if (!expectedClient.getId().equals(issuedCred.get().getClientId())) {
            throw new IllegalStateException("Different client sent credential request than client from issued-credential");
        }

        // Resolve verifiableCredentialId to check against expected scope
        UserVerifiableCredentialModel verifiableCredential = session.users()
                .getVerifiableCredentialById(issuedCred.get().getVerifiableCredentialId());
        if (verifiableCredential == null) {
            throw new IllegalStateException("User verifiable credential not found for issued credential");
        }
        if (!expectedCredentialScope.getId().equals(verifiableCredential.getClientScopeId())) {
            throw new IllegalStateException("Different client scope than client scope from issued-credential");
        }
    }

    public static List<IssuedVerifiableCredentialModel> getIssuedVerifiableCredentialsByUserAndClient(KeycloakSession session, UserModel user, ClientModel client) {
        return session.users().getIssuedVerifiableCredentialsStreamByUser(user.getId())
                .filter(issuedCredential -> client.getId().equals(issuedCredential.getClientId()))
                .toList();
    }
}
