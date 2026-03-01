package org.freedesktop.dbus.transport.jre;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.IFileBasedBusAddress;
import org.freedesktop.dbus.utils.Util;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class UnixBusAddress extends BusAddress implements IFileBasedBusAddress {

    public UnixBusAddress(BusAddress _obj) {
        super(_obj);
    }

    public boolean hasPath() {
        return hasParameter("path");
    }

    public String getPath() {
        return getParameterValue("path");
    }

    @Override
    public void updatePermissions(String _fileOwner, String _fileGroup, Set<PosixFilePermission> _fileUnixPermissions) {
        Util.setFilePermissions(Path.of(getPath()), _fileOwner, _fileGroup, _fileUnixPermissions);
    }

}
