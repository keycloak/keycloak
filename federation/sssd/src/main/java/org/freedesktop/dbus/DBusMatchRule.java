package org.freedesktop.dbus;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.MethodReturn;
import org.freedesktop.dbus.utils.DBusNamingUtil;
import org.freedesktop.dbus.utils.Util;

/**
 * Defined a rule to match a message.<br>
 * This is mainly used to handle / take actions when signals arrive.
 */
public class DBusMatchRule {
    private static final Pattern IFACE_PATTERN = Pattern.compile(".*\\..*");
    private static final Map<String, Class<? extends DBusSignal>> SIGNALTYPEMAP = new ConcurrentHashMap<>();

    /** Equals operations used in {@link #matches(DBusMatchRule, boolean)} - do not change order! */
    private static final List<Function<DBusMatchRule, String>> MATCHRULE_EQUALS_OPERATIONS = List.of(
            x -> x.getInterface(),
            x -> x.getMember(),
            x -> x.getObject(),
            x -> x.getSource()
            );

    /** Equals operations used in {@link #matches(DBusSignal, boolean)} - do not change order! */
    private static final List<Function<DBusSignal, String>> SIGNAL_EQUALS_OPERATIONS = List.of(
            x -> x.getInterface(),
            x -> x.getName(),
            x -> x.getPath(),
            x -> x.getSource()
            );

    /* signal, error, method_call, method_reply */
    private final String                                          type;
    private final String                                          iface;
    private final String                                          member;
    private final String                                          object;
    private final String                                          source;

    public DBusMatchRule(String _type, String _iface, String _member) {
        this(_type, _iface, _member, null);
    }

    public DBusMatchRule(String _type, String _iface, String _member, String _object) {
        type = _type;
        iface = _iface;
        member = _member;
        object = _object;
        source = null;
    }

    public DBusMatchRule(DBusExecutionException _e) throws DBusException {
        this(_e.getClass());
    }

    public DBusMatchRule(Message _m) {
        iface = _m.getInterface();
        source = null;
        object = null;
        member = _m instanceof Error ? null : _m.getName();
        if (_m instanceof DBusSignal) {
            type = "signal";
        } else if (_m instanceof Error) {
            type = "error";
        } else if (_m instanceof MethodCall) {
            type = "method_call";
        } else if (_m instanceof MethodReturn) {
            type = "method_reply";
        } else {
            type = null;
        }
    }

    public DBusMatchRule(Class<? extends DBusInterface> _c, String _method) throws DBusException {
        this(_c, null, null, "method_call", _method);
    }

    @SuppressWarnings("unchecked")
    DBusMatchRule(Class<? extends Object> _c, String _source, String _object, String _type, String _member) throws DBusException {
        if (DBusInterface.class.isAssignableFrom(_c)) {
            iface = DBusNamingUtil.getInterfaceName(_c);
            assertDBusInterface(iface);

            member = _member != null ? _member : null;
            type = _type != null ? _type : null;
        } else if (DBusSignal.class.isAssignableFrom(_c)) {
            if (null == _c.getEnclosingClass()) {
                throw new DBusException("Signals must be declared as a member of a class implementing DBusInterface which is the member of a package.");
            }
            iface = DBusNamingUtil.getInterfaceName(_c.getEnclosingClass());
            // Don't export things which are invalid D-Bus interfaces
            assertDBusInterface(iface);

            member = _member != null ? _member : DBusNamingUtil.getSignalName(_c);
            SIGNALTYPEMAP.put(iface + '$' + member, (Class<? extends DBusSignal>) _c);
            type = _type != null ? _type : "signal";
        } else if (Error.class.isAssignableFrom(_c)) {
            iface = DBusNamingUtil.getInterfaceName(_c);
            assertDBusInterface(iface);
            member = _member != null ? _member : null;
            type = _type != null ? _type : "error";
        } else if (DBusExecutionException.class.isAssignableFrom(_c)) {
            iface = DBusNamingUtil.getInterfaceName(_c);
            assertDBusInterface(iface);
            member = _member != null ? _member : null;
            type = _type != null ? _type : "error";
        } else {
            throw new DBusException("Invalid type for match rule: " + _c);
        }

        source = _source;
        object = _object;
    }

    public DBusMatchRule(Class<? extends Object> _c, String _source, String _object) throws DBusException {
        this(_c, _source, _object, null, null);
    }

