package org.freedesktop.dbus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.matchrules.DBusMatchRuleBuilder;
import org.freedesktop.dbus.matchrules.MatchRuleField;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Error;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.constants.MessageTypes;
import org.freedesktop.dbus.utils.DBusNamingUtil;
import org.freedesktop.dbus.utils.DBusObjects;
import org.freedesktop.dbus.utils.Util;

/**
 * Defined a rule to match a message.<br>
 * This is mainly used to handle / take actions when signals arrive.
 *
 * @deprecated use {@link DBusMatchRuleBuilder}
 */
@Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-03")
public final class DBusMatchRule extends org.freedesktop.dbus.matchrules.DBusMatchRule {

    private static final Map<String, Class<? extends DBusSignal>> SIGNALTYPEMAP = new ConcurrentHashMap<>();

    private static final List<Function<DBusMatchRule, String>> MATCHRULE_EQUALS_OPERATIONS = List.of(
        DBusMatchRule::getInterface,
        DBusMatchRule::getMember,
        DBusMatchRule::getPath,
        DBusMatchRule::getSender,
        DBusMatchRule::getDestination
    );

    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
    public DBusMatchRule(String _type, String _iface, String _member) {
        this(_type, _iface, _member, null);
    }

    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
    public DBusMatchRule(String _type, String _iface, String _member, String _path) {
        this(_type, _iface, _member, _path, null);
    }

    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
    public DBusMatchRule(String _type, String _iface, String _member, String _path, String _destination) {
        super(Stream.of(MatchRuleField.TYPE.entryOf(_type),
            MatchRuleField.INTERFACE.entryOf(_iface),
            MatchRuleField.MEMBER.entryOf(_member),
            MatchRuleField.PATH.entryOf(_path),
            MatchRuleField.DESTINATION.entryOf(_destination)
            )
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())), null);
    }

    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
    public DBusMatchRule(DBusExecutionException _e) throws DBusException {
        this(_e.getClass());
    }

    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
    public DBusMatchRule(Message _m) {
        super(Stream.of(
            MatchRuleField.INTERFACE.entryOf(_m.getInterface()),
            MatchRuleField.MEMBER.entryOf(_m instanceof Error ? null : _m.getName()),
            MatchRuleField.TYPE.entryOf(MessageTypes.getRuleNameById(_m.getType())),
            MatchRuleField.DESTINATION.entryOf(_m.getDestination()))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())), null);
    }

    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
    public DBusMatchRule(Class<? extends DBusInterface> _c, String _method) throws DBusException {
        this(_c, null, null, MessageTypes.METHOD_CALL.getMatchRuleName(), _method);
    }

    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
    DBusMatchRule(Class<?> _c, String _sender, String _path, String _type, String _member) throws DBusException {
        super(legacyCompat(_c, _sender, _path, _type, _member), null);
    }

    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
    public DBusMatchRule(Class<?> _c, String _source, String _object) throws DBusException {
        this(_c, _source, _object, null, null);
    }

    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
    public DBusMatchRule(Class<?> _c) throws DBusException {
        this(_c, null, null);
    }

    @SuppressWarnings("unchecked")
    static Map<MatchRuleField, String> legacyCompat(Class<?> _c, String _sender, String _path, String _type, String _member) throws DBusException {
        Map<MatchRuleField, String> values = new LinkedHashMap<>();
        if (DBusInterface.class.isAssignableFrom(_c)) {
            values.put(MatchRuleField.INTERFACE, DBusObjects.requireDBusInterface(DBusNamingUtil.getInterfaceName(_c)));
            values.put(MatchRuleField.MEMBER, _member);
            values.put(MatchRuleField.TYPE, _type);
        } else if (DBusSignal.class.isAssignableFrom(_c)) {
            if (null == _c.getEnclosingClass()) {
                throw new DBusException("Signals must be declared as a member of a class implementing DBusInterface which is the member of a package.");
            }
            // Don't export things which are invalid D-Bus interfaces
            String interfaceName = DBusObjects.requireDBusInterface(DBusNamingUtil.getInterfaceName(_c.getEnclosingClass()));
            values.put(MatchRuleField.INTERFACE, interfaceName);
            String member = _member != null ? _member : DBusNamingUtil.getSignalName(_c);
            values.put(MatchRuleField.MEMBER, member);
            SIGNALTYPEMAP.put(interfaceName + '$' + member, (Class<? extends DBusSignal>) _c);
            values.put(MatchRuleField.TYPE, _type != null ? _type : MessageTypes.SIGNAL.getMatchRuleName());
        } else if (Error.class.isAssignableFrom(_c) || DBusExecutionException.class.isAssignableFrom(_c)) {
            values.put(MatchRuleField.INTERFACE, DBusObjects.requireDBusInterface(DBusNamingUtil.getInterfaceName(_c)));
            values.put(MatchRuleField.MEMBER, _member);
            values.put(MatchRuleField.TYPE, _type != null ? _type : MessageTypes.ERROR.getMatchRuleName());
        } else {
            throw new DBusException("Invalid type for match rule: " + _c);
        }

        values.put(MatchRuleField.SENDER, _sender);
        values.put(MatchRuleField.PATH, _path);

        return values;
    }

    /**
     * @deprecated use {@link org.freedesktop.dbus.matchrules.DBusMatchRule#getCachedSignalType(String)}
     */
    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-03")
    public static Class<? extends DBusSignal> getCachedSignalType(String _type) {
        return org.freedesktop.dbus.matchrules.DBusMatchRule.getCachedSignalType(_type);
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
     * @param _strict deprecated, no longer used, will be removed in future
     *
     * @return true if matching
     *
     * @deprecated using a MatchRule to match against a MatchRule makes no sense,
     *  therefore this method will be removed without any replacement
     */
    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
    public boolean matches(DBusMatchRule _rule, @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01") boolean _strict) {
        if (_rule == null) {
            return false;
        }

        String[] compareVals = new String[] {getInterface(), getMember(), getPath(), getSender()};

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
     * @param _strict deprecated, no longer used, will be removed in future
     *
     * @return true if matching
     */
    public boolean matches(DBusSignal _signal, @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01") boolean _strict) {
        return super.matches(_signal);
    }

    /**
     * @deprecated use {@link #getSender()}
     * @return sender
     */
    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
    public String getSource() {
        return getSender();
    }

    /**
     * @deprecated use {@link #getPath()}
     * @return path
     */
    @Deprecated(forRemoval = true, since = "5.2.0 - 2025-05-01")
    public String getObject() {
        return getPath();
    }

}
