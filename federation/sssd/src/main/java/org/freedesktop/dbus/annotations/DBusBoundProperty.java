package org.freedesktop.dbus.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.annotations.PropertiesEmitsChangedSignal.EmitChangeSignal;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * Binds a <strong>setter</strong> or <strong>getter</strong> method to a DBus property, in
 * a similar manner to the familiar JavaBeans pattern.
 * <p>
 * Using this annotation means you do not need to implement the {@link Properties}
 * interface and provide your own handling of {@link Properties#Get(String, String)},
 * {@link Properties#GetAll(String)} and {@link Properties#Set(String, String, Object)}.
 * <p>
 * Each DBus property should map to either one or two methods. If it has
 * {@link DBusBoundProperty#access()} of {@link Access#READ} then a single <strong>getter</strong>
 * method should be created with no parameters. The type of property will be determined by
 * the return type of the method, and the name of the property will be derived from the method name,
 * with either the <code>get</code> or the <code>is</code> stripped off.
 * <pre>
 * {@literal @}DBusBoundProperty
 * public String getMyStringProperty();
 *
 * {@literal @}DBusBoundProperty
 * public boolean isMyBooleanProperty();
 * </pre>
 * If it has {@link DBusBoundProperty#access()} of {@link Access#WRITE} then a single <strong>setter</strong>
 * method should be created with a single parameter and no return type. The type of the property
 * will be determined by that parameter, and the name of the property will be derived from the
 * method name, with either the <code>get</code> or the <code>is</code> stripped off.
 * <pre>
 * {@literal @}DBusBoundProperty
 * public void setMyStringProperty(String _property);
 * </pre>
 * If it has {@link DBusBoundProperty#access()} of {@link Access#READ_WRITE}, the both of
 * the above methods should be provided.
 * <p>
 * Any of the <code>name</code>, <code>type</code> and <code>access</code> attributes that would
 * normally be automatically determined, may be overridden using the corresponding annotation attributes.
 * <p>
 * It is allowed if you wish to mix use of {@link DBusProperty} and {@link DBusBoundProperty} as
 * long as individual properties are not repeated.
 *
 * @see org.freedesktop.dbus.interfaces.DBusInterface
 * @see org.freedesktop.dbus.annotations.DBusProperty
 * @see org.freedesktop.dbus.annotations.DBusProperties
 * @see org.freedesktop.dbus.TypeRef
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DBusBoundProperty {

    /**
     * Property name. If not supplied, the property name will be inferred from the method. See
     * class documentation for semantics.
     *
     * @return name
     */
    String name() default "";

    /**
     * Type of the property, in case of complex types please create custom interface that extends {@link org.freedesktop.dbus.TypeRef}.
     * If not supplied, then the type will be inferred from either the return value of a getter, or
     * the first parameter of a setter.
     *
     * @return type
     */
    Class<?> type() default Void.class;

    /**
     * Property access type. When {@link Access#READ_WRITE}, the access will be inferred from
     * the method name, whether it is a setter or a getter.
     *
     * @return access
     */
    Access access() default Access.READ_WRITE;

    /**
     * Annotation org.freedesktop.DBus.Property.EmitsChangedSignal.
     * <b>From <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#standard-interfaces">DBUS Specification</a>:</b><br>
     * If set to false, the org.freedesktop.DBus.Properties.PropertiesChanged signal,<br>
     * see the section called “org.freedesktop.DBus.Properties” is not guaranteed to be emitted if the property changes.<br>
     * <br>
     * If set to const the property never changes value during the lifetime of the object it belongs to, <br>
     * and hence the signal is never emitted for it. <br>
     * <br>
     * If set to invalidates the signal is emitted but the value is not included in the signal.<br>
     * <br>
     * If set to true the signal is emitted with the value included. <br>
     * The value for the annotation defaults to true if the enclosing interface element does not specify the annotation.
     * Otherwise it defaults to the value specified in the enclosing interface element.<br>
     * <br>
     * This annotation is intended to be used by code generators to implement client-side caching of property values. <br>
     * For all properties for which the annotation is set to const, invalidates or true the client may unconditionally <br>
     * cache the values as the properties don't change or notifications are generated for them if they do.
     *
     * @return emitChangeSignal
     */
    EmitChangeSignal emitChangeSignal() default EmitChangeSignal.TRUE;
}
