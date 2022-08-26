package org.keycloak.common.crypto;

import java.security.Provider;
import java.security.spec.ECParameterSpec;

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

}
