/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static org.freedesktop.dbus.Gettext.getString;

class ExportedObject {
    @SuppressWarnings("unchecked")
    private String getAnnotations(AnnotatedElement c) {
        String ans = "";
        for (Annotation a : c.getDeclaredAnnotations()) {
            Class t = a.annotationType();
            String value = "";
            try {
                Method m = t.getMethod("value");
                value = m.invoke(a).toString();
            } catch (NoSuchMethodException NSMe) {
            } catch (InvocationTargetException ITe) {
            } catch (IllegalAccessException IAe) {
            }

            ans += "  <annotation name=\"" + AbstractConnection.dollar_pattern.matcher(t.getName()).replaceAll(".") + "\" value=\"" + value + "\" />\n";
        }
        return ans;
    }

    @SuppressWarnings("unchecked")
    private Map<MethodTuple, Method> getExportedMethods(Class c) throws DBusException {
        if (DBusInterface.class.equals(c)) return new HashMap<MethodTuple, Method>();
        Map<MethodTuple, Method> m = new HashMap<MethodTuple, Method>();
        for (Class i : c.getInterfaces())
            if (DBusInterface.class.equals(i)) {
                // add this class's public methods
                if (null != c.getAnnotation(DBusInterfaceName.class)) {
                    String name = ((DBusInterfaceName) c.getAnnotation(DBusInterfaceName.class)).value();
                    introspectiondata += " <interface name=\"" + name + "\">\n";
                    DBusSignal.addInterfaceMap(c.getName(), name);
                } else {
                    // don't let people export things which don't have a
                    // valid D-Bus interface name
                    if (c.getName().equals(c.getSimpleName()))
                        throw new DBusException(getString("interfaceNotAllowedOutsidePackage"));
                    if (c.getName().length() > DBusConnection.MAX_NAME_LENGTH)
                        throw new DBusException(getString("introspectInterfaceExceedCharacters") + c.getName());
                    else
                        introspectiondata += " <interface name=\"" + AbstractConnection.dollar_pattern.matcher(c.getName()).replaceAll(".") + "\">\n";
                }
                introspectiondata += getAnnotations(c);
                for (Method meth : c.getDeclaredMethods())
                    if (Modifier.isPublic(meth.getModifiers())) {
                        String ms = "";
                        String name;
                        if (meth.isAnnotationPresent(DBusMemberName.class))
                            name = meth.getAnnotation(DBusMemberName.class).value();
                        else
                            name = meth.getName();
                        if (name.length() > DBusConnection.MAX_NAME_LENGTH)
                            throw new DBusException(getString("introspectMethodExceedCharacters") + name);
                        introspectiondata += "  <method name=\"" + name + "\" >\n";
                        introspectiondata += getAnnotations(meth);
                        for (Class ex : meth.getExceptionTypes())
                            if (DBusExecutionException.class.isAssignableFrom(ex))
                                introspectiondata +=
                                        "   <annotation name=\"org.freedesktop.DBus.Method.Error\" value=\"" + AbstractConnection.dollar_pattern.matcher(ex.getName()).replaceAll(".") + "\" />\n";
                        for (Type pt : meth.getGenericParameterTypes())
                            for (String s : Marshalling.getDBusType(pt)) {
                                introspectiondata += "   <arg type=\"" + s + "\" direction=\"in\"/>\n";
                                ms += s;
                            }
                        if (!Void.TYPE.equals(meth.getGenericReturnType())) {
                            if (Tuple.class.isAssignableFrom((Class) meth.getReturnType())) {
                                ParameterizedType tc = (ParameterizedType) meth.getGenericReturnType();
                                Type[] ts = tc.getActualTypeArguments();

                                for (Type t : ts)
                                    if (t != null)
                                        for (String s : Marshalling.getDBusType(t))
                                            introspectiondata += "   <arg type=\"" + s + "\" direction=\"out\"/>\n";
                            } else if (Object[].class.equals(meth.getGenericReturnType())) {
                                throw new DBusException(getString("cannotIntrospectReturnType"));
                            } else
                                for (String s : Marshalling.getDBusType(meth.getGenericReturnType()))
                                    introspectiondata += "   <arg type=\"" + s + "\" direction=\"out\"/>\n";
                        }
                        introspectiondata += "  </method>\n";
                        m.put(new MethodTuple(name, ms), meth);
                    }
                for (Class sig : c.getDeclaredClasses())
                    if (DBusSignal.class.isAssignableFrom(sig)) {
                        String name;
                        if (sig.isAnnotationPresent(DBusMemberName.class)) {
                            name = ((DBusMemberName) sig.getAnnotation(DBusMemberName.class)).value();
                            DBusSignal.addSignalMap(sig.getSimpleName(), name);
                        } else
                            name = sig.getSimpleName();
                        if (name.length() > DBusConnection.MAX_NAME_LENGTH)
                            throw new DBusException(getString("introspectSignalExceedCharacters") + name);
                        introspectiondata += "  <signal name=\"" + name + "\">\n";
                        Constructor con = sig.getConstructors()[0];
                        Type[] ts = con.getGenericParameterTypes();
                        for (int j = 1; j < ts.length; j++)
                            for (String s : Marshalling.getDBusType(ts[j]))
                                introspectiondata += "   <arg type=\"" + s + "\" direction=\"out\" />\n";
                        introspectiondata += getAnnotations(sig);
                        introspectiondata += "  </signal>\n";

                    }
                introspectiondata += " </interface>\n";
            } else {
                // recurse
                m.putAll(getExportedMethods(i));
            }
        return m;
    }

    Map<MethodTuple, Method> methods;
    Reference<DBusInterface> object;
    String introspectiondata;

    public ExportedObject(DBusInterface object, boolean weakreferences) throws DBusException {
        if (weakreferences)
            this.object = new WeakReference<DBusInterface>(object);
        else
            this.object = new StrongReference<DBusInterface>(object);
        introspectiondata = "";
        methods = getExportedMethods(object.getClass());
        introspectiondata +=
                " <interface name=\"org.freedesktop.DBus.Introspectable\">\n" +
                        "  <method name=\"Introspect\">\n" +
                        "   <arg type=\"s\" direction=\"out\"/>\n" +
                        "  </method>\n" +
                        " </interface>\n";
        introspectiondata +=
                " <interface name=\"org.freedesktop.DBus.Peer\">\n" +
                        "  <method name=\"Ping\">\n" +
                        "  </method>\n" +
                        " </interface>\n";
    }
}


