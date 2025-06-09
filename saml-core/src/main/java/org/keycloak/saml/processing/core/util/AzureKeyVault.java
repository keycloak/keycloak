package org.keycloak.saml.processing.core.util;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.cryptography.models.KeyWrapAlgorithm;
import org.keycloak.saml.processing.core.constants.Constants;

import java.util.HashMap;

public class AzureKeyVault extends HSM<EncryptionAlgorithm> {

	private final String keyVaultId;

	private String clientId;
	private String clientCredentials;
	private String tenantId;

	private CryptographyClient akvClient;

	// Creates a mapping between the algorithm URIs and the key wrap algorithm
	// expected from the Azure Key Vault
	HashMap<String, KeyWrapAlgorithm> keyWrapAlgorithmMap = new HashMap<String, KeyWrapAlgorithm>() {
		{
			put(Constants.RSA_1_5, KeyWrapAlgorithm.RSA1_5);
			put(Constants.RSA_OAEP_MGF1P, KeyWrapAlgorithm.RSA_OAEP);
			put(Constants.A128KW, KeyWrapAlgorithm.A128KW);
			put(Constants.A192KW, KeyWrapAlgorithm.A192KW);
			put(Constants.A256KW, KeyWrapAlgorithm.A256KW);
		}
	};

	/**
	 * Initialises an Azure Key Vault object using client credentials.
	 *
	 * @param clientId          The Azure Key Vault client ID.
	 * @param clientCredentials The Azure Key Vault client credentials.
	 * @param tenantId          The Azure Key Vault tenant ID.
	 * @param keyVaultId        The Azure Key Vault ID.
	 */
	public AzureKeyVault(String clientId, String clientCredentials, String tenantId, String keyVaultId) {
		this(keyVaultId);

		this.clientId = clientId;
		this.clientCredentials = clientCredentials;
		this.tenantId = tenantId;
	}

	/**
	 * Initialises an Azure Key Vault object using managed identity.
	 *
	 * @param keyVaultId The Azure Key Vault ID.
	 */
	public AzureKeyVault(String keyVaultId) {
		super();

		this.keyVaultId = keyVaultId;
	}

	/**
	 * Creates a mapping between the algorithm URIs and the encryption algorithm
	 * expected from the Azure Key Vault.
	 *
	 * @return a mapping for which encryption algorithm to use.
	 */
	@Override
	protected HashMap<String, EncryptionAlgorithm> createEncryptionAlgorithmMap() {
		HashMap<String, EncryptionAlgorithm> map = new HashMap<>();

		map.put(Constants.RSA_1_5, EncryptionAlgorithm.RSA1_5);
		map.put(Constants.RSA_OAEP_MGF1P, EncryptionAlgorithm.RSA_OAEP);
		map.put(Constants.A128KW, EncryptionAlgorithm.A128KW);
		map.put(Constants.A192KW, EncryptionAlgorithm.A192KW);
		map.put(Constants.A256KW, EncryptionAlgorithm.A256KW);

		return map;
	}

	/**
	 * Retrieves the key wrap algorithm to use from the Azure Key Vault when
	 * performing cryptographic functions.
	 *
	 * @param algorithmUri The algorithm URI.
	 *
	 * @return The key wrap algorithm to use from the Azure Key Vault.
	 */
	protected KeyWrapAlgorithm getKeyWrapAlgorithm(String algorithmUri) {
		KeyWrapAlgorithm algorithm = keyWrapAlgorithmMap.get((algorithmUri));

		if (algorithm == null)
			throw new RuntimeException(
					"Cannot find which key wrap algorithm to use within the HSM for the algorithm URI "
					+ algorithmUri
			);

		return algorithm;
	}

	/**
	 * Sets the Azure Key Vault client.
	 */
	@Override
	public void setClient() {
		TokenCredential credential;

		if (clientId == null)
			credential = new ManagedIdentityCredentialBuilder().build();
		else
			credential = new ClientSecretCredentialBuilder()
					.clientId(clientId)
					.clientSecret(clientCredentials)
					.tenantId(tenantId)
					.build();

		HttpClient httpClient = new NettyAsyncHttpClientBuilder().build();

		this.akvClient = new CryptographyClientBuilder()
				.httpClient(httpClient)
				.credential(credential)
				.keyIdentifier(keyVaultId)
				.buildClient();

	}

	/**
	 * Wraps a key with a particular algorithm using the Azure Key Vault.
	 *
	 * @param algorithm The algorithm URI to use to wrap the key.
	 * @param key       The key to wrap.
	 *
	 * @return A wrapped key.
	 */
	@Override
	public byte[] wrapKey(String algorithm, byte[] key) {
		return this.akvClient.wrapKey(getKeyWrapAlgorithm(algorithm), key).getEncryptedKey();
	}

	/**
	 * Unwraps a key with a particular algorithm using the Azure Key Vault.
	 *
	 * @param algorithm  The algorithm URI to use to unwrap the key.
	 * @param wrappedKey The key to unwrap.
	 *
	 * @return An unwrapped key.
	 */
	@Override
	public byte[] unwrapKey(String algorithm, byte[] wrappedKey) {
		return this.akvClient.unwrapKey(getKeyWrapAlgorithm(algorithm), wrappedKey).getKey();
	}

	/**
	 * Encrypts an array of bytes with a particular algorithm using the Azure Key
	 * Vault.
	 *
	 * @param algorithm The algorithm URI to use for encryption.
	 * @param plainText The array of bytes to encrypt.
	 *
	 * @return An encrypted array of bytes.
	 */
	@Override
	public byte[] encrypt(String algorithm, byte[] plainText) {
		return this.akvClient.encrypt(getEncryptionAlgorithm(algorithm), plainText).getCipherText();
	}

	/**
	 * Decrypts an array of bytes with a particular algorithm using the Azure Key
	 * Vault.
	 *
	 * @param algorithm  The algorithm URI to use for decryption.
	 * @param cipherText The encrypted array of bytes.
	 *
	 * @return A decrypted array of bytes.
	 */
	@Override
	public byte[] decrypt(String algorithm, byte[] cipherText) {
		return this.akvClient.decrypt(getEncryptionAlgorithm(algorithm), cipherText).getPlainText();
	}
}
