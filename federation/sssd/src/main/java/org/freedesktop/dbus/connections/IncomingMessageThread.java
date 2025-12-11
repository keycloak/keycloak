package org.freedesktop.dbus.connections;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.IllegalThreadPoolStateException;
import org.freedesktop.dbus.interfaces.FatalException;
import org.freedesktop.dbus.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncomingMessageThread extends Thread {
    private final Logger             logger = LoggerFactory.getLogger(getClass());

    private volatile boolean         terminate;
    private final AbstractConnection connection;

    public IncomingMessageThread(AbstractConnection _connection, BusAddress _busAddress) {
        Objects.requireNonNull(_connection);
        connection = _connection;
        setName("DBusConnection [listener=" + _busAddress.isListeningSocket() + "]");
        setDaemon(true);
    }

    public void terminate() {
        terminate = true;
        interrupt();
    }

    @Override
    public void run() {

        Message msg;
        while (!terminate) {
            msg = null;

            // read from the wire
            try {
                // this blocks on outgoing being non-empty or a message being available.
                msg = connection.readIncoming();
                if (msg != null) {
                    logger.trace("Got Incoming Message: {}", msg);

                    connection.handleMessage(msg);
                }
            } catch (DBusException | RejectedExecutionException | IllegalThreadPoolStateException _ex) {
                if (_ex instanceof FatalException) {
                    if (terminate) { // requested termination, ignore failures
                        return;
                    }
                    logger.error("FatalException in connection thread", _ex);
                    if (connection.isConnected()) {
                        terminate = true;
                        if (_ex.getCause() instanceof IOException) {
                            connection.internalDisconnect((IOException) _ex.getCause());
                        } else {
                            connection.internalDisconnect(null);
                        }
                    }
                    return;
                }

                if (!terminate) { // only log exceptions if the connection was not intended to be closed
                    logger.error("Exception in connection thread", _ex);
                }
            }
        }
    }
}
