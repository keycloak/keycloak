package org.freedesktop.dbus.matchrules;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.freedesktop.dbus.errors.MatchRuleInvalid;

/**
 * Utility to read match rule strings.
 *
 * @author hypfvieh
 * @since 5.2.0 - 2025-05-03
 */
public final class MatchRuleParser {
    private static final Pattern PATTERN = Pattern.compile("(\\w+)=('(?:\\\\'|[^'])*')");

    private MatchRuleParser() {
    }

    /**
     * Creates a DBusMatchRule from a rule string as used on DBus.
     * @param _ruleStr rule string
     * @return DBusMatchRule
     * @throws MatchRuleInvalid when rule is invalid
     */
    public static DBusMatchRule convertMatchRule(String _ruleStr) {
        try {
            return DBusMatchRuleBuilder.create().fromMap(parseMatchRule(_ruleStr));
        } catch (Exception _ex) {
            throw new MatchRuleInvalid("Matchrule \"" + _ruleStr + "\"string is invalid", _ex);
        }
    }

    /**
     * Reads a DBusMatchRule-String to a Map.
     * @param _ruleStr rule string
     * @return Map
     */
    public static Map<String, String> parseMatchRule(String _ruleStr) {
        if (_ruleStr == null || _ruleStr.isBlank()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();

        Matcher matcher = PATTERN.matcher(_ruleStr);

        while (matcher.find()) {
            String key = matcher.group(1);
            String rawVal = matcher.group(2);

            String val = rawVal.substring(1, rawVal.length() - 1);

            val = val.replace("\\'", "'");

            result.put(key, val);
        }

        return result;
    }
}
