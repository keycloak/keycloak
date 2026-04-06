package org.freedesktop.dbus.messages.constants;

import java.util.stream.Stream;

public enum MessageTypes {
    METHOD_CALL((byte) 1, "method_call"),
    METHOD_REPLY((byte) 2, "method_reply"),
    ERROR((byte) 3, "error"),
    SIGNAL((byte) 4, "signal");

    private final byte id;
    private final String matchRuleName;

    MessageTypes(byte _id, String _name) {
        id = _id;
        matchRuleName = _name;
    }

    public byte getId() {
        return id;
    }

    public String getMatchRuleName() {
        return matchRuleName;
    }

    public static MessageTypes getById(byte _id) {
        return Stream.of(values())
            .filter(e -> e.getId() == _id)
            .findFirst().orElse(null);
    }

    public static String getRuleNameById(byte _id) {
        return Stream.of(values())
            .filter(e -> e.getId() == _id)
            .map(e -> e.getMatchRuleName())
            .findFirst().orElse(null);
    }

}
