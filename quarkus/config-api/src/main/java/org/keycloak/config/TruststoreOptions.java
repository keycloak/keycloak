package org.keycloak.config;

import java.util.List;

import org.keycloak.common.enums.HostnameVerificationPolicy;

public class TruststoreOptions {

    public static final Option<List<String>> TRUSTSTORE_PATHS = OptionBuilder.listOptionBuilder("truststore-paths", String.class)
            .category(OptionCategory.TRUSTSTORE)
            .description("List of pkcs12 (p12, pfx, or pkcs12 file extensions), PEM files, or directories containing those files that will be used as a system truststore.")
            .build();

    public static final Option<HostnameVerificationPolicy> HOSTNAME_VERIFICATION_POLICY = new OptionBuilder<>("tls-hostname-verifier", HostnameVerificationPolicy.class)
            .category(OptionCategory.TRUSTSTORE)
            .description("The TLS hostname verification policy for out-going HTTPS and SMTP requests. ANY should not be used in production.")
            .defaultValue(HostnameVerificationPolicy.DEFAULT)
            .deprecatedValues("STRICT and WILDCARD have been deprecated, use DEFAULT instead.", HostnameVerificationPolicy.STRICT, HostnameVerificationPolicy.WILDCARD)
            .build();

}
