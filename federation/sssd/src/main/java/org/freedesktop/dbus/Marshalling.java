/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import cx.ath.matthew.debug.Debug;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.DBusMapType;
import org.freedesktop.dbus.types.DBusStructType;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static org.freedesktop.dbus.Gettext.getString;

/**
 * Contains static methods for marshalling values.
 */
public class Marshalling {
    private static Map<Type, String[]> typeCache = new HashMap<Type, String[]>();

    /**
     * Will return the DBus type corresponding to the given Java type.
     * Note, container type should have their ParameterizedType not their
     * Class passed in here.
     *
     * @param c The Java types.
     * @return The DBus types.
     * @throws DBusException If the given type cannot be converted to a DBus type.
     */
    public static String getDBusType(Type[] c) throws DBusException {
        StringBuffer sb = new StringBuffer();
        for (Type t : c)
            for (String s : getDBusType(t))
                sb.append(s);
        return sb.toString();
    }

    /**
     * Will return the DBus type corresponding to the given Java type.
     * Note, container type should have their ParameterizedType not their
     * Class passed in here.
     *
     * @param c The Java type.
     * @return The DBus type.
     * @throws DBusException If the given type cannot be converted to a DBus type.
     */
    public static String[] getDBusType(Type c) throws DBusException {
        String[] cached = typeCache.get(c);
        if (null != cached) return cached;
        cached = getDBusType(c, false);
        typeCache.put(c, cached);
        return cached;
    }

    /**
     * Will return the DBus type corresponding to the given Java type.
     * Note, container type should have their ParameterizedType not their
     * Class passed in here.
     *
     * @param c     The Java type.
     * @param basic If true enforces this to be a non-compound type. (compound types are Maps, Structs and Lists/arrays).
     * @return The DBus type.
     * @throws DBusException If the given type cannot be converted to a DBus type.
     */
    public static String[] getDBusType(Type c, boolean basic) throws DBusException {
        return recursiveGetDBusType(c, basic, 0);
    }

    private static StringBuffer[] out = new StringBuffer[10];

