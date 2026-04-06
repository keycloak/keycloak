package org.freedesktop.dbus.messages;


import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.constants.ArgumentType;
import org.freedesktop.dbus.messages.constants.HeaderField;
import org.freedesktop.dbus.types.UInt32;

public abstract class MethodBase extends Message {

    MethodBase() {
    }

    protected MethodBase(byte _endianness, byte _methodCall, byte _flags) throws DBusException {
        super(_endianness, _methodCall, _flags);
    }

    /**
     * Appends filedescriptors (if any).
     *
     * @param _hargs
     * @param _args
     * @throws DBusException
     */
    void appendFileDescriptors(List<Object> _hargs, Object... _args) {
        Objects.requireNonNull(_hargs);

        long totalFileDes = _args == null ? 0 : Arrays.stream(_args)
            .filter(FileDescriptor.class::isInstance)
            .count();

        if (totalFileDes > 0) {
            _hargs.add(createHeaderArgs(HeaderField.UNIX_FDS, ArgumentType.UINT32_STRING, new UInt32(totalFileDes)));
        }

    }
}
