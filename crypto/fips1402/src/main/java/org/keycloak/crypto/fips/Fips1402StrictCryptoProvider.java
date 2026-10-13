package org.keycloak.crypto.fips;

import org.bouncycastle.crypto.CryptoServicesRegistrar;

/**
 * <p>A {@link FIPS1402Provider} that forces BC to run in FIPS approve mode by default.
 *
 * <p>In order to set the default mode the {@code org.bouncycastle.fips.approved_only} must be set. Otherwise,
 * calling {@link CryptoServicesRegistrar#setApprovedOnlyMode(boolean)} the mode is set on a per thread-basis and does not work
 * well when handling requests using multiple threads.
 */
public class Fips1402StrictCryptoProvider extends FIPS1402Provider {

    static {
        System.setProperty("org.bouncycastle.fips.approved_only", Boolean.TRUE.toString());
    }

    @Override
    public String[] getSupportedRsaKeySizes() {
        // RSA key of 1024 bits not supported in BCFIPS approved mode
        return new String[] {"2048", "3072", "4096"};
    }
}
