package org.keycloak.protocol.oid4vc.issuance.signing;

import org.keycloak.crypto.Algorithm;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Properties for configuring the {@link VerifiableCredentialsSigningService}'s
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public enum SigningProperties {

    ISSUER_DID("issuerDid", "Did of the issuer.", "Provide the DID of the issuer. Needs to match the provided key material.", ProviderConfigProperty.STRING_TYPE, null),
    KEY_ID("keyId", "Id of the signing key.", "The id of the key to be used for signing credentials. The key needs to be provided as a realm key.", ProviderConfigProperty.STRING_TYPE, null),
    PROOF_TYPE("proofType", "Type of the LD-Proof.", "The type of LD-Proofs to be created. Needs to fit the provided signing key.", ProviderConfigProperty.STRING_TYPE, null),
    ALGORITHM_TYPE("algorithmType", "Type of the signing algorithm.", "The type of the algorithm to be used for signing. Needs to fit the provided signing key.", ProviderConfigProperty.STRING_TYPE, Algorithm.RS256),
    DECOYS("decoys", "Number of decoys to be added.", "The number of decoys to be added to the SD-JWT.", ProviderConfigProperty.STRING_TYPE, 0);

    private final String key;
    private final String label;
    private final String helpText;
    private final String type;
    private final Object defaultValue;

    SigningProperties(String key, String label, String helpText, String type, Object defaultValue) {
        this.key = key;
        this.label = label;
        this.helpText = helpText;
        this.type = type;
        this.defaultValue = defaultValue;
    }


    public ProviderConfigProperty asConfigProperty() {
        return new ProviderConfigProperty(key, label, helpText, type, defaultValue);
    }

    public String getKey() {
        return key;
    }
}
