package org.keycloak.crypto.def;

import java.security.spec.ECParameterSpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.crypto.CryptoProviderTypes;
import org.keycloak.common.crypto.CertificateUtilsProvider;
import org.keycloak.common.crypto.PemUtilsProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultCryptoProvider implements CryptoProvider {

    private Map<String, Object> providers = new ConcurrentHashMap<>();

    public DefaultCryptoProvider() {
        providers.put(CryptoProviderTypes.BC_SECURITY_PROVIDER, new BouncyCastleProvider());
        providers.put(CryptoProviderTypes.AES_KEY_WRAP_ALGORITHM_PROVIDER, new AesKeyWrapAlgorithmProvider());
    }

    @Override
    public <T> T getAlgorithmProvider(Class<T> clazz, String algorithm) {
        Object o = providers.get(algorithm);
        if (o == null) {
            throw new IllegalArgumentException("Not found provider of algorithm: " + algorithm);
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
