package org.freedesktop.dbus.connections.base;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.MethodTuple;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.impl.ConnectionConfig;
import org.freedesktop.dbus.errors.InvalidMethodArgument;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.ExportedObject;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.constants.Flags;
import org.freedesktop.dbus.propertyref.PropRefRemoteHandler;
import org.freedesktop.dbus.propertyref.PropertyRef;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.utils.Util;

/**
 * Abstract class containing methods for handling DBus properties and {@link DBusBoundProperty} annotation. <br>
 * Part of the {@link AbstractConnectionBase} &rarr;  {@link ConnectionMethodInvocation}
 * &rarr; {@link DBusBoundPropertyHandler} &rarr; {@link ConnectionMessageHandler} &rarr; {@link AbstractConnection} hierarchy.
 *
 * @author hypfvieh
 * @since 5.1.0 - 2024-03-18
 */
public abstract class DBusBoundPropertyHandler extends ConnectionMethodInvocation {

    private static final Method PROP_GETALL_METHOD = PropRefRemoteHandler.getPropertiesMethod("GetAll", String.class);

    protected DBusBoundPropertyHandler(ConnectionConfig _conCfg, TransportConfig _transportConfig, ReceivingServiceConfig _rsCfg) throws DBusException {
        super(_conCfg, _transportConfig, _rsCfg);
    }

    /**
     * Method which handles the magic related to {@link DBusBoundProperty} annotation.<br>
     * It takes care of proper method calling (calling Get/Set stuff on DBus Properties interface)<br>
     * and will also take care of converting wrapped Variant types.
     *
     * @param _exportObject exported object
     * @param _methodCall method to call
     * @param _params parameter to pass to method
     *
     * @return Any of:<br>
     *
     * {@link PropHandled#HANDLED} when property was defined by annotation and was handled by this method<br>
     * {@link PropHandled#NOT_HANDLED} when object implements DBus Properties but the requested property was not defined by annotation<br>
     * {@link PropHandled#NO_PROPERTY} when property is not defined by annotation and object does not implement DBus Properties<br>
     *
     * @throws DBusException when something fails
     */
    protected PropHandled handleDBusBoundProperties(ExportedObject _exportObject, final MethodCall _methodCall, Object[] _params) throws DBusException {
        if (_params.length == 2 && _params[0] instanceof String
            && _params[1] instanceof String
            && _methodCall.getName().equals("Get")) {
            // 'Get'
            return handleGet(_exportObject, _methodCall, _params);

        } else if (_params.length == 3
            && _params[0] instanceof String
            && _params[1] instanceof String
            && _methodCall.getName().equals("Set")) {
            // 'Set'
            return handleSet(_exportObject, _methodCall, _params);

        } else if (_params.length == 1 && _params[0] instanceof String
            && _methodCall.getName().equals("GetAll")) {
            // 'GetAll'
            return handleGetAll(_exportObject, _methodCall);
        }
        return PropHandled.NOT_HANDLED;
    }

    /**
     * Called when 'GetAll' method of DBus {@link Properties} interface is called.
     *
     * @param _exportObject exported object
     * @param _methodCall method call
     *
     * @return {@link PropHandled#HANDLED} when call was handled {@link PropHandled#NOT_HANDLED} otherwise
     *
     * @throws DBusException when handling fails
     */
    @SuppressWarnings("unchecked")
    protected PropHandled handleGetAll(ExportedObject _exportObject, final MethodCall _methodCall) throws DBusException {
        Set<Entry<PropertyRef, Method>> allPropertyMethods = _exportObject.getPropertyMethods().entrySet();
        /* If there are no property methods on this object, just process as normal */
        if (!allPropertyMethods.isEmpty()) {
            Object object = _exportObject.getObject().get();
            Method meth = null;
            if (object instanceof Properties) {
                meth = _exportObject.getMethods().get(new MethodTuple(_methodCall.getName(), _methodCall.getSig()));
                if (null == meth) {
                    sendMessage(getMessageFactory().createError(_methodCall, new UnknownMethod(String.format(
                        "The method `%s.%s' does not exist on this object.", _methodCall.getInterface(), _methodCall.getName()))));
                    return PropHandled.HANDLED;
                }
            } else {
                meth = PROP_GETALL_METHOD;
            }

            Method originalMeth = meth;

            getReceivingService().execMethodCallHandler(() -> {
                Map<String, Object> resultMap = new HashMap<>();
                for (Entry<PropertyRef, Method> propEn : allPropertyMethods) {
                    Method propMeth = propEn.getValue();
                    if (propEn.getKey().getAccess() == Access.READ) {
                        try {
                            _methodCall.setArgs(new Object[0]);
                            Object val = invokeMethod(_methodCall, propMeth, object);

                            // when the value is a collection, array or map, wrap them in a proper variant type
                            if (val != null && val.getClass().isArray() || Collection.class.isInstance(val) || Map.class.isInstance(val)) {
                                String[] dataType = Marshalling.getDBusType(propEn.getValue().getGenericReturnType());
                                String dataTypeStr = Arrays.stream(dataType).collect(Collectors.joining());
                                getLogger().trace("Creating embedded Array/Collection/Map of type {}", dataTypeStr);
                                val = new Variant<>(val, dataTypeStr);
                            }

                            resultMap.put(propEn.getKey().getName(), val);
                        } catch (Throwable _ex) {
                            getLogger().debug("Error executing method {} on method call {}", propMeth, _methodCall, _ex);
                            handleException(_methodCall, new UnknownMethod("Failure in de-serializing message: " + _ex));
                        }
                    }
                }

                // this object implements Properties, so we have to query for these properties as well as
                // collecting the properties only available by annotations
                if (object instanceof Properties) {
                    _methodCall.setArgs(new Object[] {_methodCall.getInterface()});
                    resultMap.putAll((Map<String, ? extends Variant<?>>) setupAndInvoke(_methodCall, originalMeth, object, true));
                }

                try {
                    invokedMethodReply(_methodCall, originalMeth, resultMap);
                } catch (DBusExecutionException _ex) {
                    getLogger().debug("Error invoking method call", _ex);
                    handleException(_methodCall, _ex);
                } catch (Throwable _ex) {
                    getLogger().debug("Failed to invoke method call", _ex);
                    handleException(_methodCall,
                        new DBusExecutionException(String.format("Error Executing Method %s.%s: %s",
                        _methodCall.getInterface(), _methodCall.getName(), _ex.getMessage()), _ex));
                }
            });
            return PropHandled.HANDLED;
        }
        return PropHandled.NOT_HANDLED;
    }

