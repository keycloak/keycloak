package org.freedesktop.dbus;

import java.util.Optional;

import org.freedesktop.dbus.exceptions.MarshallingException;
import org.freedesktop.dbus.spi.message.ISocketProvider;
import org.freedesktop.dbus.utils.ReflectionFileDescriptorHelper;

/**
 * Represents a FileDescriptor to be passed over the bus. <br>
 * Can be created from either an integer (gotten through some JNI/JNA/JNR call) or from a
 * {@link java.io.FileDescriptor}.
 */
public final class FileDescriptor {

    private final int fd;

    public FileDescriptor(int _fd) {
        fd = _fd;
    }

    /**
     * Converts this DBus {@link FileDescriptor} to a {@link java.io.FileDescriptor}.<br>
     * Tries to use the provided ISocketProvider if present first. <br>
     * If not present or conversion failed, tries to convert using reflection.
     *
     * @param _provider provider or null
     *
     * @return java file descriptor
     * @throws MarshallingException when converting fails
     */
    public java.io.FileDescriptor toJavaFileDescriptor(ISocketProvider _provider) throws MarshallingException {
        if (_provider != null) {
            Optional<java.io.FileDescriptor> result = _provider.createFileDescriptor(fd);
            if (result.isPresent()) {
                return result.get();
            }
        }

        return ReflectionFileDescriptorHelper.getInstance()
                .flatMap(helper -> helper.createFileDescriptor(fd))
                .orElseThrow(() -> new MarshallingException("Could not create new FileDescriptor instance"));
    }

    public int getIntFileDescriptor() {
        return fd;
    }

    @Override
    public boolean equals(Object _o) {
        if (this == _o) {
            return true;
        }
        if (_o == null || getClass() != _o.getClass()) {
            return false;
        }
        FileDescriptor that = (FileDescriptor) _o;
        return fd == that.fd;
    }

    @Override
    public int hashCode() {
        return fd;
    }

    @Override
    public String toString() {
        return FileDescriptor.class.getSimpleName() + "[fd=" + fd + "]";
    }

    /**
     * Utility method to create a DBus {@link FileDescriptor} from a {@link java.io.FileDescriptor}.<br>
     * Tries to use the provided ISocketProvider if present first.<br>
     * If not present or conversion failed, tries to convert using reflection.
     *
     * @param _data file descriptor
     * @param _provider socket provider or null
     *
     * @return DBus FileDescriptor
     * @throws MarshallingException when conversion fails
     */
    public static FileDescriptor fromJavaFileDescriptor(java.io.FileDescriptor _data, ISocketProvider _provider) throws MarshallingException {
        if (_provider != null) {
            Optional<Integer> result = _provider.getFileDescriptorValue(_data);
            if (result.isPresent()) {
                return new FileDescriptor(result.get());
            }
        }

        return new FileDescriptor(ReflectionFileDescriptorHelper.getInstance()
            .flatMap(helper -> helper.getFileDescriptorValue(_data))
            .orElseThrow(() -> new MarshallingException("Could not get FileDescriptor value")));
    }
}
