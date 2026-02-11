package org.keycloak.testsuite.util.oauth.oid4vc;

import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

public class OID4VCClient {

    private final AbstractOAuthClient<?> client;

    public OID4VCClient(AbstractOAuthClient<?> client) {
        this.client = client;
    }

    public CredentialIssuerMetadataRequest issuerMetadataRequest() {
        return new CredentialIssuerMetadataRequest(client);
    }

    public CredentialIssuerMetadataResponse doIssuerMetadataRequest() {
        return issuerMetadataRequest().send();
    }

    public CredentialOfferUriRequest credentialOfferUriRequest(String credConfigId) {
        return new CredentialOfferUriRequest(client, credConfigId);
    }

    public CredentialOfferRequest credentialOfferRequest(CredentialOfferURI credOfferUri) {
        return new CredentialOfferRequest(client, credOfferUri);
    }

    public CredentialOfferRequest credentialOfferRequest(String nonce) {
        return new CredentialOfferRequest(client, nonce);
    }

    public Oid4vcCredentialRequest credentialRequest() {
        return new Oid4vcCredentialRequest(client, new CredentialRequest());
    }

    public Oid4vcCredentialRequest credentialRequest(CredentialRequest credRequest) {
        return new Oid4vcCredentialRequest(client, credRequest);
    }

    public PreAuthorizedCodeGrantRequest preAuthorizedCodeGrantRequest(String preAuthorizedCode) {
        return new PreAuthorizedCodeGrantRequest(preAuthorizedCode, client);
    }

    public AccessTokenResponse doPreAuthorizedCodeGrant(String preAuthorizedCode) {
        return preAuthorizedCodeGrantRequest(preAuthorizedCode).send();
    }

    public Oid4vcNonceRequest nonceRequest() {
        return new Oid4vcNonceRequest(client);
    }
}
