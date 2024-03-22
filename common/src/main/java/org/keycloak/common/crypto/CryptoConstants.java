package org.keycloak.common.crypto;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CryptoConstants {

    // JWE algorithms
    public static final String A128KW = "A128KW";
    public static final String RSA1_5 = "RSA1_5";
    public static final String RSA_OAEP = "RSA-OAEP";
    public static final String RSA_OAEP_256 = "RSA-OAEP-256";
    public static final String ECDH_ES = "ECDH-ES";
    public static final String ECDH_ES_A128KW = "ECDH-ES+A128KW";
    public static final String ECDH_ES_A192KW = "ECDH-ES+A192KW";
    public static final String ECDH_ES_A256KW = "ECDH-ES+A256KW";

    // Constant for the OCSP provider
    // public static final String OCSP = "OCSP";

    /** Name of Java security provider used with non-fips BouncyCastle. Should be used in non-FIPS environment */
    public static final String BC_PROVIDER_ID = "BC";

    /** Name of Java security provider used with fips BouncyCastle. Should be used in FIPS environment */
    public static final String BCFIPS_PROVIDER_ID = "BCFIPS";

}
