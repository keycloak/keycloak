package org.freedesktop.dbus.connections;

import org.freedesktop.dbus.errors.UnknownObject;
import org.freedesktop.dbus.messages.ExportedObject;

public class GlobalHandler implements org.freedesktop.dbus.interfaces.Peer, org.freedesktop.dbus.interfaces.Introspectable {
    /**
     *
     */
    private final AbstractConnection connection;
    private final String             objectpath;

    GlobalHandler(AbstractConnection _abstractConnection) {
        connection = _abstractConnection;
        this.objectpath = null;
    }

    GlobalHandler(AbstractConnection _abstractConnection, String _objectpath) {
        connection = _abstractConnection;
        this.objectpath = _objectpath;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public void Ping() {
        // nothing to do
    }

    @Override
    public String Introspect() {
        String intro = connection.getObjectTree().Introspect(objectpath);
        if (null == intro) {
            ExportedObject eo = connection.getFallbackContainer().get(objectpath);
            if (null != eo) {
                intro = eo.getIntrospectiondata();
            }
        }
        if (null == intro) {
            throw new UnknownObject("Introspecting on non-existent object");
        } else {
            return "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\" "
                    + "\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">\n" + intro;
        }
    }

    @Override
    public String getObjectPath() {
        return objectpath;
    }

    @Override
    public String GetMachineId() {
        return connection.getMachineId();
    }
}
