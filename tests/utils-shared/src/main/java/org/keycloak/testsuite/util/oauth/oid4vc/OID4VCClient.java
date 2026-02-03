package org.keycloak.testsuite.util.oauth.oid4vc;

import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.AuthorizationRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationRequestRequest;

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

    public AuthorizationRequestRequest authorizationRequest(AuthorizationRequest authRequest) {
        return new AuthorizationRequestRequest(client, authRequest);
    }

    public CredentialOfferUriRequest credentialOfferUriRequest(String credConfigId) {
        return new CredentialOfferUriRequest(client, credConfigId);
    }

    public Oid4vcCredentialRequest credentialRequest() {
        return new Oid4vcCredentialRequest(client);
    }

    public PreAuthorizedCodeGrantRequest preAuthorizedCodeGrantRequest(String preAuthorizedCode) {
        return new PreAuthorizedCodeGrantRequest(preAuthorizedCode, client);
    }

    public AccessTokenResponse doPreAuthorizedCodeGrant(String preAuthorizedCode) {
        return preAuthorizedCodeGrantRequest(preAuthorizedCode).send();
    }

    public CredentialOfferRequest credentialOfferRequest(CredentialOfferURI credOfferUri) {
        return new CredentialOfferRequest(client, credOfferUri);
    }

    public CredentialOfferRequest credentialOfferRequest(String credOfferUrl) {
        return new CredentialOfferRequest(client, credOfferUrl);
    }

    public Oid4vcNonceRequest nonceRequest() {
        return new Oid4vcNonceRequest(client);
    }

    public String doNonceRequest() {
        return nonceRequest().send().getNonce();
    }

}
