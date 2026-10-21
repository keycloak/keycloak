/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.httpclient;

/**
 * Configuration for HTTP client retry behavior.
 * <p>
 * This class provides configuration options for HTTP client retry behavior when
 * making requests. It allows customization of the maximum number of retry
 * attempts, exponential backoff settings, jitter for backoff times, and
 * connection/socket timeouts.
 * <p>
 * The default configuration is 0 retry attempts (no retries), with exponential backoff starting
 * at 1000ms and multiplying by 2.0 for each retry. Jitter is enabled by
 * default with a factor of 0.5 to prevent synchronized retry storms.
 * <p>
 * This configuration is used internally by {@link HttpClientProvider#getHttpClient()}
 * to create HTTP clients with server-wide retry capabilities. The configuration
 * is set globally through the HTTP client provider SPI configuration.
 * <p>
 * Server-wide SPI properties for configuring retry behavior:
 * <ul>
 * <li>{@code spi-connections-http-client-default-max-retries} - Maximum number
 * of retry attempts (default: 0)</li>
 * <li>{@code spi-connections-http-client-default-initial-backoff-millis} - Initial
 * backoff time in milliseconds (default: 1000)</li>
 * <li>{@code spi-connections-http-client-default-backoff-multiplier} - Multiplier
 * for exponential backoff (default: 2.0)</li>
 * <li>{@code spi-connections-http-client-default-jitter-factor} - Random jitter
 * factor to apply to backoff times (default: 0.5)</li>
 * <li>{@code spi-connections-http-client-default-use-jitter} - Whether to apply
 * jitter to backoff times (default: true)</li>
 * </ul>
 * <p>
 * Example configuration:
 *
 * <pre>
 * spi-connections-http-client-default-max-retries=3
 * spi-connections-http-client-default-initial-backoff-millis=1000
 * spi-connections-http-client-default-backoff-multiplier=2.0
 * </pre>
 *
 * This configuration applies to all outgoing HTTP requests from Keycloak,
 * including OCSP validation, identity provider communication, and other
 * external HTTP calls.
 */
public class RetryConfig {
    private final int maxRetries;
    private final long initialBackoffMillis;
    private final double backoffMultiplier;
    private final boolean useJitter;
    private final double jitterFactor;
    private final int connectionTimeoutMillis;
    private final int socketTimeoutMillis;

    private RetryConfig(Builder builder) {
        this.maxRetries = builder.maxRetries;
        this.initialBackoffMillis = builder.initialBackoffMillis;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.useJitter = builder.useJitter;
        this.jitterFactor = builder.jitterFactor;
        this.connectionTimeoutMillis = builder.connectionTimeoutMillis;
        this.socketTimeoutMillis = builder.socketTimeoutMillis;
    }

    /**
     * Gets the maximum number of retry attempts.
     *
     * @return The maximum number of retry attempts
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Gets the initial backoff time in milliseconds before the first retry attempt.
     *
     * @return The initial backoff time in milliseconds
     */
    public long getInitialBackoffMillis() {
        return initialBackoffMillis;
    }

    /**
     * Gets the multiplier used for exponential backoff between retry attempts.
     *
     * @return The backoff multiplier
     */
    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    /**
     * Gets the connection timeout in milliseconds.
     *
     * @return The connection timeout in milliseconds
     */
    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    /**
     * Gets the socket timeout in milliseconds.
     *
     * @return The socket timeout in milliseconds
     */
    public int getSocketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    /**
     * Determines whether to apply jitter to backoff times.
     * <p>
     * Jitter adds randomness to backoff times to prevent synchronized retry storms
     * when multiple clients are retrying at the same time.
     *
     * @return {@code true} if jitter should be applied, {@code false} otherwise
     */
    public boolean isUseJitter() {
        return useJitter;
    }

    /**
     * Gets the jitter factor to apply to backoff times.
     * <p>
     * The jitter factor determines how much randomness to apply to the backoff
     * time.
     * A value of 0.5 means the actual backoff time will be between 50% and 150% of
     * the calculated exponential backoff time.
     *
     * @return The jitter factor
     */
    public double getJitterFactor() {
        return jitterFactor;
    }

    /**
     * Compares this RetryConfig with another object for equality.
     * <p>
     * Two RetryConfig objects are considered equal if all their configuration
     * parameters match exactly.
     *
     * @param obj The object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        RetryConfig that = (RetryConfig) obj;

        if (maxRetries != that.maxRetries) return false;
        if (initialBackoffMillis != that.initialBackoffMillis) return false;
        if (Double.compare(backoffMultiplier, that.backoffMultiplier) != 0) return false;
        if (useJitter != that.useJitter) return false;
        if (Double.compare(jitterFactor, that.jitterFactor) != 0) return false;
        if (connectionTimeoutMillis != that.connectionTimeoutMillis) return false;
        return socketTimeoutMillis == that.socketTimeoutMillis;
    }

    /**
     * Returns a hash code value for this RetryConfig.
     * <p>
     * This method is implemented to be consistent with {@link #equals(Object)},
     * ensuring that equal RetryConfig objects have the same hash code.
     *
     * @return A hash code value for this object
     */
    @Override
    public int hashCode() {
        int result;
        long temp;
        result = maxRetries;
        result = 31 * result + (int) (initialBackoffMillis ^ (initialBackoffMillis >>> 32));
        temp = Double.doubleToLongBits(backoffMultiplier);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (useJitter ? 1 : 0);
        temp = Double.doubleToLongBits(jitterFactor);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + connectionTimeoutMillis;
        result = 31 * result + socketTimeoutMillis;
        return result;
    }

