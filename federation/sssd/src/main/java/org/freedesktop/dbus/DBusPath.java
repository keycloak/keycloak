package org.freedesktop.dbus;

import java.util.Objects;

public class DBusPath implements Comparable<DBusPath> {
    private final String source;
    private String path;

    public DBusPath(String _source, String _path) {
        source = _source;
        path = _path;
    }

    public DBusPath(String _path) {
        this(null, _path);
    }

    public String getPath() {
        return path;
    }

    /**
     * @deprecated path will be final in future, please do no longer use the setter.
     */
    @Deprecated(forRemoval = true, since = "5.1.1 - 2024-11-16")
    public void setPath(String _path) {
        path = _path;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return getPath();
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, path);
    }

    @Override
    public boolean equals(Object _obj) {
        if (this == _obj) {
            return true;
        } else if (_obj == null || getClass() != _obj.getClass()) {
            return false;
        }

        DBusPath dbusPath = (DBusPath) _obj;

        return Objects.equals(path, dbusPath.path)
            && Objects.equals(source, dbusPath.source);
    }

    @Override
    public int compareTo(DBusPath _that) {
        if (getPath() == null || _that == null) {
            return 0;
        }
        return getPath().compareTo(_that.getPath());
    }

    /**
     * Create a DBusPath object using one or multiple string parts.
     * Leading slash will automatically appended if missing.
     *
     * @param _parts parts to build DBusPath
     * @return DBusPath
     * @throws IllegalArgumentException when no parts are given
     */
    public static DBusPath of(String... _parts) {
        if (_parts == null || _parts.length == 0) {
            throw new IllegalArgumentException("No Strings given to build DBusPath");
        }

        String pathStr = _parts[0].indexOf('/') == 0 ? "" : "/" + String.join("/", _parts);

        return new DBusPath(pathStr);
    }
}
