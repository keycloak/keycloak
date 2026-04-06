package org.freedesktop.dbus.messages;

import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import org.freedesktop.dbus.Container;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.MethodTuple;
import org.freedesktop.dbus.StrongReference;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusIgnore;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.annotations.DBusProperties;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.annotations.PropertiesEmitsChangedSignal;
import org.freedesktop.dbus.annotations.PropertiesEmitsChangedSignal.EmitChangeSignal;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Peer;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.propertyref.PropertyRef;
import org.freedesktop.dbus.utils.DBusNamingUtil;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.LoggerFactory;

public class ExportedObject {
    private final Map<MethodTuple, Method> methods;
    private final Map<PropertyRef, Method> propertyMethods;
    private final String                   introspectionData;
    private final Reference<DBusInterface> object;
    private final Set<Class<?>>            implementedInterfaces;

    public ExportedObject(DBusInterface _object, boolean _weakreferences) throws DBusException {
        object = _weakreferences ? new WeakReference<>(_object) : new StrongReference<>(_object);

        methods = new HashMap<>();
        propertyMethods = new HashMap<>();

        implementedInterfaces = getDBusInterfaces(_object.getClass());
        implementedInterfaces.add(Introspectable.class);
        implementedInterfaces.add(Peer.class);

        this.introspectionData = generateIntrospectionXml(implementedInterfaces);
    }

    /**
     * Generates the introspection data xml for annotations
     *
     * @param _c input interface/method/signal
     * @return xml with annotation definition
     */
    protected String generateAnnotationsXml(AnnotatedElement _c) {
        StringBuilder ans = new StringBuilder();
        for (Annotation a : _c.getDeclaredAnnotations()) {

            if (!a.annotationType().isAnnotationPresent(DBusInterfaceName.class)) {
                // skip all interfaces not compatible with
                // DBusInterface (mother of all DBus
                // related interfaces)
                continue;
            }
            Class<? extends Annotation> t = a.annotationType();
            String value = "";
            try {
                Method m = t.getMethod("value");
                value = m.invoke(a).toString();
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException _ex) {
                LoggerFactory.getLogger(getClass()).trace("Could not find value", _ex);
            }

            String name = DBusNamingUtil.getAnnotationName(t);
            ans.append("  <annotation name=\"").append(name)
                    .append("\" value=\"").append(value).append("\" />\n");
        }
        return ans.toString();
    }

    /**
     * Generates the introspection data for the single property.
     *
     * @param _property input property annotation
     * @return xml with property definition
     * @throws DBusException in case of unknown data types
     */
    protected String generatePropertyXml(DBusProperty _property) throws DBusException {
        return generatePropertyXml(_property.name(), _property.type(), _property.access(), _property.emitChangeSignal());
    }

    /**
     * Generates the introspection data for the single property.
     *
     * @param _propertyName name
     * @param _propertyTypeClass type class
     * @param _access type access
     * @return xml with property definition
     * @throws DBusException in case of unknown data types
     */
    protected String generatePropertyXml(String _propertyName, Class<?> _propertyTypeClass, Access _access, EmitChangeSignal _emitChangeSignal) throws DBusException {
        String propertyTypeString;
        if (TypeRef.class.isAssignableFrom(_propertyTypeClass)) {
            Type actualType = Optional.ofNullable(Util.unwrapTypeRef(_propertyTypeClass))
                .orElseThrow(() ->
                    new DBusException("Could not read TypeRef type for property '" + _propertyName + "'")
            );
            propertyTypeString = Marshalling.getDBusType(new Type[]{actualType});
        } else if (List.class.equals(_propertyTypeClass)) {
            // default non generic list types
            propertyTypeString = "av";
        } else if (Map.class.equals(_propertyTypeClass)) {
            // default non generic map type
            propertyTypeString = "a{vv}";
        } else {
            propertyTypeString = Marshalling.getDBusType(new Type[]{_propertyTypeClass});
        }

        String access = _access.getAccessName();
        if (_emitChangeSignal != null && _emitChangeSignal != EmitChangeSignal.TRUE) {
            return "<property name=\"" + _propertyName + "\" type=\"" + propertyTypeString + "\" access=\"" + access + "\">"
                    + "\n    <annotation name=\"org.freedesktop.DBus.Property.EmitsChangedSignal\" value=\"" + _emitChangeSignal.name().toLowerCase()
                    + "\"/>\n  </property>\n";
        }
        return "<property name=\"" + _propertyName + "\" type=\"" + propertyTypeString + "\" access=\"" + access + "\" />";
    }

