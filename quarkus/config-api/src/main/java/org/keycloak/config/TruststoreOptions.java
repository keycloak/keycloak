package org.keycloak.config;

import java.util.List;

import org.keycloak.common.enums.HostnameVerificationPolicy;

import static org.keycloak.config.OptionsUtil.DURATION_DESCRIPTION;

public class TruststoreOptions {

    public static final Option<List<String>> TRUSTSTORE_PATHS = OptionBuilder.listOptionBuilder("truststore-paths", String.class)
            .category(OptionCategory.TRUSTSTORE)
            .description("List of pkcs12 (p12, pfx, or pkcs12 file extensions), PEM files, or directories containing those files that will be used as a system truststore.")
            .build();

    public static final Option<Boolean> TRUSTSTORE_KUBERNETES_CA_ENABLED = new OptionBuilder<>("truststore-kubernetes-enabled", Boolean.class)
            .category(OptionCategory.TRUSTSTORE)
            .description("If enabled, the server will automatically include the default Kubernetes service account CA certificate from \"/var/run/secrets/kubernetes.io/serviceaccount/ca.crt\" and the OpenShift service CA certificate from \"/var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt\" when running in a container environment.")
            .defaultValue(true)
            .build();

    public static final Option<HostnameVerificationPolicy> HOSTNAME_VERIFICATION_POLICY = new OptionBuilder<>("tls-hostname-verifier", HostnameVerificationPolicy.class)
            .category(OptionCategory.TRUSTSTORE)
            .description("The TLS hostname verification policy for out-going HTTPS and SMTP requests. ANY should not be used in production.")
            .defaultValue(HostnameVerificationPolicy.DEFAULT)
            .deprecatedValues("STRICT and WILDCARD have been deprecated, use DEFAULT instead.", HostnameVerificationPolicy.STRICT, HostnameVerificationPolicy.WILDCARD)
            .build();

    public static final String TRUSTSTORE_PATHS_RELOAD_PERIOD_KEY = "truststore-paths-reload-period";

    public static final Option<String> TRUSTSTORE_PATHS_RELOAD_PERIOD = new OptionBuilder<>(TRUSTSTORE_PATHS_RELOAD_PERIOD_KEY, String.class)
            .buildTime(true)
            .category(OptionCategory.TRUSTSTORE)
            .description("Interval on which to reload the system truststore material referenced by the truststore-paths option and the automatically discovered CA certificates. "
                    + DURATION_DESCRIPTION + " If not set, the system truststore is not reloaded automatically. When the reload period is configured, there must be at least one truststore source: a truststore-paths entry, a file under conf/truststores, or a discovered cluster CA certificate.")
            .build();

}
