package org.freedesktop.dbus.utils;

import java.io.FileDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.freedesktop.dbus.spi.message.ISocketProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to work with {@link FileDescriptor} instances by using reflection
 *
 * @author Sergey Shatunov
 * @since 5.0.0 - 2023-10-07
 */
public final class ReflectionFileDescriptorHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionFileDescriptorHelper.class);
    private static final Optional<ReflectionFileDescriptorHelper> INSTANCE = createInstance();

    private final Field fdField;
    private final Constructor<FileDescriptor> constructor;

    private ReflectionFileDescriptorHelper() throws ReflectiveOperationException {
        fdField = FileDescriptor.class.getDeclaredField("fd");
        fdField.setAccessible(true);
        constructor = FileDescriptor.class.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
    }

    /**
     * @see ISocketProvider#getFileDescriptorValue(FileDescriptor)
     */
    public Optional<Integer> getFileDescriptorValue(FileDescriptor _fd) {
        try {
            return Optional.of(fdField.getInt(_fd));
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException _ex) {
            LOGGER.error("Could not get file descriptor by reflection.", _ex);
            return Optional.empty();
        }
    }

    /**
     * @see ISocketProvider#createFileDescriptor(int)
     */
    public Optional<FileDescriptor> createFileDescriptor(int _fd) {
        try {
            return Optional.of(constructor.newInstance(_fd));
        } catch (SecurityException | InstantiationException | IllegalAccessException
                 | IllegalArgumentException | InvocationTargetException _ex) {
            LOGGER.error("Could not create new FileDescriptor instance by reflection.", _ex);
            return Optional.empty();
        }
    }

    private static Optional<ReflectionFileDescriptorHelper> createInstance() {
        try {
            return Optional.of(new ReflectionFileDescriptorHelper());
        } catch (ReflectiveOperationException _ex) {
            LOGGER.error("Unable to hook up java.io.FileDescriptor by using reflection.", _ex);
            return Optional.empty();
        }
    }

    /**
     * @return {@link ReflectionFileDescriptorHelper} instance, or {@link Optional#empty()} if it cannot be initialized
     * (mainly due to missing reflection access)
     */
    public static Optional<ReflectionFileDescriptorHelper> getInstance() {
        return INSTANCE;
    }
}
