package org.keycloak.testsuite.util.oauth.oid4vc;

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

    public CredentialOfferUriRequest credentialOfferUriRequest() {
        return new CredentialOfferUriRequest(client);
    }

    public CredentialOfferRequest credentialOfferRequest() {
        return new CredentialOfferRequest(client);
    }

    public CredentialOfferRequest credentialOfferRequest(String nonce) {
        return new CredentialOfferRequest(nonce, client);
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
