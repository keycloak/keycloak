package org.freedesktop.dbus.utils;

import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.exceptions.ClassOutsideOfPackageException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.InvalidBusNameException;
import org.freedesktop.dbus.exceptions.InvalidInterfaceSignature;
import org.freedesktop.dbus.exceptions.InvalidObjectPathException;
import org.freedesktop.dbus.exceptions.InvalidSignalException;
import org.freedesktop.dbus.exceptions.MissingInterfaceImplementationException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;

/**
 * Various validations.
 *
 * @since 5.0.0 - 2023-11-08
 * @author hypfvieh
 */
public final class DBusObjects {
    private static final int     MAX_NAME_LENGTH      = 255;
    private static final Pattern OBJECT_REGEX_PATTERN = Pattern.compile("^/([-_a-zA-Z0-9]+(/[-_a-zA-Z0-9]+)*)?$");
    private static final Pattern BUSNAME_REGEX        = Pattern.compile("^[-_a-zA-Z][-_a-zA-Z0-9]*(\\.[-_a-zA-Z][-_a-zA-Z0-9]*)*$");
    private static final Pattern CONNID_REGEX         = Pattern.compile("^:[0-9]*\\.[0-9]*$");

    private DBusObjects() {
    }

    /**
     * Basic checks of input (e.g. null checks).
     * @param <X> exception type
     * @param _input input
     * @param _validation predicate for validation if value is not null or blank in case of string
     * @param _exSupplier function providing a new exception which will be thrown,
     *                    will receive _customMessage if given or string value of input
     * @param _customMessage custom exception message text, null to omit
     * @return checked input
     * @throws X when validation fails
     */
    private static <T, X extends DBusException> T requireBase(T _input, Predicate<T> _validation, Function<String, X> _exSupplier, String _customMessage) throws X {
        if (_input == null) {
            throw _exSupplier.apply(_customMessage != null ? _customMessage : null);
        } else if (_input instanceof String str && str.isBlank()) {
            throw _exSupplier.apply(_customMessage != null ? _customMessage : "<Empty String>");
        } else if (!_validation.test(_input)) {
            throw _exSupplier.apply(_customMessage != null ? _customMessage : String.valueOf(_input));
        }

        return _input;
    }

    /**
     * Ensures that the given class is part of a package.
     *
     * @param _clz class to check
     * @return input if valid
     *
     * @throws ClassOutsideOfPackageException when class has no package
     */
    public static Class<?> requirePackage(Class<?> _clz) throws ClassOutsideOfPackageException {
        return requirePackage(_clz, null);
    }

    /**
     * Ensures that the given class is implements or extends {@link DBusInterface} class.
     *
     * @param _clz class to check
     * @return input if valid
     *
     * @throws MissingInterfaceImplementationException when class is incompatible
     */
    public static Class<?> requireDBusInterface(Class<?> _clz) throws MissingInterfaceImplementationException {
        return requireDBusInterface(_clz, null);
    }

    /**
     * Ensures given string is a valid object path.
     *
     * @param _objectPath string to check
     *
     * @return input string if valid
     *
     * @throws InvalidObjectPathException when input is not a valid object path
     */
    public static String requireObjectPath(String _objectPath) throws InvalidObjectPathException {
        return requireObjectPath(_objectPath, null);
    }

    /**
     * Ensures given DBusPath is a valid object path.
     *
     * @param _dbusPath to check
     *
     * @return input DBusPath if valid
     *
     * @throws InvalidObjectPathException when input is not a valid object path
     */
    public static DBusPath requireObjectPath(DBusPath _dbusPath) throws InvalidObjectPathException {
        return requireObjectPath(_dbusPath, null);
    }

    /**
     * Ensures that the given class is part of a package.
     *
     * @param _clz class to check
     * @param _customMsg custom error message
     * @return input if valid
     *
     * @throws ClassOutsideOfPackageException when class has no package
     */
    public static Class<?> requirePackage(Class<?> _clz, String _customMsg) throws ClassOutsideOfPackageException {
        return requireBase(_clz, DBusObjects::validateClassHasPackage, msg -> {
            if (Util.isBlank(_customMsg)) {
                return new ClassOutsideOfPackageException(_clz);
            }
            return new ClassOutsideOfPackageException(msg);

        }, _customMsg);
    }

    /**
     * Ensures that the given class is implements or extends {@link DBusInterface} class.
     *
     * @param _clz class to check
     * @param _customMsg custom error message
     * @return input if valid
     *
     * @throws MissingInterfaceImplementationException when class is incompatible
     */
    public static Class<?> requireDBusInterface(Class<?> _clz, String _customMsg) throws MissingInterfaceImplementationException {
        return requireBase(_clz, DBusObjects::validateDBusInterface, msg -> {
            if (Util.isBlank(_customMsg)) {
                return new MissingInterfaceImplementationException(_clz);
            }
            return new MissingInterfaceImplementationException(msg);
        }, _customMsg);
    }

