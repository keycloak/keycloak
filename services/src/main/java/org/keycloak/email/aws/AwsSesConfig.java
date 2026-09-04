/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.email.aws;

import java.net.URI;
import java.net.URISyntaxException;

import org.keycloak.Config;
import org.keycloak.email.aws.credentials.AwsEnvironment;

/**
 * Server-wide configuration of the SES sender, resolved once at boot.
 * <p>
 * Everything here is an SPI option ({@code --spi-email-sender-aws-ses-<key>}) with an AWS-standard
 * environment variable as a fallback, so a server already configured the way every other AWS tool on
 * the box is configured needs no Keycloak-specific settings at all.
 * <p>
 * What is deliberately <em>not</em> here is credentials. There is no {@code access-key} option: the
 * secret would then live in Keycloak's configuration, where {@code kc.sh show-config} prints
 * unregistered SPI options unmasked. Credentials come from the standard AWS chain — an IAM role
 * where one exists, {@code AWS_ACCESS_KEY_ID}/{@code AWS_SECRET_ACCESS_KEY} in the process
 * environment where it does not.
 * <p>
 * Per-realm settings are not here either. The email sender is resolved once for the whole server, so
 * a per-realm region or endpoint would be a setting Keycloak has no way to honour. The realm keeps
 * what is genuinely per-realm — sender address, display name, reply-to — in its existing SMTP block.
 */
public final class AwsSesConfig {

    /**
     * SES answers on {@code email.<region>.amazonaws.com} but signs as {@code ses}. The two are not
     * interchangeable and the hostname is not the source of the signing name.
     */
    static final String SIGNING_SERVICE = "ses";

    static final String SEND_EMAIL_PATH = "/v2/email/outbound-emails";

    static final int DEFAULT_TIMEOUT_MILLIS = 10_000;

    private final String region;
    private final URI sendEmailUri;
    private final String hostHeader;
    private final String configurationSetName;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    private AwsSesConfig(String region, URI sendEmailUri, String configurationSetName,
                         int connectTimeoutMillis, int readTimeoutMillis) {
        this.region = region;
        this.sendEmailUri = sendEmailUri;
        this.hostHeader = hostHeader(sendEmailUri);
        this.configurationSetName = configurationSetName;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public static AwsSesConfig from(Config.Scope scope, AwsEnvironment environment) {
        String region = firstPresent(scope.get("region"), environment.value("AWS_REGION"), environment.value("AWS_DEFAULT_REGION"));
        if (region == null) {
            throw new AwsSesConfigurationException("No AWS region configured for the Amazon SES email sender."
                    + " Set --spi-email-sender-aws-ses-region, or AWS_REGION in the server environment."
                    + " SES identities are per region: a verified domain in one region does not exist in another.");
        }
        if (!AwsRegion.isValid(region)) {
            throw new AwsSesConfigurationException("'" + region + "' is not a valid AWS region name");
        }

        String endpoint = firstPresent(scope.get("endpoint"),
                environment.value("AWS_ENDPOINT_URL_SESV2"), environment.value("AWS_ENDPOINT_URL"));
        URI sendEmailUri = sendEmailUri(endpoint, region);

        int connectTimeout = timeout(scope, "connect-timeout");
        int readTimeout = timeout(scope, "read-timeout");

        return new AwsSesConfig(region, sendEmailUri, blankToNull(scope.get("configuration-set")),
                connectTimeout, readTimeout);
    }

    /**
     * Reads a timeout option, converting a malformed value into a configuration error rather than
     * letting it escape.
     * <p>
     * {@code Config.Scope.getInt} throws a bare {@link NumberFormatException}, which is not an
     * {@link AwsSesConfigurationException} — so a typo here would escape the factory's "this provider
     * is not the one in use" branch and stop a server that sends over SMTP from booting at all. The
     * typo is an easy one to make: most Keycloak duration options are written {@code 10s}.
     */
    private static int timeout(Config.Scope scope, String key) {
        String value = scope.get(key);
        if (value == null || value.isBlank()) {
            return DEFAULT_TIMEOUT_MILLIS;
        }
        int millis;
        try {
            millis = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new AwsSesConfigurationException("spi-email-sender-aws-ses-" + key + " must be a whole number of"
                    + " milliseconds, but is '" + value + "'");
        }
        if (millis <= 0) {
            throw new AwsSesConfigurationException("spi-email-sender-aws-ses-" + key + " must be positive; an"
                    + " unbounded timeout would hold a Keycloak transaction open for as long as the network takes"
                    + " to give up");
        }
        return millis;
    }

    private static URI sendEmailUri(String endpoint, String region) {
        // The suffix comes from the region's partition: hard-coding amazonaws.com would build an
        // endpoint that does not resolve in the China partition, where cn-north-1 answers on
        // amazonaws.com.cn.
        String base = endpoint != null ? stripTrailingSlashes(endpoint)
                : "https://email." + region + "." + AwsRegion.dnsSuffix(region);
        try {
            URI parsed = new URI(base);
            if (parsed.getScheme() == null || parsed.getHost() == null) {
                throw new AwsSesConfigurationException("'" + endpoint + "' is not a usable Amazon SES endpoint URL:"
                        + " it must be absolute, for example https://email.eu-central-1.amazonaws.com");
            }
            if (!"https".equalsIgnoreCase(parsed.getScheme()) && !"http".equalsIgnoreCase(parsed.getScheme())) {
                // The transport speaks HTTP only, so any other scheme parses happily at boot and then
                // fails on every single send.
                throw new AwsSesConfigurationException("The Amazon SES endpoint URL must be http or https,"
                        + " but is '" + endpoint + "'");
            }
            if (parsed.getRawQuery() != null || parsed.getRawFragment() != null) {
                // Appending the operation path to a URL that already has a query string would bury it
                // inside that query — the request would be posted to "/" and SES would answer 404,
                // a long way from the typo that caused it.
                throw new AwsSesConfigurationException("The Amazon SES endpoint URL must carry no query string or"
                        + " fragment, but is '" + endpoint + "'");
            }
            return new URI(base + SEND_EMAIL_PATH);
        } catch (URISyntaxException e) {
            throw new AwsSesConfigurationException("'" + endpoint + "' is not a valid Amazon SES endpoint URL", e);
        }
    }

    /**
     * The {@code Host} header value, computed here rather than left to the HTTP client because it is
     * signed: if the client renders it differently — appending an explicit {@code :443} the signature
     * did not cover, for instance — AWS answers 403 with nothing to explain why.
     */
    private static String hostHeader(URI uri) {
        return uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
    }

    /**
     * Removes every trailing slash, not just one: the operation path is appended to what this
     * returns, so an endpoint written {@code https://host//} would otherwise post to
     * {@code //v2/email/outbound-emails}.
     */
    private static String stripTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private static String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public String region() {
        return region;
    }

    public URI sendEmailUri() {
        return sendEmailUri;
    }

    public String hostHeader() {
        return hostHeader;
    }

    public String configurationSetName() {
        return configurationSetName;
    }

    public int connectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int readTimeoutMillis() {
        return readTimeoutMillis;
    }
}
