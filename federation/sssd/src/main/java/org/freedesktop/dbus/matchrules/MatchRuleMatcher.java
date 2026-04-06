package org.freedesktop.dbus.matchrules;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.utils.DBusObjects;
import org.freedesktop.dbus.utils.Util;

final class MatchRuleMatcher {

    private MatchRuleMatcher() {

    }

    /**
     * Matcher for arg0...argX matcher of DBus rules.<br>
     * <br><b>DBus Specification Quote:</b>
     * <p>
     * Arg matches are special and are used for further restricting the match based on the arguments
     * in the body of a message. Only arguments of type STRING can be matched in this way.
     * An example of an argument match would be arg3='Foo'. Only argument indexes from 0 to 63 should be accepted.
     * </p>
     * @param _msg message to match on
     * @param _compare compare values where map key is argument index
     * @return true if matching false otherwise
     * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#message-bus-routing-match-rules">DBus Specification</a>
     */
    static boolean matchArg0123(Message _msg, Map<Integer, String> _compare) {
        if (_msg == null || _compare == null || _compare.isEmpty()) {
            return false;
        }

        try {
            List<Type> dataType = new ArrayList<>();
            Marshalling.getJavaType(_msg.getSig(), dataType, -1);

            if (dataType.isEmpty()) {
                return false;
            }

            Object[] parameters = _msg.getParameters();

            for (int i = 0; i < parameters.length; i++) {
                if (!_compare.containsKey(i)) {
                    continue;
                }
                if (dataType.get(i) instanceof Class<?> clz && clz.isAssignableFrom(String.class)) {
                    String compareVal = _compare.get(i);
                    return compareVal == parameters[i] || compareVal.equals(parameters[i]);
                }
            }
        } catch (DBusException _ex) {
            throw new DBusExecutionException("Unable to get parameters from signal", _ex);
        }

        return false;
    }

    /**
     * Matcher for arg0Path...argXPath matcher of DBus rules.<br>
     * <br><b>DBus Specification Quote:</b>
     * <p>
     * Argument path matches provide a specialised form of wildcard matching for path-like namespaces.
     * They can match arguments whose type is either STRING or OBJECT_PATH. As with normal argument matches,
     * if the argument is exactly equal to the string given in the match rule then the rule is satisfied.
     * Additionally, there is also a match when either the string given in the match rule or the appropriate
     * message argument ends with '/' and is a prefix of the other. An example argument path match is
     * arg0path='/aa/bb/'. This would match messages with first arguments of '/', '/aa/', '/aa/bb/',
     * '/aa/bb/cc/' and '/aa/bb/cc'. It would not match messages with first arguments of '/aa/b',
     * '/aa' or even '/aa/bb'. <br><br>
     * This is intended for monitoring “directories” in file system-like hierarchies,
     * as used in the dconf configuration system. An application interested in all nodes in a particular
     * hierarchy would monitor arg0path='/ca/example/foo/'. Then the service could emit a signal
     * with zeroth argument "/ca/example/foo/bar" to represent a modification to the “bar” property,
     * or a signal with zeroth argument "/ca/example/" to represent atomic modification of many properties
     * within that directory, and the interested application would be notified in both cases.
     * </p>
     * @param _msg message to match on
     * @param _compare compare values where map key is argument index
     * @return true if matching false otherwise
     * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#message-bus-routing-match-rules">DBus Specification</a>
     */
    static boolean matchArg0123Path(Message _msg, Map<Integer, String> _compare) {
        if (_msg == null || _compare == null || _compare.isEmpty()) {
            return false;
        }

        try {
            List<Type> dataType = new ArrayList<>();
            Marshalling.getJavaType(_msg.getSig(), dataType, -1);
            if (dataType.isEmpty()) {
                return false;
            }

            Object[] parameters = _msg.getParameters();

            for (int i = 0; i < parameters.length; i++) {
                if (!_compare.containsKey(i)) {
                    continue;
                }
                if (dataType.get(i) instanceof Class<?> clz) {
                    String matchVal;
                    if (clz.isAssignableFrom(String.class)) {
                        matchVal = (String) parameters[i];
                    } else if (clz.isAssignableFrom(DBusPath.class)) {
                        matchVal = ((DBusPath) parameters[i]).getPath();
                    } else {
                        continue; // not String or DBusPath, do not try to match
                    }
                    String compareVal = _compare.get(i);
                    return compareVal == matchVal || matchesArg0Path(matchVal, compareVal);
                }
            }
        } catch (DBusException _ex) {
            throw new DBusExecutionException("Unable to get parameters from signal", _ex);
        }

        return false;
    }

    private static boolean matchesArg0Path(String _matchVal, String _compareVal) {
        return "/".equals(_compareVal) || _matchVal.startsWith(_compareVal);
    }

    /**
     * Matcher for path_namespace match rule.
     * <br><b>DBus Specification Quote:</b>
     * <p>
     * Matches messages which are sent from or to an object for which the object path is either the given value,
     * or that value followed by one or more path components.<br><br>
     * For example, path_namespace='/com/example/foo' would match signals sent
     * by /com/example/foo or by /com/example/foo/bar, but not by /com/example/foobar.<br><br>
     * Using both path and path_namespace in the same match rule is not allowed.
     * </p>
     * @param _input signal sender input
     * @param _compare compare value
     * @return true if matching
     * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#message-bus-routing-match-rules">DBus Specification</a>
     */
    static boolean matchPathNamespace(String _input, String _compare) {
        if (DBusObjects.validateNotObjectPath(_compare) || DBusObjects.validateNotObjectPath(_compare)) {
            return false;
        } else if (!_input.startsWith(_compare)) {
            return false;
        }

        String[] inputSplit = _input.split("/");
        String[] compareSplit = _compare.split("/");

        if (inputSplit.length < compareSplit.length) {
            return false;
        }

        for (int i = 0; i < compareSplit.length; i++) {
            if (!Util.strEquals(compareSplit[i], inputSplit[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Matcher for arg0Namespace match rule.
     * <br><b>DBus Specification Quote:</b>
     * <p>
     * Match messages whose first argument is of type STRING,
     * and is a bus name or interface name within the specified namespace.
     * This is primarily intended for watching name owner changes for a group of related bus names,
     * rather than for a single name or all name changes.<br><br>
     *
     * Because every valid interface name is also a valid bus name,
     * this can also be used for messages whose first argument is an interface name.<br><br>
     *
     * For example, the match rule member='NameOwnerChanged',arg0namespace='com.example.backend1'
     * matches name owner changes for bus names such as com.example.backend1.foo, com.example.backend1.foo.bar,
     * and com.example.backend1 itself.
     * </p>
     * @param _input signal sender input
     * @param _compare compare value
     * @return true if matching
     * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#message-bus-routing-match-rules">DBus Specification</a>
     */
    static boolean matchArg0Namespace(Message _msg, String _compare) {
        if (_msg == null || _compare == null) {
            return false;
        }

        if (_compare.contains(".") && !DBusObjects.validateBusName(_compare)) {
            return false;
        }

        try {
            List<Type> dataType = new ArrayList<>();
            Marshalling.getJavaType(_msg.getSig(), dataType, -1);

            if (dataType.isEmpty()) {
                return false;
            } else if (dataType.get(0) instanceof Class<?> clz && clz.isAssignableFrom(String.class)) {
                Object[] parameters = _msg.getParameters();
                return parameters[0] == _compare || ((String) parameters[0]).startsWith(_compare);
            }

        } catch (DBusException _ex) {
            throw new DBusExecutionException("Unable to get parameters from signal", _ex);
        }

        return false;
    }

}
