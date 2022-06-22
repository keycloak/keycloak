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
     * Get some cryptographic utility implementation. Returned implementation can be dependent according to if we have
     * non-fips bouncycastle or fips bouncycastle on the classpath. The implementation should be previously registered by {@link #registerUtility(String, Object)}
     * 
     * @param clazz Returned class.
     * @param typeId Type of the utility, which we want to return 
     * @return 
     */
    <T> T getCryptoUtility(Class<T> clazz, String typeId);
}