    /**
     * Generates the introspection data for the input interface properties.
     *
     * @param _clz input interface
     * @return xml with property definitions
     * @throws DBusException in case of unknown data types
     */
    protected String generatePropertiesXml(Class<?> _clz) throws DBusException {

        EmitChangeSignal globalChangeSignal = Optional.ofNullable(_clz.getAnnotation(PropertiesEmitsChangedSignal.class))
            .map(e -> e.value())
            .orElse(EmitChangeSignal.TRUE);

        StringBuilder xml = new StringBuilder();
        Map<String, PropertyRef> map = new HashMap<>();
        DBusProperties properties = _clz.getAnnotation(DBusProperties.class);
        if (properties != null) {
            for (DBusProperty property : properties.value()) {
                if (map.containsKey(property.name())) {
                    throw new DBusException(MessageFormat.format(
                        "Property ''{0}'' defined multiple times.", property.name()));
                }
                map.put(property.name(), new PropertyRef(property));
            }
        }

        DBusProperty property = _clz.getAnnotation(DBusProperty.class);
        if (property != null) {
            if (map.containsKey(property.name())) {
                throw new DBusException(MessageFormat.format(
                    "Property ''{0}'' defined multiple times.", property.name()));
            }
            map.put(property.name(), new PropertyRef(property));
        }

        for (Method method : _clz.getDeclaredMethods()) {
            DBusBoundProperty propertyAnnot = method.getAnnotation(DBusBoundProperty.class);
            if (propertyAnnot != null) {
                String name = DBusNamingUtil.getPropertyName(method);
                Access access = PropertyRef.accessForMethod(method);
                PropertyRef.checkMethod(method);
                Class<?> type = PropertyRef.typeForMethod(method);
                PropertyRef ref = new PropertyRef(name, type, access);
                propertyMethods.put(ref, method);
                if (map.containsKey(name)) {
                    PropertyRef existing = map.get(name);
                    if (access.equals(existing.getAccess())) {
                        throw new DBusException(MessageFormat.format(
                            "Property ''{0}'' has access mode ''{1}'' defined multiple times.", name, access));
                    } else {
                        // if the signal of the annotation is null or the same as the default (TRUE),
                        // use whatever the global annotation defines
                        // this means DBusBoundProperty has precedence over the global annotation
                        EmitChangeSignal emitSignal = Optional.ofNullable(propertyAnnot.emitChangeSignal())
                            .filter(s -> s == globalChangeSignal)
                            .orElse(globalChangeSignal);

                        map.put(name, new PropertyRef(name, type, Access.READ_WRITE, emitSignal));
                    }
                } else {
                    map.put(name, ref);
                }
            }
        }

        for (PropertyRef ref : map.values()) {
            xml.append("  ").append(generatePropertyXml(ref.getName(), ref.getType(), ref.getAccess(), ref.getEmitChangeSignal())).append("\n");
        }

        return xml.toString();
    }

    /**
     * Generates the introspection data for the input interface methods
     *
     * @param _clz input interface
     * @return xml with method definitions
     *
     * @throws DBusException if marshalling fails
     */
    protected String generateMethodsXml(Class<?> _clz) throws DBusException {
        StringBuilder sb = new StringBuilder();
        for (Method meth : _clz.getDeclaredMethods()) {
            if (isExcluded(meth)) {
                continue;
            }
            String methodName = DBusNamingUtil.getMethodName(meth);
            if (methodName.length() > AbstractConnection.MAX_NAME_LENGTH) {
                throw new DBusException(
                        "Introspected method name exceeds 255 characters. Cannot export objects with method "
                                + methodName);
            }
            sb.append("  <method name=\"").append(methodName).append("\" >\n");
            sb.append(generateAnnotationsXml(meth));
            for (Class<?> ex : meth.getExceptionTypes()) {
                if (DBusExecutionException.class.isAssignableFrom(ex)) {
                    sb.append("   <annotation name=\"org.freedesktop.DBus.Method.Error\" value=\"")
                            .append(AbstractConnection.DOLLAR_PATTERN.matcher(ex.getName()).replaceAll("."))
                            .append("\" />\n");
                }
            }
            StringBuilder ms = new StringBuilder();
            for (Type pt : meth.getGenericParameterTypes()) {
                for (String s : Marshalling.getDBusType(pt)) {
                    sb.append("   <arg type=\"").append(s).append("\" direction=\"in\"/>\n");
                    ms.append(s);
                }
            }
            if (!Void.TYPE.equals(meth.getGenericReturnType())) {
                if (Container.class.isAssignableFrom(meth.getReturnType())) {
                    createReturnArguments(meth, sb);
                } else if (Object[].class.equals(meth.getGenericReturnType())) {
                    throw new DBusException("Return type of Object[] cannot be introspected properly");
                } else {
                    for (String s : Marshalling.getDBusType(meth.getGenericReturnType())) {
                        sb.append("   <arg type=\"").append(s).append("\" direction=\"out\"/>\n");
                    }
                }
            }
            sb.append("  </method>\n");
            methods.putIfAbsent(new MethodTuple(methodName, ms.toString()), meth);
        }

        return sb.toString();
    }

