package org.freedesktop.dbus.interfaces;

/**
 * Denotes a class as exportable or a remote interface which can be called.
 * <p>
 * Any interface which should be exported or imported should extend this interface. All public methods from that
 * interface are exported/imported with the given method signatures.
 * </p>
 * <p>
 * All method calls on exported objects are run in their own threads. Application writers are responsible for any
 * concurrency issues.
 * </p>
 */
public interface DBusInterface {

    /**
     * Returns true on remote objects. Local objects implementing this interface MUST return false.
     *
     * @return boolean
     */
    default boolean isRemote() {
        return false;
    }

    /**
     * Returns the path of this object.
     *
     * @return string
     */
    String getObjectPath();

}