    /**
     * Ensures that the given class is implements or extends {@link DBusSignal} class.
     *
     * @param _clz class to check
     * @param _customMsg custom error message
     * @return input if valid
     *
     * @throws InvalidSignalException when class is incompatible
     */
    public static Class<?> requireDBusSignal(Class<?> _clz, String _customMsg) throws InvalidSignalException {
        return requireBase(_clz, DBusObjects::validateDBusSignal, msg -> {
            if (Util.isBlank(_customMsg)) {
                return new InvalidSignalException(_clz);
            }
            return new InvalidSignalException(msg);
        }, _customMsg);
    }

    /**
     * Ensures that the given class is implements or extends {@link DBusSignal} class.
     *
     * @param _clz class to check
     * @return input if valid
     *
     * @throws InvalidSignalException when class is incompatible
     * @since 5.2.0 - 2025-05-02
     */
    public static Class<?> requireDBusSignal(Class<?> _clz) throws InvalidSignalException {
        return requireDBusSignal(_clz, null);
    }

    /**
     * Checks if given type is a DBusSignal and source comply with naming rules.
     *
     * @param _type class
     * @param _source source of signal (aka sender)
     *
     * @throws DBusException when validation fails
     * @since 5.2.0 - 2025-05-02
     */
    public static void requireDBusSignalRule(Class<?> _type, String _source) throws DBusException {
        requireDBusSignal(_type);
        requireNotBusName(_source, "Cannot watch for signals based on well known bus name as source. Only unique names supported");
        requireConnectionId(_source);
    }

    /**
     * Checks if given String is a valid DBusInterface.
     *
     * @param _str string to check
     * @return input if valid
     *
     * @throws InvalidObjectPathException when not matching
     * @since 5.2.0 - 2025-05-02
     */
    public static String requireDBusInterface(String _str) throws InvalidObjectPathException {
        if (_str == null || _str.isEmpty() || _str.startsWith(".") || !_str.contains(".")) {
            throw new InvalidObjectPathException(_str);
        }
        return _str;
    }

    /**
     * Ensures given string is a valid object path.
     *
     * @param _objectPath string to check
     * @param _customMsg custom error message
     * @return input string if valid
     *
     * @throws InvalidObjectPathException when input is not a valid object path
     */
    public static String requireObjectPath(String _objectPath, String _customMsg) throws InvalidObjectPathException {
        return requireBase(_objectPath, DBusObjects::validateObjectPath, InvalidObjectPathException::new, _customMsg);
    }

    /**
     * Ensures given DBusPath is a valid object path.
     *
     * @param _dbusPath to check
     * @param _customMsg custom error message
     * @return input DBusPath if valid
     *
     * @throws InvalidObjectPathException when input is not a valid object path
     */
    public static DBusPath requireObjectPath(DBusPath _dbusPath, String _customMsg) throws InvalidObjectPathException {
        return requireBase(_dbusPath, x -> validateObjectPath(x.getPath()), InvalidObjectPathException::new, _customMsg);
    }

    /**
     * Ensures given string is a valid bus name.
     *
     * @param _busName string to check
     *
     * @return input if valid
     *
     * @throws InvalidBusNameException when input is not a valid bus name
     */
    public static String requireBusName(String _busName) throws InvalidBusNameException {
        return requireBusName(_busName, null);
    }

    /**
     * Ensures given string is a valid bus name.
     *
     * @param _busName string to check
     * @param _customMessage custom exception message
     *
     * @return input if valid
     *
     * @throws InvalidBusNameException when input is not a valid bus name
     */
    public static String requireBusName(String _busName, String _customMessage) throws InvalidBusNameException {
        return requireBase(_busName, DBusObjects::validateBusName, InvalidBusNameException::new, _customMessage);
    }

    /**
     * Ensures given string is <b>NOT</b> a valid bus name.
     *
     * @param _busName string to check
     * @param _customMessage custom exception message
     *
     * @return input if valid
     *
     * @throws InvalidBusNameException when input is a valid bus name
     */
    public static String requireNotBusName(String _busName, String _customMessage) throws InvalidBusNameException {
        return requireBase(_busName, DBusObjects::validateNotBusName, InvalidBusNameException::new, _customMessage);
    }

