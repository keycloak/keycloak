package org.keycloak.config;

import java.util.Arrays;
import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.common.crypto.FipsProvider;

public class SecurityOptions {

    public static final Option<FipsMode> FIPS_MODE = new OptionBuilder<>("fips-mode", FipsMode.class)
            .category(OptionCategory.SECURITY)
            .expectedValues(getFipsModeValues())
            .buildTime(true)
            .description("Sets the FIPS mode. If '" + FipsMode.NON_STRICT + "' is set, FIPS is enabled but on non-approved mode. For full FIPS compliance, set '" + FipsMode.STRICT + "' to run on approved mode. "
                    + "This option defaults to '" + FipsMode.DISABLED + "' when '" + Profile.Feature.FIPS.getKey() + "' feature is disabled, which is by default. "
                    + "This option defaults to '" + FipsMode.NON_STRICT + "' when '" + Profile.Feature.FIPS.getKey() + "' feature is enabled.")
            .defaultValue(FipsMode.DISABLED)
            .build();

    public static final Option<FipsProvider> FIPS_PROVIDER = new OptionBuilder<>("fips-provider", FipsProvider.class)
            .category(OptionCategory.SECURITY)
            .expectedValues(FipsProvider.values())
            .buildTime(true)
            .description("The cryptographic provider used when FIPS mode is enabled. The 'auto' value uses Bouncy Castle when its FIPS provider classes are available, otherwise it uses Glassless.")
            .defaultValue(FipsProvider.AUTO)
            .build();

    private static List<String> getFipsModeValues() {
        return Arrays.asList(FipsMode.NON_STRICT.toString(), FipsMode.STRICT.toString());
    }
}
