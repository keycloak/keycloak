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

    public CredentialOfferUriRequest credentialOfferUriRequest(String credentialConfigurationId) {
        return new CredentialOfferUriRequest(client, credentialConfigurationId);
    }

    public Oid4vcCredentialRequest credentialRequest(CredentialRequest credRequest) {
        return new Oid4vcCredentialRequest(client, credRequest);
    }

    public PreAuthorizedCodeGrantRequest preAuthorizedCodeGrantRequest(String preAuthorizedCode) {
        return new PreAuthorizedCodeGrantRequest(client, preAuthorizedCode);
    }

    public AccessTokenResponse doPreAuthorizedCodeGrantRequest(String preAuthorizedCode) {
        return preAuthorizedCodeGrantRequest(preAuthorizedCode).send();
    }

    public CredentialOfferRequest credentialOfferRequest(CredentialOfferURI credOfferURI) {
        return new CredentialOfferRequest(client, credOfferURI);
    }

    public CredentialOfferRequest credentialOfferRequest(String credOfferUrl) {
        return new CredentialOfferRequest(client, credOfferUrl);
    }

    public CredentialOfferResponse doCredentialOfferRequest(CredentialOfferURI credOfferURI) {
        return credentialOfferRequest(credOfferURI).send();
    }

    public Oid4vcNonceRequest nonceRequest() {
        return new Oid4vcNonceRequest(client);
    }

    public Oid4vcNonceResponse doNonceRequest() {
        return nonceRequest().send();
    }
}
