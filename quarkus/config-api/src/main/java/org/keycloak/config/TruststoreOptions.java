package org.keycloak.config;

import org.keycloak.common.enums.HostnameVerificationPolicy;

import java.util.List;
import java.util.Set;

public class TruststoreOptions {

    public static final Option<List<String>> TRUSTSTORE_PATHS = OptionBuilder.listOptionBuilder("truststore-paths", String.class)
            .category(OptionCategory.TRUSTSTORE)
            .description("List of pkcs12 (p12 or pfx file extensions), PEM files, or directories containing those files that will be used as a system truststore.")
            .build();

    public static final Option<HostnameVerificationPolicy> HOSTNAME_VERIFICATION_POLICY = new OptionBuilder<>("tls-hostname-verifier", HostnameVerificationPolicy.class)
            .category(OptionCategory.TRUSTSTORE)
            .description("The TLS hostname verification policy for out-going HTTPS and SMTP requests.")
            .defaultValue(HostnameVerificationPolicy.DEFAULT)
            .deprecatedValues(Set.of("STRICT", "WILDCARD"), "STRICT and WILDCARD have been deprecated, use DEFAULT instead.")
            .build();

}
