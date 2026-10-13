/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.spi.infinispan;

import java.time.Duration;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.keycloak.provider.Provider;

/**
 * A {@link Provider} for the TLS certificate for JGroups communication.
 * <p>
 * <b>Implementation notes</b>
 * <p>
 * If the method {@link #isEnabled()} returns {@code true}, then the implementation must also implement
 * {@link #keyManager()} and {@link #trustManager()}.
 * <p>
 * If the method {@link #supportRotateAndReload()} returns {@code true}, then the implementation must also implement
 * {@link #rotateCertificate()}, {@link #reloadCertificate()} and {@link #nextRotation()}.
 */
public interface JGroupsCertificateProvider extends Provider {

    JGroupsCertificateProvider DISABLED = new JGroupsCertificateProvider() {
    };

    /**
     * A new certificate must be generated.
     * <p>
     * The generated certificate should not be used immediately, but only after {@link #reloadCertificate()} is
     * invoked.
     * <p>
     * This method must be implemented when {@link #supportRotateAndReload()} returns {@code true}.
     */
    default void rotateCertificate() {
        throw new UnsupportedOperationException();
    }

    /**
     * Reloads the most recent certificate and apply it to the {@link KeyManager} and {@link TrustManager}.
     * <p>
     * This method must be implemented when {@link #supportRotateAndReload()} returns {@code true}.
     */
    default void reloadCertificate() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns when the next certificate rotation is required.
     * <p>
     * It is used to automatically rotate certificates periodically.
     * <p>
     * This method must be implemented when {@link #supportRotateAndReload()} returns {@code true}.
     *
     * @return The time until the next rotation.
     */
    default Duration nextRotation() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a managed {@link KeyManager}.
     * <p>
     * If {@link #supportRotateAndReload()} returns {@code true}, the instance returned must be updated with the new
     * certificate when {@link #reloadCertificate()}. This method is invoked only once at boot time.
     * <p>
     * This method must be implemented when {@link #isEnabled()} returns {@code true}.
     *
     * @return The {@link KeyManager} to use by the {@link javax.net.ssl.SSLContext}.
     */
    default KeyManager keyManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a managed {@link TrustManager}.
     * <p>
     * If {@link #supportRotateAndReload()} returns {@code true}, the instance returned must be updated with the new
     * certificate when {@link #reloadCertificate()}. This method is invoked only once at boot time.
     * <p>
     * This method must be implemented when {@link #isEnabled()} returns {@code true}.
     *
     * @return The {@link TrustManager} to use by the {@link javax.net.ssl.SSLContext}.
     */
    default TrustManager trustManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return {@code true} if rotation and reload requests is possible.
     */
    default boolean supportRotateAndReload() {
        return false;
    }

    /**
     * @return {@code true} if TLS is enabled for JGroups communication.
     */
    default boolean isEnabled() {
        return false;
    }

    @Override
    default void close() {
    }
}
