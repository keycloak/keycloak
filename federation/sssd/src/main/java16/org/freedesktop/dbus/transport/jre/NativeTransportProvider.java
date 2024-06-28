package org.freedesktop.dbus.transport.jre;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.spi.transport.ITransportProvider;
import org.freedesktop.dbus.utils.Util;

public class NativeTransportProvider implements ITransportProvider {

    @Override
    public String getTransportName() {
        return "dbus-java-transport-native-unixsocket";
    }

    @Override
    public AbstractTransport createTransport(BusAddress _address, TransportConfig _config) throws TransportConfigurationException {
        UnixBusAddress address;
        if (!(_address instanceof UnixBusAddress)) {
            address = new UnixBusAddress(_address);
        } else {
            address = (UnixBusAddress) _address;
        }
        return new NativeUnixSocketTransport(address, _config);
    }

    @Override
    public String getSupportedBusType() {
        return "UNIX";
    }

    @Override
    public String createDynamicSessionAddress(boolean _listeningSocket) {
        return Util.createDynamicSessionAddress(_listeningSocket, false); // native unix sockets do not support abstract sockets
    }

}
