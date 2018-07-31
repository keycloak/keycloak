package org.keycloak.performance.dataset;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.commons.lang.Validate;
import org.keycloak.performance.util.Loggable;
import static org.keycloak.util.JsonSerialization.writeValueAsString;

/**
 *
 * @author tkyjovsk
 * @param <REP> representation
 */
public interface Representable<REP> extends Loggable {

    public REP newRepresentation();

    public REP getRepresentation();

    public void setRepresentation(REP representation);

    public default void setId(String uuid) {
        if (uuid == null) {
            logger().debug(this.getClass().getSimpleName() + " " + this + " " + " setId " + uuid);
            throw new IllegalArgumentException();
        }
        try {
            Class<REP> c = (Class<REP>) getRepresentation().getClass();
            Method setId = c.getMethod("setId", String.class);
            setId.invoke(getRepresentation(), uuid);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    public default String getIdFromRepresentation(REP representation) {
        Validate.notNull(representation);
        try {
            Class<REP> c = (Class<REP>) representation.getClass();
            Method getId = c.getMethod("getId");
            Validate.notNull(getId);
            return (String) getId.invoke(representation);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    public default String getId() {
        return getIdFromRepresentation(getRepresentation());
    }

    public default String toJSON() throws IOException {
        return writeValueAsString(getRepresentation());
    }

}
