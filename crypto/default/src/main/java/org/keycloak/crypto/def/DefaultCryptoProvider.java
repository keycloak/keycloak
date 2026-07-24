package org.keycloak.crypto.def;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.spec.ECParameterSpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.net.ssl.SSLSocketFactory;

import org.keycloak.common.crypto.CertificateUtilsProvider;
import org.keycloak.common.crypto.CryptoConstants;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.crypto.ECDSACryptoProvider;
import org.keycloak.common.crypto.PemUtilsProvider;
import org.keycloak.common.crypto.UserIdentityExtractorProvider;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.util.KeystoreUtil.KeystoreFormat;
import org.keycloak.crypto.JavaAlgorithm;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultCryptoProvider implements CryptoProvider {

    private static final Logger log = Logger.getLogger(DefaultCryptoProvider.class);

    private final Provider bcProvider;

    private Map<String, Object> providers = new ConcurrentHashMap<>();

    public DefaultCryptoProvider() {
        // Make sure to instantiate this only once due it is expensive. And skip registration if already available in Java security providers (EG. due explicitly configured in java security file)
        Provider existingBc = Security.getProvider(CryptoConstants.BC_PROVIDER_ID);
        this.bcProvider = existingBc == null ? new BouncyCastleProvider() : existingBc;

        providers.put(CryptoConstants.A128KW, new AesKeyWrapAlgorithmProvider());
        providers.put(CryptoConstants.RSA1_5, new DefaultRsaKeyEncryptionJWEAlgorithmProvider("RSA/ECB/PKCS1Padding"));
        providers.put(CryptoConstants.RSA_OAEP, new DefaultRsaKeyEncryptionJWEAlgorithmProvider("RSA/ECB/OAEPWithSHA-1AndMGF1Padding"));
        providers.put(CryptoConstants.RSA_OAEP_256, new DefaultRsaKeyEncryption256JWEAlgorithmProvider("RSA/ECB/OAEPWithSHA-256AndMGF1Padding"));
        providers.put(CryptoConstants.ECDH_ES, new BCEcdhEsAlgorithmProvider());
        providers.put(CryptoConstants.ECDH_ES_A128KW, new BCEcdhEsAlgorithmProvider());
        providers.put(CryptoConstants.ECDH_ES_A192KW, new BCEcdhEsAlgorithmProvider());
        providers.put(CryptoConstants.ECDH_ES_A256KW, new BCEcdhEsAlgorithmProvider());

        if (existingBc == null) {
            Security.addProvider(this.bcProvider);
            log.debugv("Loaded {0} security provider", this.bcProvider.getClass().getName());
        } else {
            log.debugv("Security provider {0} already loaded", this.bcProvider.getClass().getName());
        }
    }


    @Override
    public Provider getBouncyCastleProvider() {
        return bcProvider;
    }

    @Override
    public int order() {
        return 200;
    }

    @Override
    public <T> T getAlgorithmProvider(Class<T> clazz, String algorithmType) {
        Object o = providers.get(algorithmType);
        if (o == null) {
            throw new IllegalArgumentException("Not found provider of algorithm type: " + algorithmType);
        }
        return clazz.cast(o);
    }

    @Override
    public CertificateUtilsProvider getCertificateUtils() {
        return new BCCertificateUtilsProvider();
    }

    @Override
    public PemUtilsProvider getPemUtils() {
        return new BCPemUtilsProvider();
    }

    @Override
    public ECParameterSpec createECParams(String curveName) {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(curveName);
        return new ECNamedCurveSpec("prime256v1", spec.getCurve(), spec.getG(), spec.getN());
    }

    @Override
    public UserIdentityExtractorProvider getIdentityExtractorProvider() {
        return new BCUserIdentityExtractorProvider();
    }

    @Override
    public ECDSACryptoProvider getEcdsaCryptoProvider() {
        return new BCECDSACryptoProvider();
    }


    @Override
    public <T> T getOCSPProver(Class<T> clazz) {
        return clazz.cast(new BCOCSPProvider());
    }


    @Override
    public KeyPairGenerator getKeyPairGen(String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
        return KeyPairGenerator.getInstance(algorithm, BouncyIntegration.PROVIDER);
    }


    @Override
    public KeyFactory getKeyFactory(String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
        return KeyFactory.getInstance(algorithm, BouncyIntegration.PROVIDER);
    }


    @Override
    public Cipher getAesCbcCipher() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
        return Cipher.getInstance("AES/CBC/PKCS7Padding", BouncyIntegration.PROVIDER);
    }

    @Override
    public Cipher getAesGcmCipher() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
        return Cipher.getInstance("AES/GCM/NoPadding", BouncyIntegration.PROVIDER);
    }

    @Override
    public SecretKeyFactory getSecretKeyFact(String keyAlgorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
        return SecretKeyFactory.getInstance(keyAlgorithm, BouncyIntegration.PROVIDER);
    }

    @Override
    public KeyStore getKeyStore(KeystoreFormat format) throws KeyStoreException, NoSuchProviderException {
        if (format == KeystoreFormat.JKS) {
            return KeyStore.getInstance(format.toString());
        } else {
            return KeyStore.getInstance(format.toString(), BouncyIntegration.PROVIDER);
        }
    }

    @Override
    public CertificateFactory getX509CertFactory() throws CertificateException, NoSuchProviderException {
        return CertificateFactory.getInstance("X.509", BouncyIntegration.PROVIDER);
    }

    @Override
    public CertStore getCertStore(CollectionCertStoreParameters certStoreParams) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {

        return CertStore.getInstance("Collection", certStoreParams, BouncyIntegration.PROVIDER);

    }


    @Override
    public CertPathBuilder getCertPathBuilder() throws NoSuchAlgorithmException, NoSuchProviderException {
        return CertPathBuilder.getInstance("PKIX", BouncyIntegration.PROVIDER);
    }

    @Override
    public Signature getSignature(String sigAlgName) throws NoSuchAlgorithmException, NoSuchProviderException {
        return Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(sigAlgName), BouncyIntegration.PROVIDER);

    }

    @Override
    public SSLSocketFactory wrapFactoryForTruststore(SSLSocketFactory delegate) {
        return delegate;
    }
}
