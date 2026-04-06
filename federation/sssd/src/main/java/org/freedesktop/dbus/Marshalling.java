package org.freedesktop.dbus;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.base.AbstractConnectionBase;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusTypeConversationRuntimeException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSerializable;
import org.freedesktop.dbus.messages.constants.ArgumentType;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.DBusMapType;
import org.freedesktop.dbus.types.DBusStructType;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.utils.LoggingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains static methods for marshalling values.
 */
public final class Marshalling {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

    /** Used as initial and incremental size of StringBuffer array when resolving DBusTypes recursively. */
    private static final int INITIAL_BUFFER_SZ = 10;

    private static final String MTH_NAME_DESERIALIZE = "deserialize";
    private static final String ERROR_MULTI_VALUED_ARRAY = "Multi-valued array types not permitted";

    private static final Logger LOGGER = LoggerFactory.getLogger(Marshalling.class);

    private static final Map<Type, String[]> TYPE_CACHE = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Byte> CLASS_TO_ARGUMENTTYPE = new LinkedHashMap<>();
    static {
        CLASS_TO_ARGUMENTTYPE.put(Boolean.class, ArgumentType.BOOLEAN); // class
        CLASS_TO_ARGUMENTTYPE.put(Boolean.TYPE, ArgumentType.BOOLEAN); // primitive type

        CLASS_TO_ARGUMENTTYPE.put(Byte.class, ArgumentType.BYTE);
        CLASS_TO_ARGUMENTTYPE.put(Byte.TYPE, ArgumentType.BYTE);

        CLASS_TO_ARGUMENTTYPE.put(Short.class, ArgumentType.INT16);
        CLASS_TO_ARGUMENTTYPE.put(Short.TYPE, ArgumentType.INT16);

        CLASS_TO_ARGUMENTTYPE.put(Integer.class, ArgumentType.INT32);
        CLASS_TO_ARGUMENTTYPE.put(Integer.TYPE, ArgumentType.INT32);

        CLASS_TO_ARGUMENTTYPE.put(Long.class, ArgumentType.INT64);
        CLASS_TO_ARGUMENTTYPE.put(Long.TYPE, ArgumentType.INT64);

        CLASS_TO_ARGUMENTTYPE.put(Double.class, ArgumentType.DOUBLE);
        CLASS_TO_ARGUMENTTYPE.put(Double.TYPE, ArgumentType.DOUBLE);

        if (AbstractConnection.FLOAT_SUPPORT) {
            CLASS_TO_ARGUMENTTYPE.put(Float.class, ArgumentType.FLOAT);
            CLASS_TO_ARGUMENTTYPE.put(Float.TYPE, ArgumentType.FLOAT);
        } else {
            CLASS_TO_ARGUMENTTYPE.put(Float.class, ArgumentType.DOUBLE);
            CLASS_TO_ARGUMENTTYPE.put(Float.TYPE, ArgumentType.DOUBLE);
        }

        CLASS_TO_ARGUMENTTYPE.put(UInt16.class, ArgumentType.UINT16);
        CLASS_TO_ARGUMENTTYPE.put(UInt32.class, ArgumentType.UINT32);
        CLASS_TO_ARGUMENTTYPE.put(UInt64.class, ArgumentType.UINT64);

        CLASS_TO_ARGUMENTTYPE.put(CharSequence.class, ArgumentType.STRING);
        CLASS_TO_ARGUMENTTYPE.put(Variant.class, ArgumentType.VARIANT);

        CLASS_TO_ARGUMENTTYPE.put(FileDescriptor.class, ArgumentType.FILEDESCRIPTOR);

        CLASS_TO_ARGUMENTTYPE.put(DBusInterface.class, ArgumentType.OBJECT_PATH);
        CLASS_TO_ARGUMENTTYPE.put(DBusPath.class, ArgumentType.OBJECT_PATH);
        CLASS_TO_ARGUMENTTYPE.put(ObjectPath.class, ArgumentType.OBJECT_PATH);
    }

    private Marshalling() {
    }

