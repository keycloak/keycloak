package org.keycloak.representations.idm.oid4vc;

import java.io.IOException;

import org.keycloak.common.util.Base64Url;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CredentialOfferActionConfig {

    public static final String CREDENTIAL_CONFIGURATION_ID = "credential_configuration_id";
    public static final String CLIENT_ID = "client_id";
    public static final String PRE_AUTHORIZED = "pre_authorized";

    /**
     * Credential configuration ID of the corresponding OID4VCI credential. This is the same as the attribute "Credential configuration ID"
     * of the corresponding OID4VCI client scope
     */
    @JsonProperty(CREDENTIAL_CONFIGURATION_ID)
    private String credentialConfigurationId;

    /**
     * Client ID (UUID) of the client, which this client is targeted to. If not filled (which is most common case), the credential offer can be used by any
     * client/wallet
     */
    @JsonProperty(CLIENT_ID)
    private String clientId;

    /**
     * If true, then the credential offer is target for OID4VCI pre-authorized code grant. If false (default), then it is target for OID4VCI authorization_code grant
     */
    @JsonProperty(PRE_AUTHORIZED)
    private Boolean preAuthorized;

    public String getCredentialConfigurationId() {
        return credentialConfigurationId;
    }

    public void setCredentialConfigurationId(String credentialConfigurationId) {
        this.credentialConfigurationId = credentialConfigurationId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Boolean getPreAuthorized() {
        return preAuthorized;
    }

    public void setPreAuthorized(Boolean preAuthorized) {
        this.preAuthorized = preAuthorized;
    }

    @Override
    public String toString() {
        return "CredentialOfferActionConfig{" +
                "credentialConfigurationId='" + credentialConfigurationId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", preAuthorized='" + preAuthorized + '\'' +
                '}';
    }

    // Encode to the string, which can be used as parameter of AIA
    public String asEncodedParameter() throws IOException {
        byte[] bytes = JsonSerialization.writeValueAsBytes(this);
        return Base64Url.encode(bytes);
    }

    // Encode to the string, which can be used as parameter of AIA
    public static CredentialOfferActionConfig decodeConfig(String configStr) throws IOException {
        byte[] bytes = Base64Url.decode(configStr);
        return JsonSerialization.readValue(bytes, CredentialOfferActionConfig.class);
    }
}
