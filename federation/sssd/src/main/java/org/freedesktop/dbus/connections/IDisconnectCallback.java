package org.freedesktop.dbus.connections;

import java.io.IOException;

/**
 * Callback interface which can be used to get notified about connection losses.
 *
 * @author hypfvieh
 * @version 4.1.0 - 2022-02-03
 */
public interface IDisconnectCallback {
    /**
     * Called when the connection is closed due to a connection error (e.g. stream closed).
     *
     * @param _ex exception which was thrown by transport
     */
    default void disconnectOnError(IOException _ex) {}

    /**
     * Called when the disconnect was intended.
     * @param _connectionId connection Id if this was a shared connection,
     *                      null if last shared or non-shared connection
     */
    default void requestedDisconnect(Integer _connectionId) {}

    /**
     * Called when a client disconnected (only if this is a server/listening connection).
     */
    default void clientDisconnect() {}

    /**
     * Called when the transport throws an exception
     * while connection was already terminating.
     *
     * @param _ex exception which was thrown by transport
     */
    default void exceptionOnTerminate(IOException _ex) {}

}
