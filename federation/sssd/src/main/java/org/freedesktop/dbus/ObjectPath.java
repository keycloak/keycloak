package org.freedesktop.dbus;

public class ObjectPath extends DBusPath {
    private String source;

    public ObjectPath(String _source, String _path) {
        super(_path);
        this.source = _source;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String _source) {
        source = _source;
    }

}
