package org.keycloak.saml.processing.core.util;

import java.util.HashMap;

public abstract class HSM<T> {

    protected HashMap<String, T> encryptionAlgorithmMap;

    public HSM() {
        this.encryptionAlgorithmMap = createEncryptionAlgorithmMap();
    }

    /**
     * Creates a mapping between the algorithm URIs and the encryption algorithm
     * expected from the HSM.
     *
     * @return a mapping for which encryption algorithm to use.
     */
    protected abstract HashMap<String, T> createEncryptionAlgorithmMap();

    /**
     * Retrieves the encryption algorithm to use from the HSM when performing
     * cryptographic functions.
     *
     * @param algorithmUri The algorithm URI.
     *
     * @return The encryption algorithm to use from the HSM.
     */
    protected T getEncryptionAlgorithm(String algorithmUri) {
        T algorithm = encryptionAlgorithmMap.get((algorithmUri));

        if (algorithm == null)
            throw new RuntimeException(
                    "Cannot find which encryption algorithm to use within the HSM for the algorithm URI "
                            + algorithmUri
            );

        return algorithm;
    }

    /**
     * Sets the HSM client.
     */
    public abstract void setClient();

    /**
     * Wraps a key with a particular algorithm using the HSM.
     *
     * @param algorithm The algorithm URI to wrap the key.
     * @param key       The key to wrap
     *
     * @return A wrapped key.
     */
    public abstract byte[] wrapKey(String algorithm, byte[] key);

    /**
     * Unwraps a key with a particular algorithm using the HSM.
     *
     * @param algorithm  The algorithm URI to unwrap the key.
     * @param wrappedKey The key to unwrap
     *
     * @return An unwrapped key.
     */
    public abstract byte[] unwrapKey(String algorithm, byte[] wrappedKey);

    /**
     * Encrypts an array of bytes with a particular algorithm using the HSM.
     *
     * @param algorithm The algorithm URI to use for encryption.
     * @param plainText The array of bytes to encrypt.
     *
     * @return An encrypted array of bytes.
     */
    public abstract byte[] encrypt(String algorithm, byte[] plainText);

    /**
     * Decrypts an array of bytes with a particular algorithm using the HSM.
     *
     * @param algorithm  The algorithm URI to use for decryption.
     * @param cipherText The encrypted array of bytes.
     *
     * @return A decrypted array of bytes.
     */
    public abstract byte[] decrypt(String algorithm, byte[] cipherText);
}
