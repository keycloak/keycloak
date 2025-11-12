package org.keycloak.tests.utils;

import org.keycloak.representations.info.ServerInfoRepresentation;

public class FipsUtils {

    private final String cryptoProvider;

    private FipsUtils(ServerInfoRepresentation info) {
        this.cryptoProvider = info.getCryptoInfo().getCryptoProvider();
    }

    public static FipsUtils create(ServerInfoRepresentation info) {
        return new FipsUtils(info);
    }

    public String[] getExpectedSupportedKeyStoreTypes() {
        return switch (cryptoProvider) {
            case "FIPS1402Provider" -> new String[] { "PKCS12", "BCFKS" };
            case "Fips1402StrictCryptoProvider" -> new String[] { "BCFKS" };
            default -> new String[] { "JKS", "PKCS12", "BCFKS" };
        };
    }

    public String[] getExpectedSupportedRsaKeySizes() {
        return switch (cryptoProvider) {
            case "Fips1402StrictCryptoProvider" -> new String[]{"2048", "3072", "4096"};
            default -> new String[]{"1024", "2048", "3072", "4096"};
        };
    }

}
