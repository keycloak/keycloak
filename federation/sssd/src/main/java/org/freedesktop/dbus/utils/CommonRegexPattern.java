package org.freedesktop.dbus.utils;

import java.util.regex.Pattern;

/**
 * Utility class containing commonly used regular expression patterns.
 *
 * @author hypfvieh
 * @version 4.1.0 - 2022-02-08
 */
public final class CommonRegexPattern {
    public static final Pattern PROXY_SPLIT_PATTERN = Pattern.compile("[<>]");
    public static final Pattern IFACE_PATTERN       = Pattern.compile("^interface *name *= *['\"]([^'\"]*)['\"].*$");

    public static final Pattern DBUS_IFACE_PATTERN  = Pattern.compile("^.*\\.([^\\.]+)$");

    public static final Pattern EXCEPTION_EXTRACT_PATTERN = Pattern.compile("\\.([^\\.]*)$");
    public static final Pattern EXCEPTION_PARTIAL_PATTERN = Pattern.compile(".*\\..*");

    private CommonRegexPattern() {

    }
}
