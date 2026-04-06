package org.freedesktop.dbus.connections.shared;

/**
 * Enum representing different executor services.
 *
 * @author hypfvieh
 * @version 4.0.1 - 2022-02-02
 */
public enum ExecutorNames {
    SIGNAL("SignalExecutor"),
    ERROR("ErrorExecutor"),
    METHODCALL("MethodCallExecutor"),
    METHODRETURN("MethodReturnExecutor");

    private final String description;

    ExecutorNames(String _name) {
        description = _name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
