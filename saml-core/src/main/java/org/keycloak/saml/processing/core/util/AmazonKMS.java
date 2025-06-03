package org.keycloak.saml.processing.core.util;

import org.keycloak.saml.processing.core.constants.Constants;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptionAlgorithmSpec;

import java.util.HashMap;

public class AmazonKMS extends HSM<EncryptionAlgorithmSpec> {

    private final String keyId;
    private KmsClient kmsClient;

    /**
     * Initialises an Amazon KMS object.
     *
     * @param keyId the key ID to use when performing cryptographic functions.
     */
    public AmazonKMS(String keyId) {
        super();

        this.keyId = keyId;
    }

    /**
     * Creates a mapping between the algorithm URIs and the encryption algorithm
     * expected from the Amazon KMS.
     *
     * @return a mapping for which encryption algorithm to use.
     */
    @Override
    protected HashMap<String, EncryptionAlgorithmSpec> createEncryptionAlgorithmMap() {
        HashMap<String, EncryptionAlgorithmSpec> map = new HashMap<>();

        map.put(Constants.RSA_1_5, EncryptionAlgorithmSpec.RSAES_OAEP_SHA_1);
        map.put(Constants.RSA_OAEP_MGF1P, EncryptionAlgorithmSpec.RSAES_OAEP_SHA_1);
        map.put(Constants.A128KW, EncryptionAlgorithmSpec.SYMMETRIC_DEFAULT);
        map.put(Constants.A192KW, EncryptionAlgorithmSpec.SYMMETRIC_DEFAULT);
        map.put(Constants.A256KW, EncryptionAlgorithmSpec.SYMMETRIC_DEFAULT);
        map.put(Constants.AES128_CBC, EncryptionAlgorithmSpec.SYMMETRIC_DEFAULT);
        map.put(Constants.AES192_CBC, EncryptionAlgorithmSpec.SYMMETRIC_DEFAULT);
        map.put(Constants.AES256_CBC, EncryptionAlgorithmSpec.SYMMETRIC_DEFAULT);

        return map;
    }

    /**
     * Sets the Amazon KMS client.
     */
    @Override
    public void setClient() {
        this.kmsClient = KmsClient.create();
    }

    /**
     * Wraps a key with a particular algorithm using the Amazon KMS.
     *
     * @param algorithm The algorithm URI to wrap the key.
     * @param key       The key to wrap
     *
     * @return A wrapped key.
     */
    @Override
    public byte[] wrapKey(String algorithm, byte[] key) {
        return this.encrypt(algorithm, key);
    }

    /**
     * Unwraps a key with a particular algorithm using the Amazon KMS.
     *
     * @param algorithm  The algorithm URI to unwrap the key.
     * @param wrappedKey The key to unwrap
     *
     * @return An unwrapped key.
     */
    @Override
    public byte[] unwrapKey(String algorithm, byte[] wrappedKey) {
        return this.decrypt(algorithm, wrappedKey);
    }

    /**
     * Encrypts an array of bytes with a particular algorithm using the Amazon KMS.
     *
     * @param algorithm The algorithm URI to use for encryption.
     * @param plainText The array of bytes to encrypt.
     *
     * @return An encrypted array of bytes.
     */
    @Override
    public byte[] encrypt(String algorithm, byte[] plainText) {
        EncryptRequest encryptRequest = EncryptRequest.builder()
                .keyId(this.keyId)
                .plaintext(SdkBytes.fromByteArray(plainText))
                .encryptionAlgorithm(getEncryptionAlgorithm(algorithm))
                .build();

        return this.kmsClient.encrypt(encryptRequest).ciphertextBlob().asByteArray();
    }

    /**
     * Decrypts an array of bytes with a particular algorithm using the Amazon KMS.
     *
     * @param algorithm  The algorithm URI to use for decryption.
     * @param cipherText The encrypted array of bytes.
     *
     * @return A decrypted array of bytes.
     */
    @Override
    public byte[] decrypt(String algorithm, byte[] cipherText) {
        DecryptRequest decryptRequest = DecryptRequest.builder()
                .keyId(this.keyId)
                .ciphertextBlob(SdkBytes.fromByteArray(cipherText))
                .encryptionAlgorithm(getEncryptionAlgorithm(algorithm))
                .build();

        return this.kmsClient.decrypt(decryptRequest).plaintext().asByteArray();
    }
}
