package org.freedesktop.dbus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import org.freedesktop.dbus.exceptions.MarshallingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a FileDescriptor to be passed over the bus.  Can be created from
 * either an integer(gotten through some JNI/JNA/JNR call) or from a
 * java.io.FileDescriptor.
 *
 */
public class FileDescriptor {

    private final Logger      logger          = LoggerFactory.getLogger(getClass());

    private final int fd;

    public FileDescriptor(int _fd) {
        fd = _fd;
    }

    // TODO this should have a better exception?
    public FileDescriptor(java.io.FileDescriptor _data) throws MarshallingException {
        fd = getFileDescriptor(_data);
    }

    // TODO this should have a better exception?
    public java.io.FileDescriptor toJavaFileDescriptor() throws MarshallingException {
        return createFileDescriptorByReflection(fd);
    }

    public int getIntFileDescriptor() {
        return fd;
    }

    private int getFileDescriptor(java.io.FileDescriptor _data) throws MarshallingException {
        Field declaredField;
        try {
            declaredField = _data.getClass().getDeclaredField("fd");
            declaredField.setAccessible(true);
            return declaredField.getInt(_data);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException _ex) {
            logger.error("Could not get filedescriptor by reflection.", _ex);
            throw new MarshallingException("Could not get member 'fd' of FileDescriptor by reflection!", _ex);
        }
    }

    private java.io.FileDescriptor createFileDescriptorByReflection(long _demarshallint) throws MarshallingException {
        try {
            Constructor<java.io.FileDescriptor> constructor = java.io.FileDescriptor.class.getDeclaredConstructor(int.class);
            constructor.setAccessible(true);
            return constructor.newInstance((int) _demarshallint);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException _ex) {
            logger.error("Could not create new FileDescriptor instance by reflection.", _ex);
            throw new MarshallingException("Could not create new FileDescriptor instance by reflection", _ex);
        }
    }
}
