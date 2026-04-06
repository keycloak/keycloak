package org.freedesktop.dbus.spi.transport;

import javax.xml.transform.TransformerConfigurationException;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;

/**
 * Interface used by {@link java.util.ServiceLoader ServiceLoader} to provide a transport used by DBus.
 *
 * @author hypfvieh
 * @since v4.0.0 - 2021-09-05
 */
public interface ITransportProvider {
    /**
     * Name of the transport implementation.
     * @return String, should not be null or empty
     */
    String getTransportName();

    /**
     * Creates a new instance of this transport service using the given bus address.
     * <p>
     * If transport cannot be created because bus address type is not supported, {@code null}
     * should be returned. If initialization fails because of any other error, throw a {@link TransportConfigurationException}.
     * </p>
     *
     * @param _address bus address
     * @param _config configuration for this transport
     *
     * @return transport instance or null
     *
     * @throws TransformerConfigurationException when configuring transport fails
     */
    AbstractTransport createTransport(BusAddress _address, TransportConfig _config) throws TransportConfigurationException;

    /**
     * Type of transport.
     * Should return an identifier for the supported socket type (e.g. UNIX for unix socket, TCP for tcp sockets).
     *
     * @return String, never null
     */
    String getSupportedBusType();

    /**
     * Creates a new (dynamic) session for this transport.
     *
     * @param _listeningSocket true when listening address should be created
     *
     * @return String containing bus address
     */
    String createDynamicSessionAddress(boolean _listeningSocket);
}
