package org.keycloak.common.crypto;

import java.security.spec.ECParameterSpec;

/**
 * Abstraction to handle differences between the APIs for non-fips and fips mode
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface CryptoProvider {

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
    public CertificateUtilsProvider getCertificateUtils();


    /**
     * Get PEMUtils implementation. Returned implementation can be dependent according to if we have
     * non-fips bouncycastle or fips bouncycastle on the classpath.
     *
     * @return
     */
    public PemUtilsProvider getPemUtils();


    /**
     * Create the param spec for the EC curve
     *
     * @param curveName
     * @return
     */
    public ECParameterSpec createECParams(String curveName);

}
