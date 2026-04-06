package org.freedesktop.dbus.matchrules;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Error;
import org.freedesktop.dbus.messages.constants.MessageTypes;
import org.freedesktop.dbus.utils.DBusNamingUtil;
import org.freedesktop.dbus.utils.DBusObjects;

/**
 * Builder to configure a {@link DBusMatchRule}.
 *
 * @author hypfvieh
 * @since 5.2.0 - 2025-05-01
 */
public final class DBusMatchRuleBuilder {

    private final Map<MatchRuleField, String> values = new LinkedHashMap<>();
    private final Map<MatchRuleField, Map<Integer, String>> multiValueFields = new LinkedHashMap<>();

    private DBusMatchRuleBuilder() {
    }

    /**
     * Set sender filter.
     * <br>
     * <p>
     * <b>Possible values:</b> A bus (e.g. org.freedesktop.Hal) or unique name (e.g. :1.2)
     * </p>
     * @param _sender sender to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withSender(String _sender) {
        return putOrRemove(MatchRuleField.SENDER, _sender);
    }

    /**
     * Set destination filter.
     * <br><br>
     * <b>DBus Specification Quote:</b>
     * <p>
     * Matches messages which are being sent to the given unique name.
     * An example of a destination match is destination=':1.0'
     * </p>
     * @param _destination destination to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withDestination(String _destination) {
        return putOrRemove(MatchRuleField.DESTINATION, _destination);
    }

    /**
     * Set path filter.
     * <br><br>
     * <b>DBus Specification Quote:</b>
     * <p>
     * Matches messages which are sent from or to the given object.
     * An example of a path match is <code>path='/org/freedesktop/Hal/Manager'</code>
     * </p>
     * @param _path path to filter, {@code null} to remove
     * @return this
     * @throws IllegalArgumentException when path_namespace already set
     */
    public DBusMatchRuleBuilder withPath(String _path) {
        if (values.get(MatchRuleField.PATH_NAMESPACE) != null && _path != null) {
            throw new IllegalArgumentException("Path and PathNamespace cannot be set at the same time");
        }

        return putOrRemove(MatchRuleField.PATH, _path);
    }

    /**
     * Set interface filter.
     * <br><br>
     * <b>DBus Specification Quote:</b>
     * <p>
     * Match messages sent over or to a particular interface.
     * An example of an interface match is interface='org.freedesktop.Hal.Manager'.
     * If a message omits the interface header, it must not match any rule that specifies this key.
     * </p>
     * @param _interface interface to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withInterface(String _interface) {
        return putOrRemove(MatchRuleField.INTERFACE, _interface);
    }

    /**
     * Set member/name filter.
     * <br><br>
     * <b>DBus Specification Quote:</b>
     * <p>
     * Matches messages which have the give method or signal name.
     * An example of a member match is member='NameOwnerChanged'
     * </p>
     * @param _member member/name to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withMember(String _member) {
        return putOrRemove(MatchRuleField.MEMBER, _member);
    }

    /**
     * Set message type filter.
     * <br>
     * <p>
     * <b>Possible values:</b> 'signal', 'method_call', 'method_return', 'error'
     * </p>
     * <b>DBus Specification Quote:</b>
     * <p>
     * Match on the message type.
     * An example of a type match is type='signal'
     * </p>
     * @param _type message type to filter, {@code null} to remove
     * @return this
     * @throws IllegalArgumentException when invalid message type is given
     */
    public DBusMatchRuleBuilder withType(String _type) {
        if (Stream.of(MessageTypes.values())
            .map(e -> e.getMatchRuleName())
            .noneMatch(e -> e.equals(_type))) {
            throw new IllegalArgumentException(_type + " is not a valid message type");
        }
        return putOrRemove(MatchRuleField.TYPE, _type);
    }

