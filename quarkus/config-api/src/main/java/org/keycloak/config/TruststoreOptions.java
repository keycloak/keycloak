package org.keycloak.config;

import org.keycloak.common.enums.HostnameVerificationPolicy;

public class TruststoreOptions {

    public static final Option<String> TRUSTSTORE_PATHS = new OptionBuilder<>("truststore-paths", String.class)
            .category(OptionCategory.TRUSTSTORE)
            .description("List of pkcs12 (p12 or pfx file extensions), PEM files, or directories containing those files that will be used as a system truststore.")
            .build();

    public static final Option<HostnameVerificationPolicy> HOSTNAME_VERIFICATION_POLICY = new OptionBuilder<>("tls-hostname-verifier", HostnameVerificationPolicy.class)
            .category(OptionCategory.TRUSTSTORE)
            .description("The TLS hostname verification policy for out-going HTTPS and SMTP requests.")
            .defaultValue(HostnameVerificationPolicy.WILDCARD)
            .build();

}