    /**
     * Creates the XML parameters for return types of {@link Container} based classes.
     * This method supports both, parameterized and regular return types.
     *
     * @param _meth method which return value is investigated
     * @param _sb StringBuilder to append arguments to
     *
     * @throws DBusException when converting signature fails
     */
    private void createReturnArguments(Method _meth, StringBuilder _sb) throws DBusException {
        // ignore non-existing and void methods
        if (_meth == null || _sb == null || Void.TYPE.equals(_meth.getGenericReturnType())) {
            return;
        }

        List<Type> argTypes = new ArrayList<>();
        if (_meth.getGenericReturnType() instanceof ParameterizedType pt) {
            argTypes = Arrays.asList(pt.getActualTypeArguments());

            for (Type t : pt.getActualTypeArguments()) {
                if (t != null) {
                    for (String s : Marshalling.getDBusType(t)) {
                        _sb.append("   <arg type=\"").append(s).append("\" direction=\"out\"/>\n");
                    }
                }
            }
        } else if (_meth.getGenericReturnType() instanceof Class<?> clz) {
            argTypes = Arrays.stream(clz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Position.class))
                .filter(Objects::nonNull)
                .map(f -> Map.entry(f.getAnnotation(Position.class).value(), f))
                .sorted(Comparator.comparingInt(k -> k.getKey()))
                .map(f -> f.getValue())
                .map(Field::getType)
                .map(Type.class::cast)
                .toList();
        }

