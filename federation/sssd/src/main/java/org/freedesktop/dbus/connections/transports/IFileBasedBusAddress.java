package org.freedesktop.dbus.connections.transports;

import org.freedesktop.dbus.connections.BusAddress;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Interface which should be implemented by {@link BusAddress} subclasses which use
 * files as 'address' (e.g. unix sockets) and needs to set permission on those files.
 *
 * @author hypfvieh
 * @since 4.2.0 - 2022-07-18
 */
public interface IFileBasedBusAddress {

    void updatePermissions(String _fileOwner, String _fileGroup, Set<PosixFilePermission> _fileUnixPermissions);
}
