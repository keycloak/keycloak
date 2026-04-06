package org.freedesktop.dbus;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodTuple {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String name;
    private final String sig;

    public MethodTuple(String _name, String _sig) {
        name = _name;
        sig = Objects.requireNonNullElse(_sig, "");
        logger.trace("new MethodTuple({}, {})", name, sig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sig);
    }

    @Override
    public boolean equals(Object _obj) {
        if (this == _obj) {
            return true;
        }
        if (!(_obj instanceof MethodTuple other)) {
            return false;
        }
        return Objects.equals(name, other.name) && Objects.equals(sig, other.sig);
    }

    public Logger getLogger() {
        return logger;
    }

    public String getName() {
        return name;
    }

    public String getSig() {
        return sig;
    }
}
