package org.freedesktop.dbus.matchrules;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiPredicate;

import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.constants.MessageTypes;
import org.freedesktop.dbus.utils.Util;

public enum MatchRuleField {
    TYPE((m, s) -> Util.strEquals(MessageTypes.getRuleNameById(m.getType()), s), null),
    SENDER((m, s) -> Util.strEquals(m.getSource(), s), null),
    INTERFACE((m, s) -> Util.strEquals(m.getInterface(), s), null),
    MEMBER((m, s) -> Util.strEquals(m.getName(), s), null),
    PATH((m, s) -> Util.strEquals(m.getPath(), s), null),
    PATH_NAMESPACE((m, s) -> MatchRuleMatcher.matchPathNamespace(m.getPath(), s), null),
    DESTINATION((m, s) -> Util.strEquals(m.getDestination(), s), null),
    ARG0123(null, (m, s) -> MatchRuleMatcher.matchArg0123(m, s)),
    ARG0123PATH(null, (m, s) -> MatchRuleMatcher.matchArg0123Path(m, s)),
    ARG0NAMESPACE((m, s) -> MatchRuleMatcher.matchArg0Namespace(m, s), null);

    private final BiPredicate<Message, String> singleMatcher;
    private final BiPredicate<Message, Map<Integer, String>> multiMatcher;

    MatchRuleField(BiPredicate<Message, String> _singleMatcher, BiPredicate<Message, Map<Integer, String>> _multiMatcher) {
        singleMatcher = _singleMatcher;
        multiMatcher = _multiMatcher;
    }

    public Entry<MatchRuleField, String> entryOf(String _val) {
        return _val == null ? null : Map.entry(this, _val);
    }

    public BiPredicate<Message, String> getSingleMatcher() {
        return singleMatcher;
    }

    public BiPredicate<Message, Map<Integer, String>> getMultiMatcher() {
        return multiMatcher;
    }

}