    /**
     * Called when 'Get' method of DBus {@link Properties} interface is called.
     *
     * @param _exportObject exported object
     * @param _methodCall method call
     * @param _params parameters for method call
     *
     * @return Any of:<br>
     *
     * {@link PropHandled#HANDLED} when property was defined by annotation and was handled by this method<br>
     * {@link PropHandled#NOT_HANDLED} when object implements DBus Properties but the requested property was not defined by annotation<br>
     * {@link PropHandled#NO_PROPERTY} when property is not defined by annotation and object does not implement DBus Properties<br>
     */
    protected PropHandled handleGet(ExportedObject _exportObject, final MethodCall _methodCall, Object[] _params) {
        PropertyRef propertyRef = new PropertyRef((String) _params[1], null, DBusProperty.Access.READ);
        Method propMeth = _exportObject.getPropertyMethods().get(propertyRef);
        if (propMeth != null) {
            // This IS a property reference
            Object object = _exportObject.getObject().get();

            getReceivingService().execMethodCallHandler(() -> {
                _methodCall.setArgs(new Object[0]);
                invokeMethodAndReply(_methodCall, propMeth, object, 1 == (_methodCall.getFlags() & Flags.NO_REPLY_EXPECTED));
            });

            return PropHandled.HANDLED;
        } else if (_exportObject.getImplementedInterfaces().contains(Properties.class)) {
            return PropHandled.NOT_HANDLED;
        } else {
            return PropHandled.NO_PROPERTY;
        }
    }

    /**
     * Called when 'Set' method of DBus {@link Properties} interface is called.
     *
     * @param _exportObject exported object
     * @param _methodCall method call
     * @param _params method call parameters
     *
     * @return {@link PropHandled#HANDLED} when property was definied by annotation, {@link PropHandled#NOT_HANDLED} otherwise
     */
    protected PropHandled handleSet(ExportedObject _exportObject, final MethodCall _methodCall, Object[] _params) {

        PropertyRef propertyRef = new PropertyRef((String) _params[1], null, Access.WRITE);
        Method propMeth = _exportObject.getPropertyMethods().get(propertyRef);
        if (propMeth != null) {
            // This IS a property reference
            Object object = _exportObject.getObject().get();
            Class<?> type = PropertyRef.typeForMethod(propMeth);
            AtomicBoolean isVariant = new AtomicBoolean(false);

            Object val = Optional.ofNullable(_params[2])
                .map(v -> {
                    if (v instanceof Variant<?> va) {
                        isVariant.set(true);
                        return va.getValue();
                    }
                    return v;
                }).orElse(null);

            getReceivingService().execMethodCallHandler(() -> {
                try {
                    Object myVal = val;
                    Parameter[] parameters = propMeth.getParameters();
                    // the setter method can only be used if it has just 1 parameter
                    if (parameters.length != 1) {
                        throw new InvalidMethodArgument("Expected method with one argument, but found " + parameters.length);
                    }
                    // take care of arrays:
                    // DBus only knows arrays of types, not lists or other collections.
                    // if the method which should be called wants a Collection we have to
                    // convert the array to a proper type
                    if (Collection.class.isAssignableFrom(parameters[0].getType())
                        && isVariant.get() && myVal != null && myVal.getClass().isArray()) {

                        if (Set.class.isAssignableFrom(parameters[0].getType())) {
                            myVal = new LinkedHashSet<>(Arrays.asList(Util.toObjectArray(myVal)));
                        } else { // assume list is fine for all other collection types
                            myVal = new ArrayList<>(Arrays.asList(Util.toObjectArray(myVal)));
                        }
                    }
                    _methodCall.setArgs(Marshalling.deSerializeParameters(new Object[] {myVal}, new Type[] {type}, this));
                    invokeMethodAndReply(_methodCall, propMeth, object, 1 == (_methodCall.getFlags() & Flags.NO_REPLY_EXPECTED));
                } catch (Exception _ex) {
                    getLogger().debug("Failed to invoke method call on Properties", _ex);
                    handleException(_methodCall, new UnknownMethod("Failure in de-serializing message: " + _ex));
                }
            });
            return PropHandled.HANDLED;
        }
        return PropHandled.NOT_HANDLED;

    }

    enum PropHandled {
        /** Property request was handled. */
        HANDLED,
        /** Property request was not handled. */
        NOT_HANDLED,
        /** Property was not handled and Properties interface was not defined on exported object. */
        NO_PROPERTY
    }
}
