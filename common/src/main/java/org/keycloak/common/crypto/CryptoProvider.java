package org.keycloak.common.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

/**
 * Abstraction to handle differences between the APIs for non-fips and fips mode
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface CryptoProvider {

    /**
     * @return secureRandom implementation based on the available security algorithms according to environment (FIPS non-fips)
     */
    SecureRandom getSecureRandom() throws NoSuchAlgorithmException, NoSuchProviderException;
    
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

}
