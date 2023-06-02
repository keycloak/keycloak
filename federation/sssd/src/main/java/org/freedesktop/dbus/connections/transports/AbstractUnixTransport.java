package org.freedesktop.dbus.connections.transports;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.config.TransportConfig;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public abstract class AbstractUnixTransport extends AbstractTransport {

    protected AbstractUnixTransport(BusAddress _address, TransportConfig _config) {
        super(_address, _config);
    }

    public abstract int getUid(SocketChannel _sock) throws IOException;

}
