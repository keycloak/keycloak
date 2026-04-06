package org.freedesktop.dbus.connections.base;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.IllegalThreadPoolStateException;
import org.freedesktop.dbus.interfaces.FatalException;
import org.freedesktop.dbus.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncomingMessageThread extends Thread {
    private final Logger             logger = LoggerFactory.getLogger(getClass());

    private volatile boolean         terminate;
    private final ConnectionMessageHandler connection;

    public IncomingMessageThread(ConnectionMessageHandler _connection, BusAddress _busAddress) {
        connection = Objects.requireNonNull(_connection);
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
                    logger.trace("Read message from {}: {}", connection.getTransport(), msg);

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
                        if (_ex.getCause() instanceof IOException ioe) {
                            connection.internalDisconnect(ioe);
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