    /**
     * Set type filter using {@link MessageTypes} enum.
     * @param _type type to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withType(MessageTypes _type) {
        if (_type == null) {
            values.remove(MatchRuleField.TYPE);
        } else {
            values.put(MatchRuleField.TYPE, _type.getMatchRuleName());
        }
        return this;
    }

    /**
     * Set message type filter using class.
     * <p>
     * Class must be a DBusInterface compatible or Error/DBusSignal class.
     * </p>
     * @param _clz class to use for setting up filter, {@code null} to remove
     * @return this
     * @throws DBusException when invalid class s given
     */
    @SuppressWarnings("unchecked")
    public DBusMatchRuleBuilder withType(Class<?> _clz) throws DBusException {
        if (DBusInterface.class.isAssignableFrom(_clz)) {
            putOrRemove(MatchRuleField.INTERFACE, DBusObjects.requireDBusInterface(DBusNamingUtil.getInterfaceName(_clz)));
        } else if (DBusSignal.class.isAssignableFrom(_clz)) {
            if (_clz.getEnclosingClass() == null) {
                throw new DBusException("Signals must be declared as a member of a class implementing DBusInterface which is the member of a package.");
            }
            // Don't export things which are invalid D-Bus interfaces
            String interfaceName = DBusObjects.requireDBusInterface(DBusNamingUtil.getInterfaceName(_clz.getEnclosingClass()));
            String signalName = DBusNamingUtil.getSignalName(_clz);

            DBusMatchRule.addToTypeMap(interfaceName + '$' + signalName, (Class<? extends DBusSignal>) _clz);

            putOrRemove(MatchRuleField.INTERFACE, interfaceName);
            putOrRemove(MatchRuleField.MEMBER, signalName);
            putOrRemove(MatchRuleField.TYPE, MessageTypes.SIGNAL.getMatchRuleName());
        } else if (Error.class.isAssignableFrom(_clz) || DBusExecutionException.class.isAssignableFrom(_clz)) {
            putOrRemove(MatchRuleField.INTERFACE, DBusObjects.requireDBusInterface(DBusNamingUtil.getInterfaceName(_clz)));
            putOrRemove(MatchRuleField.TYPE, MessageTypes.ERROR.getMatchRuleName());
        } else {
            throw new DBusException("Invalid type for match rule: " + _clz);
        }

        return this;
    }

    /**
     * Set/add argument filter.
     * <br><br>
     * <b>DBus Specification Quote:</b>
     * <p>
     * Arg matches are special and are used for further restricting the match based on the
     * arguments in the body of a message. Only arguments of type STRING can be matched in this way.
     * An example of an argument match would be arg3='Foo'.
     * Only argument indexes from 0 to 63 should be accepted.
     * </p>
     * @param _argId argument number
     * @param _arg0123 argument value to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withArg0123(int _argId, String _arg0123) {
        return withArgX(MatchRuleField.ARG0123, _argId, _arg0123);
    }

    /**
     * Set argument path filter.
     * <br><br>
     * <b>DBus Specification Quote:</b>
     * <p>
     * Argument path matches provide a specialised form of wildcard matching for path-like namespaces.
     * They can match arguments whose type is either STRING or OBJECT_PATH. As with normal argument matches,
     * if the argument is exactly equal to the string given in the match rule then the rule is satisfied.
     * Additionally, there is also a match when either the string given in the match rule or the
     * appropriate message argument ends with '/' and is a prefix of the other.
     * An example argument path match is arg0path='/aa/bb/'.
     * This would match messages with first arguments of '/', '/aa/', '/aa/bb/', '/aa/bb/cc/'
     * and '/aa/bb/cc'.
     * It would not match messages with first arguments of '/aa/b', '/aa' or even '/aa/bb'.
     * <br><br>
     * This is intended for monitoring “directories” in file system-like hierarchies,
     * as used in the dconf configuration system. An application interested in all nodes
     * in a particular hierarchy would monitor arg0path='/ca/example/foo/'.
     * Then the service could emit a signal with zeroth argument "/ca/example/foo/bar"
     * to represent a modification to the “bar” property, or a signal with zeroth
     * argument "/ca/example/" to represent atomic modification of many properties within
     * that directory, and the interested application would be notified in both cases.
     * </p>
     * @param _argId argument number
     * @param _arg0123Path argument value to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withArg0123Path(int _argId, String _arg0123Path) {
        return withArgX(MatchRuleField.ARG0123PATH, _argId, _arg0123Path);
    }

    /**
     * Set arg0Namespace filter.
     * <br><br>
     * <b>DBus Specification Quote:</b>
     * <p>
     * Match messages whose first argument is of type STRING, and is a bus name or
     * interface name within the specified namespace. This is primarily intended for
     * watching name owner changes for a group of related bus names,
     * rather than for a single name or all name changes.
     * <br><br>
     * Because every valid interface name is also a valid bus name, this can also be used
     * for messages whose first argument is an interface name.
     * <br><br>
     * For example, the match rule <code>member='NameOwnerChanged',arg0namespace='com.example.backend1'</code>
     * matches name owner changes for bus names such as
     * <code>com.example.backend1.foo, com.example.backend1.foo.bar</code>, and <code>com.example.backend1</code> itself.
     * </p>
     * @param _arg0Namespace to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withArg0Namespace(String _arg0Namespace) {
        return putOrRemove(MatchRuleField.ARG0NAMESPACE, _arg0Namespace);
    }

    /**
     * Set path namespace filter.
     * <br><br>
     * <b>DBus Specification Quote:</b>
     * <p>
     * Matches messages which are sent from or to an object for which the object path
     * is either the given value, or that value followed by one or more path components.
     * <br><br>
     * For example, <code>path_namespace='/com/example/foo'</code> would match signals sent
     * by <code>/com/example/foo</code> or by <code>/com/example/foo/bar</code>,
     * but not by <code>/com/example/foobar</code>.
     * <br><br>
     * <b>Using both path and path_namespace in the same match rule is not allowed.</b>
     * </p>
     * @param _pathNamespace to filter, {@code null} to remove
     * @return this
     * @throws IllegalArgumentException when path already set
     */
    public DBusMatchRuleBuilder withPathNamespace(String _pathNamespace) {
        if (values.get(MatchRuleField.PATH) != null && _pathNamespace != null) {
            throw new IllegalArgumentException("Path and PathNamespace cannot be set at the same time");
        }
        return putOrRemove(MatchRuleField.PATH_NAMESPACE, _pathNamespace);
    }

