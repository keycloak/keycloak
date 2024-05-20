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
        // Since BC-FIPS 1.0.2.4 PKCS 1.5 is disabled by default and must be enabled via flags
        System.setProperty("org.bouncycastle.rsa.allow_pkcs15_enc",Boolean.TRUE.toString());
    }

    @Override
    public String[] getSupportedRsaKeySizes() {
        // RSA key of 1024 bits not supported in BC-FIPS mode
        return new String[] {"2048", "4096"};
    }
}
