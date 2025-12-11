package org.freedesktop.dbus.connections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add addresses of peers to a set which will watch for them to
 * disappear and automatically remove them from the set.
 */
public class PeerSet implements Set<String>, DBusSigHandler<DBus.NameOwnerChanged> {
    private final Logger      logger = LoggerFactory.getLogger(getClass());
    private final Set<String> addresses;

    public PeerSet(DBusConnection _connection) {
        addresses = new TreeSet<>();
        try {
            _connection.addSigHandler(new DBusMatchRule(DBus.NameOwnerChanged.class, null, null), this);
        } catch (DBusException _ex) {
            logger.debug("", _ex);
        }
    }

    @Override
    public void handle(DBus.NameOwnerChanged _noc) {
        logger.debug("Received NameOwnerChanged({}, {}, {})", _noc.name, _noc.oldOwner, _noc.newOwner);
        if ("".equals(_noc.newOwner) && addresses.contains(_noc.name)) {
            remove(_noc.name);
        }
    }

    @Override
    public boolean add(String _address) {
        logger.debug("Adding {}", _address);
        synchronized (addresses) {
            return addresses.add(_address);
        }
    }

    @Override
    public boolean addAll(Collection<? extends String> _addresses) {
        synchronized (this.addresses) {
            return this.addresses.addAll(_addresses);
        }
    }

    @Override
    public void clear() {
        synchronized (addresses) {
            addresses.clear();
        }
    }

    @Override
    public boolean contains(Object _o) {
        return addresses.contains(_o);
    }

    @Override
    public boolean containsAll(Collection<?> _os) {
        return addresses.containsAll(_os);
    }

    @Override
    public boolean equals(Object _o) {
        if (_o instanceof PeerSet) {
            return ((PeerSet) _o).addresses.equals(addresses);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return addresses.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return addresses.isEmpty();
    }

    @Override
    public Iterator<String> iterator() {
        return addresses.iterator();
    }

    @Override
    public boolean remove(Object _o) {
        logger.debug("Removing {}", _o);
        synchronized (addresses) {
            return addresses.remove(_o);
        }
    }

    @Override
    public boolean removeAll(Collection<?> _os) {
        synchronized (addresses) {
            return addresses.removeAll(_os);
        }
    }

    @Override
    public boolean retainAll(Collection<?> _os) {
        synchronized (addresses) {
            return addresses.retainAll(_os);
        }
    }

    @Override
    public int size() {
        return addresses.size();
    }

    @Override
    public Object[] toArray() {
        synchronized (addresses) {
            return addresses.toArray();
        }
    }

    @Override
    public <T> T[] toArray(T[] _a) {
        synchronized (addresses) {
            return addresses.toArray(_a);
        }
    }
}