    /**
    * Will return the DBus type corresponding to the given Java type.
    * Note, container type should have their ParameterizedType not their
    * Class passed in here.
    *
    * @param _javaType The Java types.
    * @return The DBus types.
    */
    public static String getDBusType(Type[] _javaType) throws DBusException {
        StringBuilder sb = new StringBuilder();
        for (Type t : _javaType) {
            for (String s : getDBusType(t)) {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    /**
    * Will return the DBus type corresponding to the given Java type.
    * Note, container type should have their ParameterizedType not their
    * Class passed in here.
    *
    * @param _javaType The Java types.
    * @return The DBus types
    *
    * @throws DBusTypeConversationRuntimeException when conversation fails
    *
    * @since 5.1.0 - 2024-05-19
    */
    public static String convertJavaClassesToSignature(Class<?>... _javaType) throws DBusTypeConversationRuntimeException {
        if (_javaType == null || _javaType.length == 0) {
            throw new DBusTypeConversationRuntimeException("No types to convert given");
        }
        StringBuilder sig = new StringBuilder();
        convertToSig(sig, 0, _javaType);

        return sig.toString();
    }

    private static int convertToSig(StringBuilder _sig, int _idx, Class<?>... _javaType) {
        for (int i = _idx; i < _javaType.length; i++) {
            Class<?> clz = _javaType[i];
            if (Collection.class.isAssignableFrom(clz)) {
                _sig.append(ArgumentType.ARRAY_STRING);
            } else if (Map.class.isAssignableFrom(clz)) {
                _sig.append(ArgumentType.ARRAY_STRING).append(ArgumentType.DICT_ENTRY1_STRING);
                i = convertToSig(_sig, i + 1, _javaType) - 1;
                _sig.append(ArgumentType.DICT_ENTRY2_STRING);
                return i;
            } else if (CharSequence.class.isAssignableFrom(clz)) {
                _sig.append(ArgumentType.STRING_STRING);
            } else if (Struct.class.isAssignableFrom(clz)) {
                _sig.append(ArgumentType.STRUCT1_STRING);

                Class<?>[] structure = Arrays.stream(clz.getDeclaredFields())
                    .map(Field::getType)
                    .toArray(Class<?>[]::new);

                convertToSig(_sig, 0, structure);
                _sig.append(ArgumentType.STRUCT2_STRING);
                return i;
            } else if (Tuple.class.isAssignableFrom(clz)) {
                continue; // simply ignore Tuple types
            } else if (CLASS_TO_ARGUMENTTYPE.containsKey(clz)) {
                char val = (char) CLASS_TO_ARGUMENTTYPE.get(clz).byteValue();
                _sig.append(val);
            } else {
                throw new DBusTypeConversationRuntimeException("Unsupported class type " + clz);
            }

        }
        return 0;
    }

    /**
    * Will return the DBus type corresponding to the given Java type.
    * Note, container type should have their ParameterizedType not their
    * Class passed in here.
    * @param _javaType The Java type.
    * @return The DBus type.
    * @throws DBusException If the given type cannot be converted to a DBus type.
    */
    public static String[] getDBusType(Type _javaType) throws DBusException {
        String[] cached = TYPE_CACHE.get(_javaType);
        if (null != cached) {
            return cached;
        }
        cached = getDBusType(_javaType, false);
        TYPE_CACHE.put(_javaType, cached);
        return cached;
    }

    /**
    * Will return the DBus type corresponding to the given Java type.
    * Note, container type should have their ParameterizedType not their
    * Class passed in here.
    * @param _dataType The Java type.
    * @param _basic If true enforces this to be a non-compound type. (compound types are Maps, Structs and Lists/arrays).
    * @return The DBus type.
    * @throws DBusException If the given type cannot be converted to a DBus type.
    */
    public static String[] getDBusType(Type _dataType, boolean _basic) throws DBusException {
        return recursiveGetDBusType(new StringBuffer[INITIAL_BUFFER_SZ], _dataType, _basic, 0);
    }

    @SuppressWarnings("checkstyle:parameterassignment")
    private static String[] recursiveGetDBusType(StringBuffer[] _out, Type _dataType, boolean _basic, int _level) throws DBusException {
        if (_out.length <= _level) {
            StringBuffer[] newout = new StringBuffer[_level + INITIAL_BUFFER_SZ];
            System.arraycopy(_out, 0, newout, 0, _out.length);
            _out = newout;
        }
        if (null == _out[_level]) {
            _out[_level] = new StringBuffer();
        } else {
            _out[_level].delete(0, _out[_level].length());
        }

        if (_basic && !(_dataType instanceof Class<?>)) {
            throw new DBusException(_dataType + " is not a basic type");
        }

        if (_dataType instanceof WildcardType wildcardType && wildcardType.getUpperBounds().length > 0) {
            return recursiveGetDBusType(_out, wildcardType.getUpperBounds()[0], _basic, _level);
        } else if (_dataType instanceof TypeVariable) {
            _out[_level].append((char) ArgumentType.VARIANT);
        } else if (_dataType instanceof GenericArrayType gat) {
            _out[_level].append((char) ArgumentType.ARRAY);
            String[] s = recursiveGetDBusType(_out, gat.getGenericComponentType(), false, _level + 1);
            if (s.length != 1) {
                throw new DBusException(ERROR_MULTI_VALUED_ARRAY);
            }
            _out[_level].append(s[0]);
        } else if (_dataType instanceof Class<?> && DBusSerializable.class.isAssignableFrom((Class<?>) _dataType)
            || _dataType instanceof ParameterizedType pt
                && DBusSerializable.class.isAssignableFrom((Class<?>) pt.getRawType())) {
            // it's a custom serializable type
            Type[] newtypes = null;
            if (_dataType instanceof Class<?> clz) {
                for (Method m : clz.getDeclaredMethods()) {
                    if (m.getName().equals(MTH_NAME_DESERIALIZE)) {
                        newtypes = m.getGenericParameterTypes();
                    }
                }
            } else {
                for (Method m : ((Class<?>) ((ParameterizedType) _dataType).getRawType()).getDeclaredMethods()) {
                    if (m.getName().equals(MTH_NAME_DESERIALIZE)) {
                        newtypes = m.getGenericParameterTypes();
                    }
                }
            }

            if (null == newtypes) {
                throw new DBusException("Serializable classes must implement a deserialize method");
            }

            String[] sigs = new String[newtypes.length];
            for (int j = 0; j < sigs.length; j++) {
                String[] ss = recursiveGetDBusType(_out, newtypes[j], false, _level + 1);
                if (1 != ss.length) {
                    throw new DBusException("Serializable classes must serialize to native DBus types");
                }
                sigs[j] = ss[0];
            }
            return sigs;
        } else if (_dataType instanceof ParameterizedType p) {
            if (p.getRawType().equals(Map.class)) {
                _out[_level].append(ArgumentType.ARRAY_STRING)
                    .append(ArgumentType.DICT_ENTRY1_STRING);
                Type[] t = p.getActualTypeArguments();
                try {
                    String[] s = recursiveGetDBusType(_out, t[0], true, _level + 1);
                    if (s.length != 1) {
                        throw new DBusException(ERROR_MULTI_VALUED_ARRAY);
                    }
                    _out[_level].append(s[0]);
                    s = recursiveGetDBusType(_out, t[1], false, _level + 1);
                    if (s.length != 1) {
                        throw new DBusException(ERROR_MULTI_VALUED_ARRAY);
                    }
                    _out[_level].append(s[0]);
                } catch (ArrayIndexOutOfBoundsException _ex) {
                    LOGGER.debug("", _ex);
                    throw new DBusException("Map must have 2 parameters");
                }
                _out[_level].append(ArgumentType.DICT_ENTRY2_STRING);
            } else if (List.class.isAssignableFrom((Class<?>) p.getRawType())) {
                for (Type t : p.getActualTypeArguments()) {
                    if (Type.class.equals(t)) {
                        _out[_level].append((char) ArgumentType.SIGNATURE);
                    } else {
                        String[] s = recursiveGetDBusType(_out, t, false, _level + 1);
                        if (s.length != 1) {
                            throw new DBusException(ERROR_MULTI_VALUED_ARRAY);
                        }
                        _out[_level].append((char) ArgumentType.ARRAY);
                        _out[_level].append(s[0]);
                    }
                }
            } else if (p.getRawType().equals(Variant.class)) {
                _out[_level].append((char) ArgumentType.VARIANT);
            } else if (DBusInterface.class.isAssignableFrom((Class<?>) p.getRawType())) {
                _out[_level].append((char) ArgumentType.OBJECT_PATH);
            } else if (Struct.class.isAssignableFrom((Class<?>) p.getRawType())) {
                _out[_level].append((char) ArgumentType.STRUCT1);
            } else if (Tuple.class.isAssignableFrom((Class<?>) p.getRawType())) {
                Type[] ts = p.getActualTypeArguments();
                List<String> vs = new ArrayList<>();
                for (Type t : ts) {
                    Collections.addAll(vs, recursiveGetDBusType(_out, t, false, _level + 1));
                }
                return vs.toArray(EMPTY_STRING_ARRAY);
            } else {
                throw new DBusException("Exporting non-exportable parameterized type " + _dataType);
            }
        } else if (_dataType instanceof Class<?> dataTypeClazz) {

            if (dataTypeClazz.isArray()) {
                if (Type.class.equals(((Class<?>) _dataType).getComponentType())) {
                    _out[_level].append((char) ArgumentType.SIGNATURE);
                } else {
                    _out[_level].append((char) ArgumentType.ARRAY);
                    String[] s = recursiveGetDBusType(_out, ((Class<?>) _dataType).getComponentType(), false, _level + 1);
                    if (s.length != 1) {
                        throw new DBusException(ERROR_MULTI_VALUED_ARRAY);
                    }
                    _out[_level].append(s[0]);
                }
            } else if (Struct.class.isAssignableFrom((Class<?>) _dataType)) {
                _out[_level].append((char) ArgumentType.STRUCT1);
                Type[] ts = Container.getTypeCache(_dataType);
                if (null == ts) {
                    Field[] fs = ((Class<?>) _dataType).getDeclaredFields();
                    ts = new Type[fs.length];
                    for (Field f : fs) {
                        Position p = f.getAnnotation(Position.class);
                        if (null == p) {
                            continue;
                        }
                        ts[p.value()] = f.getGenericType();
                    }
                    Container.putTypeCache(_dataType, ts);
                }

                for (Type t : ts) {
                    if (t != null) {
                        for (String s : recursiveGetDBusType(_out, t, false, _level + 1)) {
                            _out[_level].append(s);
                        }
                    }
                }
                _out[_level].append(ArgumentType.STRUCT2_STRING);

            } else if (Enum.class.isAssignableFrom(dataTypeClazz)) {
                _out[_level].append((char) ArgumentType.STRING);
            } else {
                boolean found = false;

                for (Entry<Class<?>, Byte> entry : CLASS_TO_ARGUMENTTYPE.entrySet()) {
                    if (entry.getKey().isAssignableFrom(dataTypeClazz)) {
                        _out[_level].append((char) entry.getValue().byteValue());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new DBusException("Exporting non-exportable type: " + _dataType);
                }
            }
        }

        LOGGER.trace("Converted Java type: {} to D-Bus Type: {}", _dataType, _out[_level]);

        return new String[] {
                _out[_level].toString()
        };
    }

    /**
    * Converts a dbus type string into Java Type objects,
    * @param _dbusType The DBus type or types.
    * @param _resultValue List to return the types in.
    * @param _limit Maximum number of types to parse (-1 == nolimit).
    * @return number of characters parsed from the type string.
    * @throws DBusException on error
    */
    public static int getJavaType(String _dbusType, List<Type> _resultValue, int _limit) throws DBusException {
        if (null == _dbusType || _dbusType.isEmpty() || 0 == _limit) {
            return 0;
        }

        try {
            int idx = 0;
            for (; idx < _dbusType.length() && (-1 == _limit || _limit > _resultValue.size()); idx++) {
                switch (_dbusType.charAt(idx)) {
                case ArgumentType.STRUCT1:
                    int structIdx = idx + 1;
                    for (int structLen = 1; structLen > 0; structIdx++) {
                        if (ArgumentType.STRUCT2 == _dbusType.charAt(structIdx)) {
                            structLen--;
                        } else if (ArgumentType.STRUCT1 == _dbusType.charAt(structIdx)) {
                            structLen++;
                        }
                    }

                    List<Type> contained = new ArrayList<>();
                    getJavaType(_dbusType.substring(idx + 1, structIdx - 1), contained, -1);
                    _resultValue.add(new DBusStructType(contained.toArray(EMPTY_TYPE_ARRAY)));
                    idx = structIdx - 1; //-1 because j already points to the next signature char
                    break;
                case ArgumentType.ARRAY:
                    if (ArgumentType.DICT_ENTRY1 == _dbusType.charAt(idx + 1)) {
                        contained = new ArrayList<>();
                        int javaType = getJavaType(_dbusType.substring(idx + 2), contained, 2);
                        _resultValue.add(new DBusMapType(contained.get(0), contained.get(1)));
                        idx += javaType + 2;
                    } else {
                        contained = new ArrayList<>();
                        int javaType = getJavaType(_dbusType.substring(idx + 1), contained, 1);
                        _resultValue.add(new DBusListType(contained.get(0)));
                        idx += javaType;
                    }
                    break;
                case ArgumentType.VARIANT:
                    _resultValue.add(Variant.class);
                    break;
                case ArgumentType.BOOLEAN:
                    _resultValue.add(Boolean.class);
                    break;
                case ArgumentType.INT16:
                    _resultValue.add(Short.class);
                    break;
                case ArgumentType.BYTE:
                    _resultValue.add(Byte.class);
                    break;
                case ArgumentType.OBJECT_PATH:
                    _resultValue.add(DBusPath.class);
                    break;
                case ArgumentType.UINT16:
                    _resultValue.add(UInt16.class);
                    break;
                case ArgumentType.INT32:
                    _resultValue.add(Integer.class);
                    break;
                case ArgumentType.UINT32:
                    _resultValue.add(UInt32.class);
                    break;
                case ArgumentType.INT64:
                    _resultValue.add(Long.class);
                    break;
                case ArgumentType.UINT64:
                    _resultValue.add(UInt64.class);
                    break;
                case ArgumentType.DOUBLE:
                    _resultValue.add(Double.class);
                    break;
                case ArgumentType.FLOAT:
                    _resultValue.add(Float.class);
                    break;
                case ArgumentType.STRING:
                    _resultValue.add(CharSequence.class);
                    break;
                case ArgumentType.FILEDESCRIPTOR:
                    _resultValue.add(FileDescriptor.class);
                    break;
                case ArgumentType.SIGNATURE:
                    _resultValue.add(Type[].class);
                    break;
                case ArgumentType.DICT_ENTRY1:
                    contained = new ArrayList<>();
                    int javaType = getJavaType(_dbusType.substring(idx + 1), contained, 2);
                    _resultValue.add(new DBusMapType(contained.get(0), contained.get(1)));
                    idx += javaType + 1;
                    break;
                default:
                    throw new DBusException(String.format("Failed to parse DBus type signature: %s (%s).", _dbusType, _dbusType.charAt(idx)));
                }
            }
            return idx;
        } catch (IndexOutOfBoundsException _ex) {
            LOGGER.debug("Failed to parse DBus type signature.", _ex);
            throw new DBusException("Failed to parse DBus type signature: " + _dbusType);
        }
    }

    /**
     * Recursively converts types for serialization onto DBus.<br>
     * <br>
     * When _customSignature is not empty or null, it will be used to wrap the given parameters to a {@link Variant}<br>
     * of the type defined in the _customSignature array.<br>
     * It is required that every {@link Variant} passed has a signature definition in _customSignature.<br>
     * E.g. 3 Objects are given in _parameters: String, Variant, Variant.<br>
     * Then it is expected that _customSignature (if used) contains 2 entries one for each {@link Variant}.<br>
     * <br>
     * If the _customSignature is smaller than the count of {@link Variant}s in _parameters, all remaining {@link Variant}s<br>
     * are created without a explicit signature (Variant constructor will try to determine type automatically).<br>
     * If more entries are given then {@link Variant}s found in _parameters, the additional signatures are ignored.
     *
     * @param _parameters The parameters to convert.
     * @param _types The (possibly generic) types of the parameters.
     * @param _customSignatures custom signatures used for variants found in _types, each found variant must have one matching custom signature
     * @param _conn the connection
     * @return The converted parameters.
     * @throws DBusException Thrown if there is an error in converting the objects.
     */
    public static Object[] convertParameters(Object[] _parameters, Type[] _types, String[] _customSignatures, AbstractConnectionBase _conn) throws DBusException {
        if (_parameters == null) {
            return null;
        }

        Object[] parameters = _parameters;
        Type[] types = _types;
        int lastCustomSig = 0;
        for (int i = 0; i < parameters.length; i++) {
            if (null == parameters[i]) {
                continue;
            }
            LOGGER.trace("Converting {} from '{}' to {}", i, parameters[i], types[i]);

            if (parameters[i] instanceof DBusSerializable ds) {
                for (Method m : parameters[i].getClass().getDeclaredMethods()) {
                    if (m.getName().equals(MTH_NAME_DESERIALIZE)) {
                        Type[] newtypes = m.getParameterTypes();
                        Type[] expand = new Type[types.length + newtypes.length - 1];
                        System.arraycopy(types, 0, expand, 0, i);
                        System.arraycopy(newtypes, 0, expand, i, newtypes.length);
                        System.arraycopy(types, i + 1, expand, i + newtypes.length, types.length - i - 1);
                        types = expand;
                        Object[] newparams = ds.serialize();
                        Object[] exparams = new Object[parameters.length + newparams.length - 1];
                        System.arraycopy(parameters, 0, exparams, 0, i);
                        System.arraycopy(newparams, 0, exparams, i, newparams.length);
                        System.arraycopy(parameters, i + 1, exparams, i + newparams.length, parameters.length - i - 1);
                        parameters = exparams;
                    }
                }
                i--;
            } else if (parameters[i] instanceof Tuple tup) {
                Type[] newtypes = ((ParameterizedType) types[i]).getActualTypeArguments();
                Type[] expand = new Type[types.length + newtypes.length - 1];
                System.arraycopy(types, 0, expand, 0, i);
                System.arraycopy(newtypes, 0, expand, i, newtypes.length);
                System.arraycopy(types, i + 1, expand, i + newtypes.length, types.length - i - 1);
                types = expand;
                Object[] newparams = tup.getParameters();
                Object[] exparams = new Object[parameters.length + newparams.length - 1];
                System.arraycopy(parameters, 0, exparams, 0, i);
                System.arraycopy(newparams, 0, exparams, i, newparams.length);
                System.arraycopy(parameters, i + 1, exparams, i + newparams.length, parameters.length - i - 1);
                parameters = exparams;

                LoggingHelper.logIf(LOGGER.isTraceEnabled(),
                    () -> LOGGER.trace("New params: {}, new types: {}", Arrays.deepToString(exparams), Arrays.deepToString(expand)));

                i--;
            } else if (types[i] instanceof TypeVariable && !(parameters[i] instanceof Variant)) {
                // its an unwrapped variant, wrap it
                if (_customSignatures != null && _customSignatures.length > 0 && _customSignatures.length > lastCustomSig) {
                    parameters[i] = new Variant<>(parameters[i], _customSignatures[lastCustomSig]);
                    lastCustomSig++;
                } else {
                    parameters[i] = new Variant<>(parameters[i]);
                }

            } else if (parameters[i] instanceof DBusInterface di) {
                parameters[i] = _conn.getExportedObject(di);
            }
        }
        return parameters;
    }

    /**
     * Recursively converts types for serialization onto DBus.
     *
     * @param _parameters The parameters to convert.
     * @param _types The (possibly generic) types of the parameters.
     * @param _conn the connection
     * @return The converted parameters.
     * @throws DBusException Thrown if there is an error in converting the objects.
     */
    public static Object[] convertParameters(Object[] _parameters, Type[] _types, AbstractConnectionBase _conn) throws DBusException {
        return convertParameters(_parameters, _types, null, _conn);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Object deSerializeParameter(Object _parameter, Type _type, AbstractConnectionBase _conn) throws Exception {
        LOGGER.trace("Deserializing from {} to {}", _parameter.getClass(), _type);

        Object parameter = _parameter;
        // its a wrapped variant, unwrap it
        if (_type instanceof TypeVariable && parameter instanceof Variant<?> variant) {
            parameter = variant.getValue();
            LOGGER.trace("Type is variant, unwrapping to {}", parameter);
        }

        // Turn a signature into a Type[]
        if (_type instanceof Class && ((Class<?>) _type).isArray() && ((Class<?>) _type).getComponentType().equals(Type.class) && parameter instanceof String) {
            List<Type> rv = new ArrayList<>();
            getJavaType((String) parameter, rv, -1);
            parameter = rv.toArray(EMPTY_TYPE_ARRAY);
        }

        // its an object path, get/create the proxy
        if (parameter instanceof ObjectPath op) {
            LOGGER.trace("Parameter is ObjectPath");
            if (_type instanceof Class && DBusInterface.class.isAssignableFrom((Class<?>) _type)) {
                parameter = _conn.getExportedObject(op.getSource(), op.getPath(), (Class<DBusInterface>) _type);
            } else {
                parameter = new DBusPath(op.getPath());
            }
        }

        if (parameter instanceof DBusPath op) {
            LOGGER.trace("Parameter is DBusPath");
            if (_type instanceof Class && DBusInterface.class.isAssignableFrom((Class<?>) _type)) {
                parameter = _conn.getExportedObject(op.getSource(), op.getPath(), (Class<DBusInterface>) _type);
            } else {
                parameter = new DBusPath(op.getPath());
            }
        }

        // its an enum, parse either as the string name or the ordinal
        if (parameter instanceof String str && _type instanceof Class && Enum.class.isAssignableFrom((Class<?>) _type)) {
            LOGGER.trace("Type seems to be an enum");
            parameter = Enum.valueOf((Class<Enum>) _type, str);
        }

        // it should be a struct. create it
        if (parameter instanceof Object[] objArr && _type instanceof Class && Struct.class.isAssignableFrom((Class<?>) _type)) {
            LOGGER.trace("Creating Struct {} from {}", _type, parameter);
            Type[] ts = Container.getTypeCache(_type);
            if (ts == null) {
                Field[] fs = ((Class<?>) _type).getDeclaredFields();
                ts = new Type[fs.length];
                for (Field f : fs) {
                    Position p = f.getAnnotation(Position.class);
                    if (null == p) {
                        continue;
                    }
                    ts[p.value()] = f.getGenericType();
                }
                Container.putTypeCache(_type, ts);
            }

            // recurse over struct contents
            parameter = deSerializeParameters(objArr, ts, _conn);
            for (Constructor<?> con : ((Class<?>) _type).getDeclaredConstructors()) {
                try {
                    parameter = con.newInstance(objArr);
                    break;
                } catch (IllegalArgumentException _exIa) {
                    LOGGER.trace("Could not create new instance", _exIa);
                }
            }
        }

        // recurse over arrays
        if (parameter instanceof Object[] oa) {
            LOGGER.trace("Parameter is object array");
            Type[] ts = new Type[oa.length];
            Arrays.fill(ts, parameter.getClass().getComponentType());
            parameter = deSerializeParameters(oa, ts, _conn);
        }
        if (parameter instanceof List) {
            LOGGER.trace("Parameter is List");
            Type type2;
            if (_type instanceof ParameterizedType pt) {
                type2 = pt.getActualTypeArguments()[0];
            } else if (_type instanceof GenericArrayType gat) {
                type2 = gat.getGenericComponentType();
            } else if (_type instanceof Class<?> clz && ((Class<?>) _type).isArray()) {
                type2 = clz.getComponentType();
            } else {
                type2 = null;
            }
            LOGGER.trace("Type is: {}", type2);
            if (null != type2) {
                parameter = deSerializeParameters((List<Object>) parameter, type2, _conn);
            }
        }

        // correct floats if appropriate
        if ((_type.equals(Float.class) || _type.equals(Float.TYPE)) && !(parameter instanceof Float)) {
            parameter = ((Number) parameter).floatValue();
            LOGGER.trace("Parameter is float of value: {}", parameter);
        }

        // make sure arrays are in the correct format
        if (parameter instanceof Object[] || parameter instanceof Collection<?> || parameter.getClass().isArray()) {
            if (_type instanceof ParameterizedType pt) {
                parameter = ArrayFrob.convert(parameter, (Class<? extends Object>) pt.getRawType());
            } else if (_type instanceof GenericArrayType gat) {
                Type ct = gat.getGenericComponentType();
                Class<?> cc = null;
                if (ct instanceof Class<?> clz) {
                    cc = clz;
                }
                if (ct instanceof ParameterizedType pt) {
                    cc = (Class<?>) pt.getRawType();
                }
                Object o = Array.newInstance(cc, 0);
                parameter = ArrayFrob.convert(parameter, o.getClass());
            } else if (_type instanceof Class<?> clz && ((Class<?>) _type).isArray()) {
                Class<?> cc = clz.getComponentType();
                if ((cc.equals(Float.class) || cc.equals(Float.TYPE)) && parameter instanceof double[] dbArr) {
                    float[] tmp2 = new float[dbArr.length];
                    for (int i = 0; i < dbArr.length; i++) {
                        tmp2[i] = (float) dbArr[i];
                    }
                    parameter = tmp2;
                }
                Object o = Array.newInstance(cc, 0);
                parameter = ArrayFrob.convert(parameter, o.getClass());
            }
        }
        if (parameter instanceof Map<?, ?> dmap) {
            LOGGER.trace("Deserializing a Map");

            Type[] maptypes;
            if (_type instanceof ParameterizedType pt) {
                maptypes = pt.getActualTypeArguments();
            } else {
                maptypes = parameter.getClass().getTypeParameters();
            }

            Map<Object, Object> map = new LinkedHashMap<>();
            for (Entry<?, ?> e : dmap.entrySet()) {
                map.put(deSerializeParameter(e.getKey(), maptypes[0], _conn),
                    deSerializeParameter(e.getValue(), maptypes[1], _conn));
            }

            parameter = map;
        }
        return parameter;
    }

    static List<Object> deSerializeParameters(List<Object> _parameters, Type _type, AbstractConnectionBase _conn) throws Exception {
        LOGGER.trace("Deserializing from {} to {}", _parameters, _type);
        if (_parameters == null) {
            return null;
        }
        for (int i = 0; i < _parameters.size(); i++) {
            if (_parameters.get(i) == null) {
                continue;
            }

            _parameters.set(i, deSerializeParameter(_parameters.get(i), _type, _conn));
        }
        return _parameters;
    }

    @SuppressWarnings("unchecked")
    public static Object[] deSerializeParameters(Object[] _parameters, Type[] _types, AbstractConnectionBase _conn) throws Exception {
        LoggingHelper.logIf(LOGGER.isTraceEnabled(), () -> LOGGER.trace("Deserializing from {} to {} ", Arrays.deepToString(_parameters), Arrays.deepToString(_types)));

        if (null == _parameters) {
            return null;
        }

        Object[] parameters = _parameters;
        Type[] types = _types;

        if (types.length == 1 && types[0] instanceof ParameterizedType pt
            && Tuple.class.isAssignableFrom((Class<?>) pt.getRawType())) {
            types = pt.getActualTypeArguments();
        }

        if (types.length == 1 && types[0] instanceof Class<?> clz && Tuple.class.isAssignableFrom(clz)) {
            String typeName = types[0].getTypeName();
            Constructor<?>[] constructors = Class.forName(typeName).getDeclaredConstructors();
            if (constructors.length != 1) {
                throw new DBusException("Error deserializing message: "
                        + "We had a Tuple type but wrong number of constructors for this Tuple. "
                        + "There should be exactly one.");
            }

            if (constructors[0].getParameterCount() != parameters.length) {
                throw new DBusException("Error deserializing message: "
                        + "We had a Tuple type but it had wrong number of constructor arguments. "
                        + "The number of constructor arguments should match the number of parameters to deserialize.");
            }

            Object o = constructors[0].newInstance(parameters);
            return new Object[] {o};
        }

        for (int i = 0; i < parameters.length; i++) {
            // CHECK IF ARRAYS HAVE THE SAME LENGTH <-- has to happen after expanding parameters
            if (i >= types.length) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Parameter length differs, expected {} but got {}", parameters.length, types.length);
                    for (int j = 0; j < parameters.length; j++) {
                        LOGGER.debug("Error, Parameters differ: {}, '{}'", j, parameters[j]);
                    }
                }
                throw new DBusException("Error deserializing message: number of parameters didn't match receiving signature");
            }
            if (null == parameters[i]) {
                continue;
            }

            if (types[i] instanceof Class
                    && DBusSerializable.class.isAssignableFrom((Class<? extends Object>) types[i])
                || types[i] instanceof ParameterizedType pt
                    && DBusSerializable.class.isAssignableFrom((Class<? extends Object>) pt.getRawType())) {
                Class<? extends DBusSerializable> dsc;
                if (types[i] instanceof Class) {
                    dsc = (Class<? extends DBusSerializable>) types[i];
                } else {
                    dsc = (Class<? extends DBusSerializable>) ((ParameterizedType) types[i]).getRawType();
                }
                for (Method m : dsc.getDeclaredMethods()) {
                    if (m.getName().equals(MTH_NAME_DESERIALIZE)) {
                        Type[] newtypes = m.getGenericParameterTypes();
                        try {
                            Object[] sub = new Object[newtypes.length];
                            System.arraycopy(parameters, i, sub, 0, newtypes.length);
                            sub = deSerializeParameters(sub, newtypes, _conn);
                            DBusSerializable sz = dsc.getDeclaredConstructor().newInstance();
                            m.invoke(sz, sub);
                            Object[] compress = new Object[parameters.length - newtypes.length + 1];
                            System.arraycopy(parameters, 0, compress, 0, i);
                            compress[i] = sz;
                            System.arraycopy(parameters, i + newtypes.length, compress, i + 1, parameters.length - i - newtypes.length);
                            parameters = compress;
                        } catch (ArrayIndexOutOfBoundsException _ex) {
                            LOGGER.debug("", _ex);
                            throw new DBusException(String.format("Not enough elements to create custom object from serialized data (%s < %s).", parameters.length - i, newtypes.length));
                        }
                    }
                }
            } else {
                parameters[i] = deSerializeParameter(parameters[i], types[i], _conn);
            }
        }
        return parameters;
    }
}
