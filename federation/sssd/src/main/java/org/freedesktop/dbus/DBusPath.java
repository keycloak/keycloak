package org.freedesktop.dbus;

public class DBusPath implements Comparable<DBusPath> {
    private String path;

    public DBusPath(String _path) {
        this.setPath(_path);
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return getPath();
    }

    @Override
    public boolean equals(Object _other) {
        return _other instanceof DBusPath && getPath().equals(((DBusPath) _other).getPath());
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    @Override
    public int compareTo(DBusPath _that) {
        return getPath().compareTo(_that.getPath());
    }

    public void setPath(String _path) {
        path = _path;
    }
}
