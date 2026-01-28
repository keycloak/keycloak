package org.freedesktop.dbus.connections.config;

import java.util.OptionalLong;

import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.connections.SASL.SaslMode;

/**
 * Bean contains configuration for SASL authentication.
 *
 * @author hypfvieh
 *
 * @since 4.2.0 - 2022-07-22
 */
public class SaslConfig {
    private SaslMode     mode;
    private int          authMode;
    private String       guid;
    private OptionalLong saslUid;

    private boolean      strictCookiePermissions;
    private boolean      fileDescriptorSupport;

    SaslConfig() {
        mode = SASL.SaslMode.CLIENT;
        authMode = SASL.AUTH_NONE;
        saslUid = OptionalLong.empty();
    }

    /**
     * Creates a new empty SaslConfig object
     * @return SaslConfig
     * @deprecated only intended for internal backward compatibility, will be removed soon
     */
    @Deprecated(forRemoval = true, since = "4.2.2 - 2023-02-03")
    public static SaslConfig create() {
        return new SaslConfig();
    }

    public SaslMode getMode() {
        return mode;
    }

    public void setMode(SaslMode _mode) {
        mode = _mode;
    }

    public int getAuthMode() {
        return authMode;
    }

    public void setAuthMode(int _types) {
        authMode = _types;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String _guid) {
        guid = _guid;
    }

    public OptionalLong getSaslUid() {
        return saslUid;
    }

    public void setSaslUid(OptionalLong _saslUid) {
        saslUid = _saslUid;
    }

    /**
     * Whether the permissions of the cookie files (used for DBUS_COOKIE_SHA1) should be checked.<br>
     * Cookie permission check will only be used on Linux/Unix like OSes.
     *
     * @return boolean
     * @since v4.2.2 - 2023-02-03
     */
    public boolean isStrictCookiePermissions() {
        return strictCookiePermissions;
    }

    /**
     * Enable/disable checking of file permissions of the cookie files (used for DBUS_COOKIE_SHA1).<br>
     * Cookie permission check will only be used on Linux/Unix like OSes.
     *
     * @return boolean
     * @since v4.2.2 - 2023-02-03
     */
    public void setStrictCookiePermissions(boolean _strictCookiePermissions) {
        strictCookiePermissions = _strictCookiePermissions;
    }

    /**
     * Whether file descriptor passing is allowed.
     *
     * @return boolean
     * @since v4.2.2 - 2023-02-03
     */
    public boolean isFileDescriptorSupport() {
        return fileDescriptorSupport;
    }

    /**
     * Enable/disable support of file descriptor passing.
     *
     * @return boolean
     * @since v4.2.2 - 2023-02-03
     */
    public void setFileDescriptorSupport(boolean _fileDescriptorSupport) {
        fileDescriptorSupport = _fileDescriptorSupport;
    }

}
