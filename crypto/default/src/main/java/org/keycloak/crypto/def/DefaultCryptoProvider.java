package org.keycloak.crypto.def;

import java.security.Provider;
import java.security.Security;
import java.security.spec.ECParameterSpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.crypto.CryptoConstants;
import org.keycloak.common.crypto.CertificateUtilsProvider;
import org.keycloak.common.crypto.PemUtilsProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultCryptoProvider implements CryptoProvider {

    private final BouncyCastleProvider bcProvider;

    private Map<String, Object> providers = new ConcurrentHashMap<>();

    public DefaultCryptoProvider() {
        // Make sure to instantiate this only once due it is expensive. And skip registration if already available in Java security providers (EG. due explicitly configured in java security file)
        BouncyCastleProvider existingBc = (BouncyCastleProvider) Security.getProvider(CryptoConstants.BC_PROVIDER_ID);
        this.bcProvider = existingBc == null ? new BouncyCastleProvider() : existingBc;

        providers.put(CryptoConstants.A128KW, new AesKeyWrapAlgorithmProvider());
        providers.put(CryptoConstants.RSA1_5, new DefaultRsaKeyEncryptionJWEAlgorithmProvider("RSA/ECB/PKCS1Padding"));
        providers.put(CryptoConstants.RSA_OAEP, new DefaultRsaKeyEncryptionJWEAlgorithmProvider("RSA/ECB/OAEPWithSHA-1AndMGF1Padding"));
        providers.put(CryptoConstants.RSA_OAEP_256, new DefaultRsaKeyEncryption256JWEAlgorithmProvider("RSA/ECB/OAEPWithSHA-256AndMGF1Padding"));
    }


    @Override
    public Provider getBouncyCastleProvider() {
        return bcProvider;
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

}
