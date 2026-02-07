package org.keycloak.testsuite.util.oauth.oid4vc;

import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.AccessTokenRequest;
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
        return new Oid4vcCredentialRequest(client);
    }

    public AccessTokenRequest preAuthAccessTokenRequest(String preAuthorizedCode) {
        return new AccessTokenRequest(client, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE, preAuthorizedCode);
    }

    public AccessTokenResponse doPreAuthAccessTokenRequest(String preAuthorizedCode) {
        return preAuthAccessTokenRequest(preAuthorizedCode).send();
    }

    public Oid4vcNonceRequest nonceRequest() {
        return new Oid4vcNonceRequest(client);
    }
}
