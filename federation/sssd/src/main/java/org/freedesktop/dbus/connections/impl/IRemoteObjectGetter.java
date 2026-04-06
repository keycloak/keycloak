package org.freedesktop.dbus.connections.impl;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.InvalidBusNameException;
import org.freedesktop.dbus.exceptions.InvalidObjectPathException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.utils.DBusObjects;

/**
 * Interface which contains all methods to retrieve any object from DBus.
 * @since 5.1.1 - 2024-11-16
 */
public interface IRemoteObjectGetter {

    default <I extends DBusInterface> I getPeerRemoteObject(String _busname, String _objectpath, Class<I> _type)
        throws DBusException {

        return getPeerRemoteObject(_busname, _objectpath, _type, true);
    }

    default <I extends DBusInterface> I getPeerRemoteObject(String _busname, DBusPath _objectpath, Class<I> _type)
        throws DBusException {
        DBusObjects.requireObjectPath(_objectpath);
        return getPeerRemoteObject(_busname, _objectpath.getPath(), _type);
    }

    /**
     * Return a reference to a remote object. This method will resolve the well known name (if given) to a unique bus
     * name when you call it. This means that if a well known name is released by one process and acquired by another
     * calls to objects gained from this method will continue to operate on the original process.
     *
     * This method will use bus introspection to determine the interfaces on a remote object and so <b>may block</b> and
     * <b>may fail</b>. The resulting proxy object will, however, be castable to any interface it implements. It will
     * also autostart the process if applicable. Also note that the resulting proxy may fail to execute the correct
     * method with overloaded methods and that complex types may fail in interesting ways. Basically, if something odd
     * happens, try specifying the interface explicitly.
     *
     * @param _busname
     *            The bus name to connect to. Usually a well known bus name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param _objectpath
     *            The path on which the process is exporting the object.$
     * @return A reference to a remote object.
     * @throws MissingInterfaceImplementationException
     *             If type is not a sub-type of DBusInterface
     * @throws InvalidObjectPathException
     *             When object path is invalid
     * @throws ClassOutsideOfPackageException
     *             When given type class has not package
     * @throws DBusException
     *             on any other errors
     */
    DBusInterface getPeerRemoteObject(String _busname, String _objectpath) throws InvalidBusNameException, DBusException;

    /**
     * Return a reference to a remote object. This method will always refer to the well known name (if given) rather
     * than resolving it to a unique bus name. In particular this means that if a process providing the well known name
     * disappears and is taken over by another process proxy objects gained by this method will make calls on the new
     * proccess.
     *
     * This method will use bus introspection to determine the interfaces on a remote object and so <b>may block</b> and
     * <b>may fail</b>. The resulting proxy object will, however, be castable to any interface it implements. It will
     * also autostart the process if applicable. Also note that the resulting proxy may fail to execute the correct
     * method with overloaded methods and that complex types may fail in interesting ways. Basically, if something odd
     * happens, try specifying the interface explicitly.
     *
     * @param _busname
     *            The bus name to connect to. Usually a well known bus name name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param _objectpath
     *            The path on which the process is exporting the object.
     * @return A reference to a remote object.
     * @throws MissingInterfaceImplementationException
     *             If type is not a sub-type of DBusInterface
     * @throws DBusException
     *             If remote object cannot be retrieved
     * @throws InvalidBusNameException
     *             If busname is incorrectly formatted
     * @throws InvalidObjectPathException
     *             If objectpath is incorrectly formatted
     */
    DBusInterface getRemoteObject(String _busname, String _objectpath) throws DBusException, InvalidBusNameException, InvalidObjectPathException;

    /**
     * @see #getRemoteObject(String, String)
     */
    default DBusInterface getRemoteObject(String _busname, DBusPath _objectpath) throws DBusException, InvalidBusNameException, InvalidObjectPathException {
        DBusObjects.requireBusNameOrConnectionId(_busname);
        DBusObjects.requireObjectPath(_objectpath);

        return getRemoteObject(_busname, _objectpath.toString());
    }