    public DBusMatchRule(Class<? extends Object> _c) throws DBusException {
        this(_c, null, null);
    }

    public static Class<? extends DBusSignal> getCachedSignalType(String _type) {
        return SIGNALTYPEMAP.get(_type);
    }

    void assertDBusInterface(String _str) throws DBusException {
        if (!IFACE_PATTERN.matcher(_str).matches()) {
            throw new DBusException("DBusInterfaces must be defined in a package.");
        }
    }

    /**
     * Checks if the given rule matches with our rule.
     * <p>
     * Method supports partial matching by setting strict to false.
     * Partial means that only the parts of this object are compared to the given
     * object which were set (non-null) on ourselves.
     * Fields set on the given object but not on ourselves will be ignored.
     * </p>
     *
     * @param _rule rule to compare
     * @param _strict true to get an exact match, false to allow partial matches
     *
     * @return true if matching
     */
    public boolean matches(DBusMatchRule _rule, boolean _strict) {
        if (_rule == null) {
            return false;
        }

        if (_strict) {
            return Util.strEquals(_rule.getInterface(), getInterface())
                    && Util.strEquals(_rule.getMember(), getMember())
                    && Util.strEquals(_rule.getObject(), getObject())
                    && Util.strEquals(_rule.getSource(), getSource());
        }

        String[] compareVals = new String[] {getInterface(), getMember(), getObject(), getSource()};

        for (int i = 0; i < compareVals.length; i++) {
            if (compareVals[i] == null) {
                continue;
            }
            Function<DBusMatchRule, String> function = MATCHRULE_EQUALS_OPERATIONS.get(i);
            if (!Util.strEquals(compareVals[i], function.apply(_rule))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the given signal matches with our rule.
     * <p>
     * Method supports partial matching by setting strict to false.
     * Partial means that only the parts of this rule are compared to the given
     * signal which were set (non-null) on ourselves.
     * Fields set on the given signal but not on ourselves will be ignored.
     * </p>
     *
     * @param _signal signal to compare
     * @param _strict true to get an exact match, false to allow partial matches
     *
     * @return true if matching
     */
    public boolean matches(DBusSignal _signal, boolean _strict) {
        if (_signal == null) {
            return false;
        }

       // _signal.getInterface(), _signal.getName(), _signal.getPath(), _signal.getSource()
        if (_strict) {
            return Util.strEquals(_signal.getInterface(), getInterface())
                    && Util.strEquals(_signal.getName(), getMember())
                    && Util.strEquals(_signal.getPath(), getObject())
                    && Util.strEquals(_signal.getSource(), getSource());
        }

        String[] compareVals = new String[] {getInterface(), getMember(), getObject(), getSource()};

        for (int i = 0; i < compareVals.length; i++) {
            if (compareVals[i] == null) {
                continue;
            }
            Function<DBusSignal, String> function = SIGNAL_EQUALS_OPERATIONS.get(i);
            if (!Util.strEquals(compareVals[i], function.apply(_signal))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        String s = null;
        if (null != type) {
            s = null == s ? "type='" + type + "'" : s + ",type='" + type + "'";
        }
        if (null != member) {
            s = null == s ? "member='" + member + "'" : s + ",member='" + member + "'";
        }
        if (null != iface) {
            s = null == s ? "interface='" + iface + "'" : s + ",interface='" + iface + "'";
        }
        if (null != source) {
            s = null == s ? "sender='" + source + "'" : s + ",sender='" + source + "'";
        }
        if (null != object) {
            s = null == s ? "path='" + object + "'" : s + ",path='" + object + "'";
        }
        return s;
    }

    @Override
    public int hashCode() {
        return Objects.hash(iface, member, object, source, type);
    }

    @Override
    public boolean equals(Object _obj) {
        if (this == _obj) {
            return true;
        }
        if (!(_obj instanceof DBusMatchRule)) {
            return false;
        }
        DBusMatchRule other = (DBusMatchRule) _obj;
        return Objects.equals(iface, other.iface) && Objects.equals(member, other.member)
                && Objects.equals(object, other.object) && Objects.equals(source, other.source)
                && Objects.equals(type, other.type);
    }

    public String getType() {
        return type;
    }

    public String getInterface() {
        return iface;
    }

    public String getMember() {
        return member;
    }

    public String getSource() {
        return source;
    }

    public String getObject() {
        return object;
    }

}
