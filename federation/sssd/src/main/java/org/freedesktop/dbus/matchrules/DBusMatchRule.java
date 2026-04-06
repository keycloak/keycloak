package org.freedesktop.dbus.matchrules;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Message;

/**
 * Represents a DBus matchrule.<br>
 * Use {@link DBusMatchRuleBuilder} to create instances of this class.
 *
 * @author hypfvieh
 * @since 5.2.0 - 2025-05-03
 */
@SuppressWarnings("removal") // required until old implementation is removed
public class DBusMatchRule {

    private static final Map<String, Class<? extends DBusSignal>> SIGNALTYPEMAP = new ConcurrentHashMap<>();

    private final Map<MatchRuleField, String> fields = new TreeMap<>();
    private final Map<MatchRuleField, Map<Integer, String>> multiValueFields = new TreeMap<>();

    protected DBusMatchRule(Map<MatchRuleField, String> _values, Map<MatchRuleField, Map<Integer, String>> _multiValues) {
        fields.putAll(Objects.requireNonNull(_values, "Values required"));
        if (_multiValues != null) {
            multiValueFields.putAll(_multiValues);
        }
    }

    public String getMessageType() {
        return fields.get(MatchRuleField.TYPE);
    }

    public String getInterface() {
        return fields.get(MatchRuleField.INTERFACE);
    }

    public String getMember() {
        return fields.get(MatchRuleField.MEMBER);
    }

    public String getSender() {
        return fields.get(MatchRuleField.SENDER);
    }

    public String getPath() {
        return fields.get(MatchRuleField.PATH);
    }

    public String getDestination() {
        return fields.get(MatchRuleField.DESTINATION);
    }

    public Map<Integer, String> getArg0123() {
        return getMultiValue(MatchRuleField.ARG0123);
    }

    public Map<Integer, String> getArg0123Path() {
        return getMultiValue(MatchRuleField.ARG0123PATH);
    }

    private Map<Integer, String> getMultiValue(MatchRuleField _field) {
        Map<Integer, String> map = multiValueFields.get(_field);
        if (map != null) {
            return Collections.unmodifiableMap(map);
        }
        return null;
    }

    /**
     * Checks if the given rule matches with our rule.
     *
     * @param _msg message to match against the configure rule
     *
     * @return true if matching
     */
    public boolean matches(Message _msg) {
        if (_msg == null) {
            return false;
        }

        for (Entry<MatchRuleField, String> entry : fields.entrySet()) {
            if (!entry.getKey().getSingleMatcher().test(_msg, entry.getValue())) {
                return false;
            }
        }

        for (Entry<MatchRuleField, Map<Integer, String>> entry : multiValueFields.entrySet()) {
            if (!entry.getKey().getMultiMatcher().test(_msg, entry.getValue())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields, multiValueFields);
    }

    @Override
    public boolean equals(Object _obj) {
        if (this == _obj) {
            return true;
        }
        if (_obj == null) {
            return false;
        }
        if (getClass() != _obj.getClass()) {
            return false;
        }

        DBusMatchRule other = (DBusMatchRule) _obj;
        return Objects.equals(fields, other.fields)
            && Objects.equals(multiValueFields, other.multiValueFields);
    }

    /**
     * Converts this DBusMatchRule to a match rule string as required to use for {@code DBus.addMatch(String)} method.
     */
    @Override
    public String toString() {
        return Stream.concat(
            fields.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> Map.entry(e.getKey().name().toLowerCase(Locale.US), e.getValue())),

        multiValueFields.entrySet().stream()
            .filter(e -> e.getValue() != null)
            .flatMap(e -> {
                String baseName = e.getKey().name().toLowerCase(Locale.US);
                return e.getValue().entrySet().stream()
                    .filter(x -> x.getValue() != null)
                    .map(x -> Map.entry(baseName.replace("0123", String.valueOf(x.getKey())), x.getValue()));
            }))
        .map(e -> "%s='%s'".formatted(e.getKey(), formatValue(e.getValue())))
        .collect(Collectors.joining(","));
    }

    /**
     * Ensures escaping of values in match rule.
     * @param _val value to escape
     * @return escaped string or input if nothing to do
     */
    static String formatValue(String _val) {
        return _val == null ? null : _val.replace("\\", "\\\\").replace("'", "\\'");
    }

    public static Class<? extends DBusSignal> getCachedSignalType(String _type) {
        return SIGNALTYPEMAP.get(_type);
    }

    static void addToTypeMap(String _key, Class<? extends DBusSignal> _clz) {
        SIGNALTYPEMAP.put(_key, _clz);
    }

}
