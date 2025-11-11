package org.keycloak.testframework.crypto;

import org.keycloak.common.crypto.FipsMode;

public class CryptoHelper {

    private final FipsMode fips;

    public CryptoHelper(FipsMode fips) {
        this.fips = fips;
    }

    public CryptoKeyStore keystore() {
        return new CryptoKeyStore(this);
    }

    public boolean isFips() {
        return switch (fips) {
            case STRICT, NON_STRICT -> true;
            default -> false;
        };
    }

    public String[] getExpectedSupportedKeyStoreTypes() {
        return switch (fips) {
            case NON_STRICT -> new String[] { "PKCS12", "BCFKS" };
            case STRICT -> new String[] { "BCFKS" };
            default -> new String[] { "BCFKS", "JKS", "PKCS12" };
        };
    }

    public String[] getExpectedSupportedRsaKeySizes() {
        return switch (fips) {
            case STRICT -> new String[]{"2048", "3072", "4096"};
            default -> new String[]{"1024", "2048", "3072", "4096"};
        };
    }

}