    /**
     * Builder for creating {@link RetryConfig} instances.
     * <p>
     * This builder uses the following defaults:
     * <ul>
     * <li>maxRetries = 0</li>
     * <li>initialBackoffMillis = 1000</li>
     * <li>backoffMultiplier = 2.0</li>
     * <li>connectionTimeoutMillis = 10000</li>
     * <li>socketTimeoutMillis = 10000</li>
     * </ul>
     */
    public static class Builder {
        private int maxRetries = 0;
        private long initialBackoffMillis = 1000;
        private double backoffMultiplier = 2.0;
        private boolean useJitter = true;
        private double jitterFactor = 0.5;
        private int connectionTimeoutMillis = 10000;
        private int socketTimeoutMillis = 10000;

        /**
         * Sets the maximum number of retry attempts.
         * <p>
         * The default value is 3. A value of 0 means no retries will be attempted.
         * Negative values are allowed but not recommended as they don't make practical
         * sense.
         *
         * @param maxRetries The maximum number of retry attempts
         * @return This builder instance for method chaining
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets the initial backoff time in milliseconds before the first retry attempt.
         * <p>
         * The default value is 1000 (1 second). This is the amount of time to wait
         * before
         * the first retry attempt. Subsequent retry attempts will use exponential
         * backoff
         * based on this value and the backoff multiplier.
         *
         * @param initialBackoffMillis The initial backoff time in milliseconds
         * @return This builder instance for method chaining
         */
        public Builder initialBackoffMillis(long initialBackoffMillis) {
            this.initialBackoffMillis = initialBackoffMillis;
            return this;
        }

        /**
         * Sets the multiplier used for exponential backoff between retry attempts.
         * <p>
         * The default value is 2.0. This means that each retry will wait twice as long
         * as
         * the previous retry. For example, with an initial backoff of 1000ms and a
         * multiplier
         * of 2.0, the retry delays would be: 1000ms, 2000ms, 4000ms, etc.
         *
         * @param backoffMultiplier The backoff multiplier
         * @return This builder instance for method chaining
         */
        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        /**
         * Sets the connection timeout in milliseconds.
         * <p>
         * The default value is 10000 (10 seconds). This is the timeout for establishing
         * a connection with the remote server.
         *
         * @param connectionTimeoutMillis The connection timeout in milliseconds
         * @return This builder instance for method chaining
         */
        public Builder connectionTimeoutMillis(int connectionTimeoutMillis) {
            this.connectionTimeoutMillis = connectionTimeoutMillis;
            return this;
        }

        /**
         * Sets the socket timeout in milliseconds.
         * <p>
         * The default value is 10000 (10 seconds). This is the timeout for waiting for
         * data
         * from an established connection.
         *
         * @param socketTimeoutMillis The socket timeout in milliseconds
         * @return This builder instance for method chaining
         */
        public Builder socketTimeoutMillis(int socketTimeoutMillis) {
            this.socketTimeoutMillis = socketTimeoutMillis;
            return this;
        }

        /**
         * Sets whether to apply jitter to backoff times.
         * <p>
         * The default value is {@code true}. When set to {@code true}, the system will
         * add
         * randomness to backoff times to prevent synchronized retry storms when
         * multiple
         * clients are retrying at the same time.
         *
         * @param useJitter {@code true} to apply jitter, {@code false} otherwise
         * @return This builder instance for method chaining
         */
        public Builder useJitter(boolean useJitter) {
            this.useJitter = useJitter;
            return this;
        }

        /**
         * Sets the jitter factor to apply to backoff times.
         * <p>
         * The default value is 0.5. This means the actual backoff time will be between
         * 50% and 150% of the calculated exponential backoff time. For example, if the
         * calculated backoff time is 1000ms, the actual backoff time will be between
         * 500ms and 1500ms.
         *
         * @param jitterFactor The jitter factor
         * @return This builder instance for method chaining
         */
        public Builder jitterFactor(double jitterFactor) {
            this.jitterFactor = jitterFactor;
            return this;
        }

        /**
         * Builds a new {@link RetryConfig} instance with the current builder settings.
         *
         * @return A new {@link RetryConfig} instance
         */
        public RetryConfig build() {
            return new RetryConfig(this);
        }
    }
}