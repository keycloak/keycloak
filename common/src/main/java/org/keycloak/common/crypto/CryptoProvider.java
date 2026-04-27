package org.keycloak.common.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Signature;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.spec.ECParameterSpec;
import java.util.stream.Stream;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.net.ssl.SSLSocketFactory;

import org.keycloak.common.util.KeystoreUtil.KeystoreFormat;

/**
 * Abstraction to handle differences between the APIs for non-fips and fips mode
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface CryptoProvider {

    /**
     * @return BouncyCastle security provider. Can be either non-FIPS or FIPS based provider
     */
    Provider getBouncyCastleProvider();

    /**
     * Order of this provider. This allows to specify which CryptoProvider will have preference in case that more of them are on the classpath.
     *
     * The higher number has preference over the lower number
     */
    int order();

    /**
     * Get some algorithm provider implementation. Returned implementation can be dependent according to if we have
     * non-fips bouncycastle or fips bouncycastle on the classpath.
     *
     * @param clazz Returned class.
     * @param algorithm Type of the algorithm, which we want to return
     * @return
     */
    <T> T getAlgorithmProvider(Class<T> clazz, String algorithm);

    /**
     * Get CertificateUtils implementation. Returned implementation can be dependent according to if we have
     * non-fips bouncycastle or fips bouncycastle on the classpath.
     *
     * @return
     */
    CertificateUtilsProvider getCertificateUtils();


    /**
     * Get PEMUtils implementation. Returned implementation can be dependent according to if we have
     * non-fips bouncycastle or fips bouncycastle on the classpath.
     *
     * @return
     */
    PemUtilsProvider getPemUtils();

    <T> T getOCSPProver(Class<T> clazz);


    public UserIdentityExtractorProvider getIdentityExtractorProvider();

    public ECDSACryptoProvider getEcdsaCryptoProvider();


    /**
     * Create the param spec for the EC curve
     *
     * @param curveName
     * @return
     */
    ECParameterSpec createECParams(String curveName);

    KeyPairGenerator getKeyPairGen(String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException;

    KeyFactory getKeyFactory(String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException;

    Cipher getAesCbcCipher() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException;

    Cipher getAesGcmCipher() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException;

    SecretKeyFactory getSecretKeyFact(String keyAlgorithm) throws NoSuchAlgorithmException, NoSuchProviderException;

    KeyStore getKeyStore(KeystoreFormat format) throws KeyStoreException, NoSuchProviderException;

    /**
     * @return Keystore types/algorithms supported by this CryptoProvider
     */
    default Stream<KeystoreFormat> getSupportedKeyStoreTypes() {
        return Stream.of(KeystoreFormat.values())
                .filter(format -> {
                    try {
                        getKeyStore(format);
                        return true;
                    } catch (KeyStoreException | NoSuchProviderException ex) {
                        return false;
                    }
                });
    }

    CertificateFactory getX509CertFactory() throws CertificateException, NoSuchProviderException;

    CertStore getCertStore(CollectionCertStoreParameters collectionCertStoreParameters) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException;

    CertPathBuilder getCertPathBuilder() throws NoSuchAlgorithmException, NoSuchProviderException;

    Signature getSignature(String sigAlgName) throws NoSuchAlgorithmException, NoSuchProviderException;

    /**
     * Wrap given SSLSocketFactory and decorate it with some additional functionality.
     *
     * This method is used in the context of truststore (where Keycloak is SSL client)
     *
     * @param delegate The original factory to wrap. Usually default java SSLSocketFactory
     * @return decorated factory
     */
    SSLSocketFactory wrapFactoryForTruststore(SSLSocketFactory delegate);

    /**
     * @return Allowed key sizes of RSA key modulus, which this cryptoProvider supports
     */
    default String[] getSupportedRsaKeySizes() {
        return new String[] {"1024", "2048", "3072", "4096"};
    }
}