    /**
     * Create a new {@link DBusMatchRule} using the configured values of this builder.
     * @return {@link DBusMatchRule}
     * @throws IllegalStateException when no rule was defined
     */
    public DBusMatchRule build() {
        if (values.isEmpty() && multiValueFields.isEmpty()) {
            throw new IllegalStateException("No rules defined");
        }
        return new DBusMatchRule(values, multiValueFields);
    }

    /**
     * Set or remove a value for a given field.
     * @param _field field to set
     * @param _val value to set or {@code null} to remove
     * @return this
     */
    private DBusMatchRuleBuilder putOrRemove(MatchRuleField _field, String _val) {
        if (_val == null) {
            values.remove(_field);
        } else {
            values.put(_field, _val);
        }
        return this;
    }

    /**
     * Adds or remove a value of a field at index.
     * @param _field field to add value to
     * @param _argId argument id to add/remove
     * @param _argValue value to set or {@code null} to remove
     * @return this
     */
    private DBusMatchRuleBuilder withArgX(MatchRuleField _field, int _argId, String _argValue) {
        if (_argId > 63 || _argId < 0) {
            throw new IllegalArgumentException("ArgId must be between 0 and 63");
        }

        Map<Integer, String> map = multiValueFields.get(_field);
        if (map != null) {
            if (_argValue == null) {
                map.remove(_argId);
            } else {
                map.put(_argId, _argValue);
            }

            if (map.isEmpty()) {
                multiValueFields.remove(_field);
            }
        } else {
            if (_argValue == null) {
                return this;
            } else {
                multiValueFields.computeIfAbsent(_field, x -> new LinkedHashMap<>()).put(_argId, _argValue);
            }
        }

        return this;
    }

    /**
     * Creates a new builder instance.
     * @return new instance
     */
    public static DBusMatchRuleBuilder create() {
        return new DBusMatchRuleBuilder();
    }

    /**
     * Creates a DBusMatchRule from a map.
     * @param _keyValues values to convert
     * @return DBusMatchRule or {@code null} if input {@code null}
     */
    DBusMatchRule fromMap(Map<String, String> _keyValues) {
        if (_keyValues == null) {
            return null;
        }
        Map<String, String> copy = new HashMap<>(_keyValues);

        Map<String, MatchRuleField> names = Stream.of(MatchRuleField.values())
            .collect(Collectors.toMap(e -> e.name().toLowerCase(Locale.US), e -> e));

        names.forEach((k, v) -> {
            String val = copy.remove(k);
            if (val != null) {
                values.putIfAbsent(v, val);
            }
        });

        if (!copy.isEmpty()) {
            Pattern argPattern = Pattern.compile("^arg([0-9]{1,2})$");
            Pattern argPathPattern = Pattern.compile("^arg([0-9]{1,2})path$");
            for (Entry<String, String> e : copy.entrySet()) {
                Matcher argMatcher = argPattern.matcher(e.getKey());
                Matcher argPathMatcher = argPathPattern.matcher(e.getKey());
                if (argMatcher.matches()) {
                    multiValueFields
                        .computeIfAbsent(MatchRuleField.ARG0123, x -> new LinkedHashMap<>())
                        .putIfAbsent(Integer.valueOf(argMatcher.group(1)), e.getValue());
                }
                if (argPathMatcher.matches()) {
                    multiValueFields
                        .computeIfAbsent(MatchRuleField.ARG0123PATH, x -> new LinkedHashMap<>())
                        .putIfAbsent(Integer.valueOf(argMatcher.group(1)), e.getValue());
                }
            }
        }

        return build();
    }
}