        for (Type f : argTypes) {
            for (String s : Marshalling.getDBusType(f)) {
                _sb.append("   <arg type=\"").append(s).append("\" direction=\"out\"/>\n");
            }
        }

    }

    /**
     * Generates the introspection data for the input interface signals
     *
     * @param _clz input interface
     * @return xml with signal definitions
     * @throws DBusException in case of invalid signal name / data types
     */
    protected String generateSignalsXml(Class<?> _clz) throws DBusException {
        StringBuilder sb = new StringBuilder();
        for (Class<?> sig : _clz.getDeclaredClasses()) {
            if (DBusSignal.class.isAssignableFrom(sig)) {
                String signalName = DBusNamingUtil.getSignalName(sig);
                if (sig.isAnnotationPresent(DBusMemberName.class)) {
                    DBusSignal.addSignalMap(sig.getSimpleName(), signalName);
                }
                if (signalName.length() > AbstractConnection.MAX_NAME_LENGTH) {
                    throw new DBusException(
                            "Introspected signal name exceeds 255 characters. Cannot export objects with signals of type "
                                    + signalName);
                }
                sb.append("  <signal name=\"").append(signalName).append("\">\n");
                Constructor<?> con = sig.getConstructors()[0];
                Type[] ts = con.getGenericParameterTypes();
                for (int j = 1; j < ts.length; j++) {
                    for (String s : Marshalling.getDBusType(ts[j])) {
                        sb.append("   <arg type=\"").append(s).append("\" direction=\"out\" />\n");
                    }
                }
                sb.append(generateAnnotationsXml(sig));
                sb.append("  </signal>\n");
            }
        }
        return sb.toString();
    }

    /**
     * Get all valid DBus interfaces which are implemented in a given class.
     * The search is performed without recursion taking into account object inheritance.
     * A valid DBus interface must directly extend the {@link DBusInterface}.
     *
     * @param _inputClazz input object class
     * @return set of DBus interfaces implements in the input class
     */
    protected Set<Class<?>> getDBusInterfaces(Class<?> _inputClazz) {
        Objects.requireNonNull(_inputClazz, "inputClazz must not be null");
        Set<Class<?>> result = new LinkedHashSet<>();

        // set of already checked classes/interfaces - used to avoid loops/redundant reflection calls
        Set<Class<?>> checked = new LinkedHashSet<>();
        // queue with classes/interfaces to check
        Queue<Class<?>> toCheck = new LinkedList<>();
        toCheck.add(_inputClazz);

        while (!toCheck.isEmpty()) {
            Class<?> clazz = toCheck.poll();
            checked.add(clazz); // avoid checking this class in the next loops

            // if it's class and it has super class, queue to check it later
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && DBusInterface.class.isAssignableFrom(superClass)) {
                toCheck.add(superClass);
            }

            List<Class<?>> interfaces = Arrays.asList(clazz.getInterfaces());
            if (interfaces.contains(DBusInterface.class)) {
                // clazz is interface and directly extends the DBusInterface
                result.add(clazz);
            }

            // if clazz is using @DBusBoundProperty, always expose the Properties interface
            Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(DBusBoundProperty.class))
                .findAny()
                .ifPresent(x -> toCheck.add(Properties.class));

            // iterate over the sub-interfaces and select the ones that extend DBusInterface
            // this is required especially for nested interfaces
            interfaces.stream()
                    .filter(DBusInterface.class::isAssignableFrom)
                    .filter(i -> i != DBusInterface.class)
                    .filter(i -> !checked.contains(i))
                    .forEach(toCheck::add);
        }

        return result;
    }

    private String generateIntrospectionXml(Set<Class<?>> _interfaces) throws DBusException {
        StringBuilder sb = new StringBuilder();
        for (Class<?> iface : _interfaces) {
            String ifaceName = DBusNamingUtil.getInterfaceName(iface);
            // don't let people export things which don't have a valid D-Bus interface name
            if (ifaceName.equals(iface.getSimpleName())) {
                throw new DBusException("DBusInterfaces cannot be declared outside a package");
            }
            if (ifaceName.length() > AbstractConnection.MAX_NAME_LENGTH) {
                throw new DBusException(
                        "Introspected interface name exceeds 255 characters. Cannot export objects of type "
                                + ifaceName);
            }

            // add mapping between class FQCN and name used in annotation (if present)
            if (iface.isAnnotationPresent(DBusInterfaceName.class)) {
                DBusSignal.addInterfaceMap(iface.getName(), ifaceName);
            }

            sb.append(" <interface name=\"").append(ifaceName).append("\">\n");
            sb.append(generateAnnotationsXml(iface));
            sb.append(generateMethodsXml(iface));
            sb.append(generatePropertiesXml(iface));
            sb.append(generateSignalsXml(iface));
            sb.append(" </interface>\n");
        }

        return sb.toString();
    }

    public Map<MethodTuple, Method> getMethods() {
        return methods;
    }

    public Map<PropertyRef, Method> getPropertyMethods() {
        return propertyMethods;
    }

    public Reference<DBusInterface> getObject() {
        return object;
    }

    public String getIntrospectiondata() {
        return introspectionData;
    }

    public Set<Class<?>> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + " [methodCount=" + methods.size()
            + ", propertyMethodCount=" + propertyMethods.size()
            + ", object=" + (object.get() != null ? Objects.toString(object) : "<no object referenced>") + "]";
    }

    public static boolean isExcluded(Method _meth) {
        return _meth == null
                || !Modifier.isPublic(_meth.getModifiers())
                || _meth.isSynthetic() // method created by compiler
                || _meth.isDefault() // method with default implementation (in interfaces), won't work anyway
                || _meth.isBridge() // bridge method created by compiler
                || _meth.getAnnotation(DBusIgnore.class) != null
                || _meth.getAnnotation(DBusBoundProperty.class) != null
                || _meth.getName().equals("getObjectPath") && _meth.getReturnType().equals(String.class)
                        && _meth.getParameterCount() == 0;
    }

}
