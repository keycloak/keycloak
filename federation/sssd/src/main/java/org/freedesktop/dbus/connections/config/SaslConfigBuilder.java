package org.freedesktop.dbus.connections.config;

import java.util.OptionalLong;
import java.util.function.Supplier;

import org.freedesktop.dbus.connections.transports.TransportBuilder.SaslAuthMode;

/**
 * Configuration used to setup a sasl authentication.
 *
 * @author hypfvieh
 * @since v4.2.2 - 2023-02-03
 */
public final class SaslConfigBuilder<R> {

    private final SaslConfig saslConfig;

    private final Supplier<TransportConfigBuilder<?, R>> transportBuilder;

    SaslConfigBuilder(SaslConfig _saslConfig, Supplier<TransportConfigBuilder<?, R>> _transportBuilder) {
        saslConfig = _saslConfig;
        transportBuilder = _transportBuilder;
    }

    /**
     * Return to the previous builder.
     * <p>
     * This allows you to return from the this builder to the builder which
     * started this builder so you can continue using the previous builder.
     * </p>
     *
     * @return previous builder, maybe null
     */
    public TransportConfigBuilder<?, R> back() {
        return transportBuilder.get();
    }

    /**
     * Setup the authentication mode to use. <br>
     * <code>null</code> values will be ignored.
     *
     * @param _types auth mode to set
     * @return this
     */
    public SaslConfigBuilder<R> withAuthMode(SaslAuthMode _types) {
        if (_types != null) {
            saslConfig.setAuthMode(_types.getAuthMode());
        }
        return this;
    }

    /**
     * Setup the user ID to use for authentication when using unix sockets.<br>
     * Will default to the user ID of the user running the current process.
     *
     * @param _guid guid to use
     * @return this
     */
    public SaslConfigBuilder<R> withSaslUid(Long _saslUid) {
        saslConfig.setSaslUid(OptionalLong.of(_saslUid));
        return this;
    }

    /**
     * Enable/disable checking of file permissions of the cookie files (used for DBUS_COOKIE_SHA1).<br>
     * Cookie permission check will only be used on Linux/Unix like OSes.<br>
     * Default is false (no strict checking).
     *
     * @param _strictCookiePermissions boolean
     * @return this
     */
    public SaslConfigBuilder<R> withStrictCookiePermissions(boolean _strictCookiePermissions) {
        saslConfig.setStrictCookiePermissions(_strictCookiePermissions);
        return this;
    }

    /**
     * Returns the created configuration.
     * @return SaslConfig
     */
    public SaslConfig build() {
        return saslConfig;
    }
}