    @SuppressWarnings("unchecked")
    public static String[] recursiveGetDBusType(Type c, boolean basic, int level) throws DBusException {
        if (out.length <= level) {
            StringBuffer[] newout = new StringBuffer[out.length];
            System.arraycopy(out, 0, newout, 0, out.length);
            out = newout;
        }
        if (null == out[level]) out[level] = new StringBuffer();
        else out[level].delete(0, out[level].length());

        if (basic && !(c instanceof Class))
            throw new DBusException(c + getString("notBasicType"));

        if (c instanceof TypeVariable) out[level].append((char) Message.ArgumentType.VARIANT);
        else if (c instanceof GenericArrayType) {
            out[level].append((char) Message.ArgumentType.ARRAY);
            String[] s = recursiveGetDBusType(((GenericArrayType) c).getGenericComponentType(), false, level + 1);
            if (s.length != 1) throw new DBusException(getString("multiValuedArrayNotPermitted"));
            out[level].append(s[0]);
        } else if ((c instanceof Class &&
                DBusSerializable.class.isAssignableFrom((Class<? extends Object>) c)) ||
                (c instanceof ParameterizedType &&
                        DBusSerializable.class.isAssignableFrom((Class<? extends Object>) ((ParameterizedType) c).getRawType()))) {
            // it's a custom serializable type
            Type[] newtypes = null;
            if (c instanceof Class) {
                for (Method m : ((Class<? extends Object>) c).getDeclaredMethods())
                    if (m.getName().equals("deserialize"))
                        newtypes = m.getGenericParameterTypes();
            } else
                for (Method m : ((Class<? extends Object>) ((ParameterizedType) c).getRawType()).getDeclaredMethods())
                    if (m.getName().equals("deserialize"))
                        newtypes = m.getGenericParameterTypes();

            if (null == newtypes) throw new DBusException(getString("mustImplementDeserializeMethod"));

            String[] sigs = new String[newtypes.length];
            for (int j = 0; j < sigs.length; j++) {
                String[] ss = recursiveGetDBusType(newtypes[j], false, level + 1);
                if (1 != ss.length) throw new DBusException(getString("mustSerializeNativeDBusTypes"));
                sigs[j] = ss[0];
            }
            return sigs;
        } else if (c instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType) c;
            if (p.getRawType().equals(Map.class)) {
                out[level].append("a{");
                Type[] t = p.getActualTypeArguments();
                try {
                    String[] s = recursiveGetDBusType(t[0], true, level + 1);
                    if (s.length != 1) throw new DBusException(getString("multiValuedArrayNotPermitted"));
                    out[level].append(s[0]);
                    s = recursiveGetDBusType(t[1], false, level + 1);
                    if (s.length != 1) throw new DBusException(getString("multiValuedArrayNotPermitted"));
                    out[level].append(s[0]);
                } catch (ArrayIndexOutOfBoundsException AIOOBe) {
                    if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, AIOOBe);
                    throw new DBusException(getString("mapParameters"));
                }
                out[level].append('}');
            } else if (List.class.isAssignableFrom((Class<? extends Object>) p.getRawType())) {
                for (Type t : p.getActualTypeArguments()) {
                    if (Type.class.equals(t))
                        out[level].append((char) Message.ArgumentType.SIGNATURE);
                    else {
                        String[] s = recursiveGetDBusType(t, false, level + 1);
                        if (s.length != 1) throw new DBusException(getString("multiValuedArrayNotPermitted"));
                        out[level].append((char) Message.ArgumentType.ARRAY);
                        out[level].append(s[0]);
                    }
                }
            } else if (p.getRawType().equals(Variant.class)) {
                out[level].append((char) Message.ArgumentType.VARIANT);
            } else if (DBusInterface.class.isAssignableFrom((Class<? extends Object>) p.getRawType())) {
                out[level].append((char) Message.ArgumentType.OBJECT_PATH);
            } else if (Tuple.class.isAssignableFrom((Class<? extends Object>) p.getRawType())) {
                Type[] ts = p.getActualTypeArguments();
                Vector<String> vs = new Vector<String>();
                for (Type t : ts)
                    for (String s : recursiveGetDBusType(t, false, level + 1))
                        vs.add(s);
                return vs.toArray(new String[0]);
            } else
                throw new DBusException(getString("nonExportableParameterizedType") + c);
        } else if (c.equals(Byte.class)) out[level].append((char) Message.ArgumentType.BYTE);
        else if (c.equals(Byte.TYPE)) out[level].append((char) Message.ArgumentType.BYTE);
        else if (c.equals(Boolean.class)) out[level].append((char) Message.ArgumentType.BOOLEAN);
        else if (c.equals(Boolean.TYPE)) out[level].append((char) Message.ArgumentType.BOOLEAN);
        else if (c.equals(Short.class)) out[level].append((char) Message.ArgumentType.INT16);
        else if (c.equals(Short.TYPE)) out[level].append((char) Message.ArgumentType.INT16);
        else if (c.equals(UInt16.class)) out[level].append((char) Message.ArgumentType.UINT16);
        else if (c.equals(Integer.class)) out[level].append((char) Message.ArgumentType.INT32);
        else if (c.equals(Integer.TYPE)) out[level].append((char) Message.ArgumentType.INT32);
        else if (c.equals(UInt32.class)) out[level].append((char) Message.ArgumentType.UINT32);
        else if (c.equals(Long.class)) out[level].append((char) Message.ArgumentType.INT64);
        else if (c.equals(Long.TYPE)) out[level].append((char) Message.ArgumentType.INT64);
        else if (c.equals(UInt64.class)) out[level].append((char) Message.ArgumentType.UINT64);
        else if (c.equals(Double.class)) out[level].append((char) Message.ArgumentType.DOUBLE);
        else if (c.equals(Double.TYPE)) out[level].append((char) Message.ArgumentType.DOUBLE);
        else if (c.equals(Float.class) && AbstractConnection.FLOAT_SUPPORT)
            out[level].append((char) Message.ArgumentType.FLOAT);
        else if (c.equals(Float.class)) out[level].append((char) Message.ArgumentType.DOUBLE);
        else if (c.equals(Float.TYPE) && AbstractConnection.FLOAT_SUPPORT)
            out[level].append((char) Message.ArgumentType.FLOAT);
        else if (c.equals(Float.TYPE)) out[level].append((char) Message.ArgumentType.DOUBLE);
        else if (c.equals(String.class)) out[level].append((char) Message.ArgumentType.STRING);
        else if (c.equals(Variant.class)) out[level].append((char) Message.ArgumentType.VARIANT);
        else if (c instanceof Class &&
                DBusInterface.class.isAssignableFrom((Class<? extends Object>) c))
            out[level].append((char) Message.ArgumentType.OBJECT_PATH);
        else if (c instanceof Class &&
                Path.class.equals((Class<? extends Object>) c))
            out[level].append((char) Message.ArgumentType.OBJECT_PATH);
        else if (c instanceof Class &&
                ObjectPath.class.equals((Class<? extends Object>) c))
            out[level].append((char) Message.ArgumentType.OBJECT_PATH);
        else if (c instanceof Class &&
                ((Class<? extends Object>) c).isArray()) {
            if (Type.class.equals(((Class<? extends Object>) c).getComponentType()))
                out[level].append((char) Message.ArgumentType.SIGNATURE);
            else {
                out[level].append((char) Message.ArgumentType.ARRAY);
                String[] s = recursiveGetDBusType(((Class<? extends Object>) c).getComponentType(), false, level + 1);
                if (s.length != 1) throw new DBusException(getString("multiValuedArrayNotPermitted"));
                out[level].append(s[0]);
            }
        } else if (c instanceof Class &&
                Struct.class.isAssignableFrom((Class<? extends Object>) c)) {
            out[level].append((char) Message.ArgumentType.STRUCT1);
            Type[] ts = Container.getTypeCache(c);
            if (null == ts) {
                Field[] fs = ((Class<? extends Object>) c).getDeclaredFields();
                ts = new Type[fs.length];
                for (Field f : fs) {
                    Position p = f.getAnnotation(Position.class);
                    if (null == p) continue;
                    ts[p.value()] = f.getGenericType();
                }
                Container.putTypeCache(c, ts);
            }

            for (Type t : ts)
                if (t != null)
                    for (String s : recursiveGetDBusType(t, false, level + 1))
                        out[level].append(s);
            out[level].append(')');
        } else {
            throw new DBusException(getString("nonExportableType") + c);
        }

        if (Debug.debug) Debug.print(Debug.VERBOSE, "Converted Java type: " + c + " to D-Bus Type: " + out[level]);

        return new String[]{out[level].toString()};
    }

    /**
     * Converts a dbus type string into Java Type objects,
     *
     * @param dbus  The DBus type or types.
     * @param rv    Vector to return the types in.
     * @param limit Maximum number of types to parse (-1 == nolimit).
     * @return number of characters parsed from the type string.
     */
    public static int getJavaType(String dbus, List<Type> rv, int limit) throws DBusException {
        if (null == dbus || "".equals(dbus) || 0 == limit) return 0;

        try {
            int i = 0;
            for (; i < dbus.length() && (-1 == limit || limit > rv.size()); i++)
                switch (dbus.charAt(i)) {
                    case Message.ArgumentType.STRUCT1:
                        int j = i + 1;
                        for (int c = 1; c > 0; j++) {
                            if (')' == dbus.charAt(j)) c--;
                            else if (Message.ArgumentType.STRUCT1 == dbus.charAt(j)) c++;
                        }

                        Vector<Type> contained = new Vector<Type>();
                        int c = getJavaType(dbus.substring(i + 1, j - 1), contained, -1);
                        rv.add(new DBusStructType(contained.toArray(new Type[0])));
                        i = j;
                        break;
                    case Message.ArgumentType.ARRAY:
                        if (Message.ArgumentType.DICT_ENTRY1 == dbus.charAt(i + 1)) {
                            contained = new Vector<Type>();
                            c = getJavaType(dbus.substring(i + 2), contained, 2);
                            rv.add(new DBusMapType(contained.get(0), contained.get(1)));
                            i += (c + 2);
                        } else {
                            contained = new Vector<Type>();
                            c = getJavaType(dbus.substring(i + 1), contained, 1);
                            rv.add(new DBusListType(contained.get(0)));
                            i += c;
                        }
                        break;
                    case Message.ArgumentType.VARIANT:
                        rv.add(Variant.class);
                        break;
                    case Message.ArgumentType.BOOLEAN:
                        rv.add(Boolean.class);
                        break;
                    case Message.ArgumentType.INT16:
                        rv.add(Short.class);
                        break;
                    case Message.ArgumentType.BYTE:
                        rv.add(Byte.class);
                        break;
                    case Message.ArgumentType.OBJECT_PATH:
                        rv.add(DBusInterface.class);
                        break;
                    case Message.ArgumentType.UINT16:
                        rv.add(UInt16.class);
                        break;
                    case Message.ArgumentType.INT32:
                        rv.add(Integer.class);
                        break;
                    case Message.ArgumentType.UINT32:
                        rv.add(UInt32.class);
                        break;
                    case Message.ArgumentType.INT64:
                        rv.add(Long.class);
                        break;
                    case Message.ArgumentType.UINT64:
                        rv.add(UInt64.class);
                        break;
                    case Message.ArgumentType.DOUBLE:
                        rv.add(Double.class);
                        break;
                    case Message.ArgumentType.FLOAT:
                        rv.add(Float.class);
                        break;
                    case Message.ArgumentType.STRING:
                        rv.add(String.class);
                        break;
                    case Message.ArgumentType.SIGNATURE:
                        rv.add(Type[].class);
                        break;
                    case Message.ArgumentType.DICT_ENTRY1:
                        rv.add(Map.Entry.class);
                        contained = new Vector<Type>();
                        c = getJavaType(dbus.substring(i + 1), contained, 2);
                        i += c + 1;
                        break;
                    default:
                        throw new DBusException(MessageFormat.format(getString("parseDBusSignatureFailure"), new Object[]{dbus, dbus.charAt(i)}));
                }
            return i;
        } catch (IndexOutOfBoundsException IOOBe) {
            if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, IOOBe);
            throw new DBusException(getString("parseDBusTypeSignatureFailure") + dbus);
        }
    }

    /**
     * Recursively converts types for serialization onto DBus.
     *
     * @param parameters The parameters to convert.
     * @param types      The (possibly generic) types of the parameters.
     * @return The converted parameters.
     * @throws DBusException Thrown if there is an error in converting the objects.
     */
    @SuppressWarnings("unchecked")
    public static Object[] convertParameters(Object[] parameters, Type[] types, AbstractConnection conn) throws DBusException {
        if (null == parameters) return null;
        for (int i = 0; i < parameters.length; i++) {
            if (Debug.debug)
                Debug.print(Debug.VERBOSE, "Converting " + i + " from " + parameters[i] + " to " + types[i]);
            if (null == parameters[i]) continue;

            if (parameters[i] instanceof DBusSerializable) {
                for (Method m : parameters[i].getClass().getDeclaredMethods())
                    if (m.getName().equals("deserialize")) {
                        Type[] newtypes = m.getParameterTypes();
                        Type[] expand = new Type[types.length + newtypes.length - 1];
                        System.arraycopy(types, 0, expand, 0, i);
                        System.arraycopy(newtypes, 0, expand, i, newtypes.length);
                        System.arraycopy(types, i + 1, expand, i + newtypes.length, types.length - i - 1);
                        types = expand;
                        Object[] newparams = ((DBusSerializable) parameters[i]).serialize();
                        Object[] exparams = new Object[parameters.length + newparams.length - 1];
                        System.arraycopy(parameters, 0, exparams, 0, i);
                        System.arraycopy(newparams, 0, exparams, i, newparams.length);
                        System.arraycopy(parameters, i + 1, exparams, i + newparams.length, parameters.length - i - 1);
                        parameters = exparams;
                    }
                i--;
            } else if (parameters[i] instanceof Tuple) {
                Type[] newtypes = ((ParameterizedType) types[i]).getActualTypeArguments();
                Type[] expand = new Type[types.length + newtypes.length - 1];
                System.arraycopy(types, 0, expand, 0, i);
                System.arraycopy(newtypes, 0, expand, i, newtypes.length);
                System.arraycopy(types, i + 1, expand, i + newtypes.length, types.length - i - 1);
                types = expand;
                Object[] newparams = ((Tuple) parameters[i]).getParameters();
                Object[] exparams = new Object[parameters.length + newparams.length - 1];
                System.arraycopy(parameters, 0, exparams, 0, i);
                System.arraycopy(newparams, 0, exparams, i, newparams.length);
                System.arraycopy(parameters, i + 1, exparams, i + newparams.length, parameters.length - i - 1);
                parameters = exparams;
                if (Debug.debug)
                    Debug.print(Debug.VERBOSE, "New params: " + Arrays.deepToString(parameters) + " new types: " + Arrays.deepToString(types));
                i--;
            } else if (types[i] instanceof TypeVariable &&
                    !(parameters[i] instanceof Variant))
                // its an unwrapped variant, wrap it
                parameters[i] = new Variant<Object>(parameters[i]);
            else if (parameters[i] instanceof DBusInterface)
                parameters[i] = conn.getExportedObject((DBusInterface) parameters[i]);
        }
        return parameters;
    }

    @SuppressWarnings("unchecked")
    static Object deSerializeParameter(Object parameter, Type type, AbstractConnection conn) throws Exception {
        if (Debug.debug)
            Debug.print(Debug.VERBOSE, "Deserializing from " + parameter.getClass() + " to " + type.getClass());
        if (null == parameter)
            return null;

        // its a wrapped variant, unwrap it
        if (type instanceof TypeVariable
                && parameter instanceof Variant) {
            parameter = ((Variant) parameter).getValue();
        }

        // Turn a signature into a Type[]
        if (type instanceof Class
                && ((Class) type).isArray()
                && ((Class) type).getComponentType().equals(Type.class)
                && parameter instanceof String) {
            Vector<Type> rv = new Vector<Type>();
            getJavaType((String) parameter, rv, -1);
            parameter = rv.toArray(new Type[0]);
        }

        // its an object path, get/create the proxy
        if (parameter instanceof ObjectPath) {
            if (type instanceof Class && DBusInterface.class.isAssignableFrom((Class) type))
                parameter = conn.getExportedObject(
                        ((ObjectPath) parameter).source,
                        ((ObjectPath) parameter).path);
            else
                parameter = new Path(((ObjectPath) parameter).path);
        }

        // it should be a struct. create it
        if (parameter instanceof Object[] &&
                type instanceof Class &&
                Struct.class.isAssignableFrom((Class) type)) {
            if (Debug.debug) Debug.print(Debug.VERBOSE, "Creating Struct " + type + " from " + parameter);
            Type[] ts = Container.getTypeCache(type);
            if (null == ts) {
                Field[] fs = ((Class) type).getDeclaredFields();
                ts = new Type[fs.length];
                for (Field f : fs) {
                    Position p = f.getAnnotation(Position.class);
                    if (null == p) continue;
                    ts[p.value()] = f.getGenericType();
                }
                Container.putTypeCache(type, ts);
            }

            // recurse over struct contents
            parameter = deSerializeParameters((Object[]) parameter, ts, conn);
            for (Constructor con : ((Class) type).getDeclaredConstructors()) {
                try {
                    parameter = con.newInstance((Object[]) parameter);
                    break;
                } catch (IllegalArgumentException IAe) {
                }
            }
        }

        // recurse over arrays
        if (parameter instanceof Object[]) {
            Type[] ts = new Type[((Object[]) parameter).length];
            Arrays.fill(ts, parameter.getClass().getComponentType());
            parameter = deSerializeParameters((Object[]) parameter,
                    ts, conn);
        }
        if (parameter instanceof List) {
            Type type2;
            if (type instanceof ParameterizedType)
                type2 = ((ParameterizedType) type).getActualTypeArguments()[0];
            else if (type instanceof GenericArrayType)
                type2 = ((GenericArrayType) type).getGenericComponentType();
            else if (type instanceof Class && ((Class) type).isArray())
                type2 = ((Class) type).getComponentType();
            else
                type2 = null;
            if (null != type2)
                parameter = deSerializeParameters((List) parameter, type2, conn);
        }

        // correct floats if appropriate
        if (type.equals(Float.class) || type.equals(Float.TYPE))
            if (!(parameter instanceof Float))
                parameter = ((Number) parameter).floatValue();

        // make sure arrays are in the correct format
        if (parameter instanceof Object[] ||
                parameter instanceof List ||
                parameter.getClass().isArray()) {
            if (type instanceof ParameterizedType)
                parameter = ArrayFrob.convert(parameter,
                        (Class<? extends Object>) ((ParameterizedType) type).getRawType());
            else if (type instanceof GenericArrayType) {
                Type ct = ((GenericArrayType) type).getGenericComponentType();
                Class cc = null;
                if (ct instanceof Class)
                    cc = (Class) ct;
                if (ct instanceof ParameterizedType)
                    cc = (Class) ((ParameterizedType) ct).getRawType();
                Object o = Array.newInstance(cc, 0);
                parameter = ArrayFrob.convert(parameter,
                        o.getClass());
            } else if (type instanceof Class &&
                    ((Class) type).isArray()) {
                Class cc = ((Class) type).getComponentType();
                if ((cc.equals(Float.class) || cc.equals(Float.TYPE))
                        && (parameter instanceof double[])) {
                    double[] tmp1 = (double[]) parameter;
                    float[] tmp2 = new float[tmp1.length];
                    for (int i = 0; i < tmp1.length; i++)
                        tmp2[i] = (float) tmp1[i];
                    parameter = tmp2;
                }
                Object o = Array.newInstance(cc, 0);
                parameter = ArrayFrob.convert(parameter,
                        o.getClass());
            }
        }
        if (parameter instanceof DBusMap) {
            if (Debug.debug) Debug.print(Debug.VERBOSE, "Deserializing a Map");
            DBusMap dmap = (DBusMap) parameter;
            Type[] maptypes = ((ParameterizedType) type).getActualTypeArguments();
            for (int i = 0; i < dmap.entries.length; i++) {
                dmap.entries[i][0] = deSerializeParameter(dmap.entries[i][0], maptypes[0], conn);
                dmap.entries[i][1] = deSerializeParameter(dmap.entries[i][1], maptypes[1], conn);
            }
        }
        return parameter;
    }

    static List<Object> deSerializeParameters(List<Object> parameters, Type type, AbstractConnection conn) throws Exception {
        if (Debug.debug) Debug.print(Debug.VERBOSE, "Deserializing from " + parameters + " to " + type);
        if (null == parameters) return null;
        for (int i = 0; i < parameters.size(); i++) {
            if (null == parameters.get(i)) continue;

         /* DO NOT DO THIS! IT'S REALLY NOT SUPPORTED! 
          * if (type instanceof Class &&
               DBusSerializable.class.isAssignableFrom((Class) types[i])) {
            for (Method m: ((Class) types[i]).getDeclaredMethods()) 
               if (m.getName().equals("deserialize")) {
                  Type[] newtypes = m.getGenericParameterTypes();
                  try {
                     Object[] sub = new Object[newtypes.length];
                     System.arraycopy(parameters, i, sub, 0, newtypes.length); 
                     sub = deSerializeParameters(sub, newtypes, conn);
                     DBusSerializable sz = (DBusSerializable) ((Class) types[i]).newInstance();
                     m.invoke(sz, sub);
                     Object[] compress = new Object[parameters.length - newtypes.length + 1];
                     System.arraycopy(parameters, 0, compress, 0, i);
                     compress[i] = sz;
                     System.arraycopy(parameters, i + newtypes.length, compress, i+1, parameters.length - i - newtypes.length);
                     parameters = compress;
                  } catch (ArrayIndexOutOfBoundsException AIOOBe) {
                     if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, AIOOBe);
                     throw new DBusException("Not enough elements to create custom object from serialized data ("+(parameters.size()-i)+" < "+(newtypes.length)+")");
                  }
               }
         } else*/
            parameters.set(i, deSerializeParameter(parameters.get(i), type, conn));
        }
        return parameters;
    }

    @SuppressWarnings("unchecked")
    static Object[] deSerializeParameters(Object[] parameters, Type[] types, AbstractConnection conn) throws Exception {
        if (Debug.debug)
            Debug.print(Debug.VERBOSE, "Deserializing from " + Arrays.deepToString(parameters) + " to " + Arrays.deepToString(types));
        if (null == parameters) return null;

        if (types.length == 1 && types[0] instanceof ParameterizedType
                && Tuple.class.isAssignableFrom((Class) ((ParameterizedType) types[0]).getRawType())) {
            types = ((ParameterizedType) types[0]).getActualTypeArguments();
        }

        for (int i = 0; i < parameters.length; i++) {
            // CHECK IF ARRAYS HAVE THE SAME LENGTH <-- has to happen after expanding parameters
            if (i >= types.length) {
                if (Debug.debug) {
                    for (int j = 0; j < parameters.length; j++) {
                        Debug.print(Debug.ERR, String.format("Error, Parameters difference (%1d, '%2s')", j, parameters[j].toString()));
                    }
                }
                throw new DBusException(getString("errorDeserializingMessage"));
            }
            if (null == parameters[i]) continue;

            if ((types[i] instanceof Class &&
                    DBusSerializable.class.isAssignableFrom((Class<? extends Object>) types[i])) ||
                    (types[i] instanceof ParameterizedType &&
                            DBusSerializable.class.isAssignableFrom((Class<? extends Object>) ((ParameterizedType) types[i]).getRawType()))) {
                Class<? extends DBusSerializable> dsc;
                if (types[i] instanceof Class)
                    dsc = (Class<? extends DBusSerializable>) types[i];
                else
                    dsc = (Class<? extends DBusSerializable>) ((ParameterizedType) types[i]).getRawType();
                for (Method m : dsc.getDeclaredMethods())
                    if (m.getName().equals("deserialize")) {
                        Type[] newtypes = m.getGenericParameterTypes();
                        try {
                            Object[] sub = new Object[newtypes.length];
                            System.arraycopy(parameters, i, sub, 0, newtypes.length);
                            sub = deSerializeParameters(sub, newtypes, conn);
                            DBusSerializable sz = dsc.newInstance();
                            m.invoke(sz, sub);
                            Object[] compress = new Object[parameters.length - newtypes.length + 1];
                            System.arraycopy(parameters, 0, compress, 0, i);
                            compress[i] = sz;
                            System.arraycopy(parameters, i + newtypes.length, compress, i + 1, parameters.length - i - newtypes.length);
                            parameters = compress;
                        } catch (ArrayIndexOutOfBoundsException AIOOBe) {
                            if (AbstractConnection.EXCEPTION_DEBUG && Debug.debug) Debug.print(Debug.ERR, AIOOBe);
                            throw new DBusException(MessageFormat.format(getString("notEnoughElementsToCreateCustomObject"),
                                    new Object[]{parameters.length - i, newtypes.length}));
                        }
                    }
            } else
                parameters[i] = deSerializeParameter(parameters[i], types[i], conn);
        }
        return parameters;
    }
}