    /**
     * Return a reference to a remote object. This method will resolve the well known name (if given) to a unique bus
     * name when you call it. This means that if a well known name is released by one process and acquired by another
     * calls to objects gained from this method will continue to operate on the original process.
     *
     * @param <I>
     *            class extending {@link DBusInterface}
     * @param _busname
     *            The bus name to connect to. Usually a well known bus name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param _objectpath
     *            The path on which the process is exporting the object.$
     * @param _type
     *            The interface they are exporting it on. This type must have the same full class name and exposed
     *            method signatures as the interface the remote object is exporting.
     * @param _autostart
     *            Disable/Enable auto-starting of services in response to calls on this object. Default is enabled; when
     *            calling a method with auto-start enabled, if the destination is a well-known name and is not owned the
     *            bus will attempt to start a process to take the name. When disabled an error is returned immediately.
     * @return A reference to a remote object.
     * @throws MissingInterfaceImplementationException
     *             If type is not a sub-type of DBusInterface
     * @throws InvalidObjectPathException
     *             When object path is invalid
     * @throws ClassOutsideOfPackageException
     *             When given type class has not package
     * @throws DBusException
     *             on any other errors
     */
    default <I extends DBusInterface> I getPeerRemoteObject(String _busname, String _objectpath, Class<I> _type,
        boolean _autostart) throws DBusException {

        DBusObjects.requireBusNameOrConnectionId(_busname);
        return getRemoteObject(getDBusOwnerName(_busname), _objectpath, _type, _autostart);
    }

    /**
     * @see #getPeerRemoteObject(String, String, Class, boolean)
     */
    default <I extends DBusInterface> I getPeerRemoteObject(String _busname, DBusPath _objectpath, Class<I> _type,
        boolean _autostart) throws DBusException {

        DBusObjects.requireBusNameOrConnectionId(_busname);
        DBusObjects.requireObjectPath(_objectpath);
        return getRemoteObject(getDBusOwnerName(_busname), _objectpath.getPath(), _type, _autostart);
    }

    /**
     * Return a reference to a remote object. This method will always refer to the well known name (if given) rather
     * than resolving it to a unique bus name. In particular this means that if a process providing the well known name
     * disappears and is taken over by another process proxy objects gained by this method will make calls on the new
     * proccess.
     *
     * @param <I>
     *            class extending {@link DBusInterface}
     * @param _busname
     *            The bus name to connect to. Usually a well known bus name name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param _objectpath
     *            The path on which the process is exporting the object.
     * @param _type
     *            The interface they are exporting it on. This type must have the same full class name and exposed
     *            method signatures as the interface the remote object is exporting.
     * @return A reference to a remote object.
     * @throws MissingInterfaceImplementationException
     *             If type is not a sub-type of DBusInterface
     * @throws InvalidObjectPathException
     *             When object path is invalid
     * @throws ClassOutsideOfPackageException
     *             When given type class has not package
     * @throws DBusException
     *             on any other errors
     */
    default <I extends DBusInterface> I getRemoteObject(String _busname, String _objectpath, Class<I> _type)
        throws DBusException {
        return getRemoteObject(_busname, _objectpath, _type, true);
    }

    /**
     * @see #getRemoteObject(String, String, Class)
     */
    default <I extends DBusInterface> I getRemoteObject(String _busname, DBusPath _objectpath, Class<I> _type)
        throws DBusException {
        DBusObjects.requireObjectPath(_objectpath);
        return getRemoteObject(_busname, _objectpath.getPath(), _type, true);
    }
    /**
     * Return a reference to a remote object. This method will always refer to the well known name (if given) rather
     * than resolving it to a unique bus name. In particular this means that if a process providing the well known name
     * disappears and is taken over by another process proxy objects gained by this method will make calls on the new
     * process.
     *
     * @param <I>
     *            class extending {@link DBusInterface}
     * @param _busname
     *            The bus name to connect to. Usually a well known bus name name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param _objectpath
     *            The path on which the process is exporting the object.
     * @param _type
     *            The interface they are exporting it on. This type must have the same full class name and exposed
     *            method signatures as the interface the remote object is exporting.
     * @param _autostart
     *            Disable/Enable auto-starting of services in response to calls on this object. Default is enabled; when
     *            calling a method with auto-start enabled, if the destination is a well-known name and is not owned the
     *            bus will attempt to start a process to take the name. When disabled an error is returned immediately.
     * @return A reference to a remote object.
     * @throws MissingInterfaceImplementationException
     *             If type is not a sub-type of DBusInterface
     * @throws InvalidObjectPathException
     *             When object path is invalid
     * @throws ClassOutsideOfPackageException
     *             When given type class has not package
     * @throws DBusException
     *             on any other errors
     */
    <I extends DBusInterface> I getRemoteObject(String _busname, String _objectpath, Class<I> _type,
        boolean _autostart) throws DBusException;

    /**
     * @see #getRemoteObject(String, String, Class, boolean)
     */
    default <I extends DBusInterface> I getRemoteObject(String _busname, DBusPath _objectpath, Class<I> _type,
        boolean _autostart) throws DBusException {
        DBusObjects.requireObjectPath(_objectpath);
        return getRemoteObject(_busname, _objectpath.getPath(), _type, _autostart);
    }

    /**
     * Returns name of the current owning dbus session.
     * @param _busName bus name
     * @return String or null
     */
    String getDBusOwnerName(String _busName);
}
