package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if the match rule is invalid
 */
public class MatchRuleInvalid extends DBusExecutionException {
    private static final long serialVersionUID = 6922529529288327323L;

    public MatchRuleInvalid(String _message) {
        super(_message);
    }
}
