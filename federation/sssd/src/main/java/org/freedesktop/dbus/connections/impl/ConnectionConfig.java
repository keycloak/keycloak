package org.freedesktop.dbus.connections.impl;

import java.util.function.Consumer;

import org.freedesktop.dbus.connections.IDisconnectCallback;
import org.freedesktop.dbus.messages.DBusSignal;

public class ConnectionConfig {
    private boolean exportWeakReferences;
    private boolean importWeakReferences;
    private IDisconnectCallback disconnectCallback;
    private Consumer<DBusSignal> unknownSignalHandler;

    public boolean isExportWeakReferences() {
        return exportWeakReferences;
    }

    public void setExportWeakReferences(boolean _exportWeakReferences) {
        exportWeakReferences = _exportWeakReferences;
    }

    public boolean isImportWeakReferences() {
        return importWeakReferences;
    }

    public void setImportWeakReferences(boolean _importWeakReferences) {
        importWeakReferences = _importWeakReferences;
    }

    public IDisconnectCallback getDisconnectCallback() {
        return disconnectCallback;
    }

    public void setDisconnectCallback(IDisconnectCallback _disconnectCallback) {
        disconnectCallback = _disconnectCallback;
    }

    public Consumer<DBusSignal> getUnknownSignalHandler() {
        return unknownSignalHandler;
    }

    public void setUnknownSignalHandler(Consumer<DBusSignal> _unknownSignalHandler) {
        unknownSignalHandler = _unknownSignalHandler;
    }

}
