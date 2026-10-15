package org.keycloak.models.untrustedlogin;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * Server-side SPI for tracking "known" devices/IPs per user and deciding whether a given
 * login is from an untrusted device or network.
 *
 * This is deliberately a thin, session-scoped Provider (same shape as UserSessionProvider,
 * UserLoginFailureProvider, etc.) so the default implementation can be swapped out — e.g. a
 * deployment could replace UntrustedLoginProviderFactory with one backed entirely by an
 * external risk-scoring service, per the "extensible via SPI" scoping decision.
 */
public interface UntrustedLoginProvider extends Provider {

    /**
     * Result of a trust check for a single login attempt.
     */
    class TrustResult {
        public final boolean knownDevice;
        public final boolean knownNetwork;

        public TrustResult(boolean knownDevice, boolean knownNetwork) {
            this.knownDevice = knownDevice;
            this.knownNetwork = knownNetwork;
        }

        public boolean isUntrusted() {
            return !knownDevice && !knownNetwork;
        }
    }

    /**
     * Checks whether the given device/IP combination has been seen before for this user,
     * WITHOUT recording it. Called from the authenticator during the login flow.
     */
    TrustResult checkTrust(RealmModel realm, UserModel user, String userAgent, String ipAddress);

    /**
     * Records the current login's device/IP as known for this user going forward.
     * Called after checkTrust(), regardless of the result, so the next login from the
     * same device/IP is recognized.
     */
    void recordLogin(RealmModel realm, UserModel user, String userAgent, String ipAddress);

    /**
     * Removes all known-device history for a user, e.g. on password reset / "log out
     * everywhere" so the very next login is treated as untrusted again by design.
     */
    void forgetAllDevices(RealmModel realm, UserModel user);
}