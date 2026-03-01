package org.freedesktop.dbus.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.freedesktop.dbus.types.Variant;

/**
 * Appends information about properties in the interface. The annotated properties are added to the introspection data.
 * In case of complex type of the property please use {@link org.freedesktop.dbus.TypeRef}.
 * <p>
 * Usage:
 * </p>
 * <pre>
 * {@literal @}DBusInterfaceName("com.example.Bar")
 * {@literal @}DBusProperty(name = "Name", type = String.class)
 * {@literal @}DBusProperty(name = "ListOfVariables", type = List.class, access = Access.READ)
 * {@literal @}DBusProperty(name = "MapOfStringList", type = ComplexTypeWithMapAndList.class, access = Access.READ)
 * public interface Bar extends DBusInterface {
 *
 *   // TypeRef allows to provide detailed information about type
 *   interface ComplexTypeWithMapAndList extends TypeRef&lt;Map&lt;String, List&lt;String&gt;&gt;&gt; {
 *   }
 * }
 * </pre>
 *
 * @see org.freedesktop.dbus.interfaces.DBusInterface
 * @see org.freedesktop.dbus.TypeRef
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DBusProperties.class)
public @interface DBusProperty {

    /**
     * Property name
     *
     * @return name
     */
    String name();

    /**
     * type of the property, in case of complex types please create custom interface that extends {@link org.freedesktop.dbus.TypeRef}
     *
     * @return type
     */
    Class<?> type() default Variant.class;

    /**
     * Property access type
     *
     * @return access
     */
    Access access() default Access.READ_WRITE;

    enum Access {
        READ("read"),
        READ_WRITE("readwrite"),
        WRITE("write");

        private final String accessName;

        Access(String _accessName) {
            this.accessName = _accessName;
        }

        public String getAccessName() {
            return accessName;
        }
    }
}