    /**
     * Ensures given string is a valid connection Id.
     *
     * @param _connId string to check
     *
     * @return input if valid
     *
     * @throws InvalidBusNameException when input is not a valid connection Id
     */
    public static String requireConnectionId(String _connId) throws InvalidBusNameException {
        return requireBase(_connId, DBusObjects::validateConnectionId, InvalidBusNameException::new, null);
    }

    /**
     * Ensures given string is a valid bus name or connection Id.
     *
     * @param _busNameOrConnId string to check
     * @return input if valid
     * @throws InvalidBusNameException when input is not a valid bus name or connection Id
     */
    public static String requireBusNameOrConnectionId(String _busNameOrConnId) throws InvalidBusNameException {
        if (validateBusName(_busNameOrConnId)) {
            return _busNameOrConnId;
        } else if (validateConnectionId(_busNameOrConnId)) {
            return _busNameOrConnId;
        } else {
            throw new InvalidBusNameException(_busNameOrConnId);
        }
    }

    /**
     * Checks if input is valid bus name.
     *
     * @param _busName input to check
     *
     * @return true if valid
     */
    public static boolean validateBusName(String _busName) {
        return (_busName != null) && (_busName.length() < MAX_NAME_LENGTH) && BUSNAME_REGEX.matcher(_busName).matches();
    }

    /**
     * Checks if input is <b>NOT</b> a valid bus name.
     *
     * @param _busName input to check
     *
     * @return true if invalid
     */
    public static boolean validateNotBusName(String _busName) {
        return !validateBusName(_busName);
    }

    /**
     * Checks if input is a valid object path.
     *
     * @param _objectPath input to check
     *
     * @return true if valid
     */
    public static boolean validateObjectPath(String _objectPath) {
        return !validateNotObjectPath(_objectPath);
    }

    /**
     * Checks if given class is compatible with {@link DBusInterface} class.
     *
     * @param _clz class to check
     *
     * @return true if class is compatible
     */
    public static boolean validateDBusInterface(Class<?> _clz) {
        return DBusInterface.class.isAssignableFrom(_clz);
    }

    /**
     * Checks if given class is compatible with {@link DBusSignal} class.
     *
     * @param _clz class to check
     *
     * @return true if class is compatible
     * @since 5.2.0 - 2025-05-02
     */
    public static boolean validateDBusSignal(Class<?> _clz) {
        return DBusSignal.class.isAssignableFrom(_clz);
    }

    /**
     * Checks if given class has/is in a package.
     *
     * @param _clz class to check
     *
     * @return true if class has a package
     */
    public static boolean validateClassHasPackage(Class<?> _clz) {
        return !_clz.getName().equals(_clz.getSimpleName());
    }

    /**
     * Checks if input is <b>NOT</b> a valid object path.
     *
     * @param _objectPath input to check
     *
     * @return true if invalid
     */
    public static boolean validateNotObjectPath(String _objectPath) {
        return _objectPath == null || _objectPath.length() > MAX_NAME_LENGTH || !_objectPath.startsWith("/") || !OBJECT_REGEX_PATTERN.matcher(_objectPath).matches();
    }

    /**
     * Checks if input is <b>NOT</b> a valid connection Id.
     *
     * @param _connectionId input to check
     *
     * @return true if invalid
     */
    public static boolean validateNotConnectionId(String _connectionId) {
        return !validateConnectionId(_connectionId);
    }

    /**
     * Checks if input is a valid connection Id.
     *
     * @param _connectionId input to check
     *
     * @return true if valid
     */
    public static boolean validateConnectionId(String _connectionId) {
        return (_connectionId != null) && CONNID_REGEX.matcher(_connectionId).matches();
    }

    /**
     * Ensure given value is not null.
     *
     * @param <T> input type
     * @param <X> exception type to throw
     * @param _input input to validate
     * @param _exception supplier providing exception which will be thrown if input is null
     *
     * @return input if not null
     *
     * @throws X exception provided by supplier thrown when input was null
     */
    public static <T, X extends Exception> T requireNotNull(T _input, Supplier<X> _exception) throws X {
        if (_input == null) {
            throw _exception.get();
        }
        return _input;
    }

    /**
     * Ensures that all interfaces found on the given object a public accessable.
     *
     * @param _object object to check
     *
     * @throws InvalidInterfaceSignature when there is any interface on the given object which is non public
     */
    public static void ensurePublicInterfaces(Object _object) throws InvalidInterfaceSignature {
        Set<String> invalid = new LinkedHashSet<>();
        for (Class<?> iClz : _object.getClass().getInterfaces()) {
            if (!Modifier.isPublic(iClz.getModifiers())) {
                invalid.add(iClz.getName());
            }
        }
        if (!invalid.isEmpty()) {
            throw new InvalidInterfaceSignature(invalid);
        }

    }
}
