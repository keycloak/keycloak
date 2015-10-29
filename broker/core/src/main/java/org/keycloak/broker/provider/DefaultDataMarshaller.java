package org.keycloak.broker.provider;

import java.io.IOException;

import org.keycloak.common.util.Base64Url;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultDataMarshaller implements IdentityProviderDataMarshaller {

    @Override
    public String serialize(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else {
            try {
                byte[] bytes = JsonSerialization.writeValueAsBytes(value);
                return Base64Url.encode(bytes);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    @Override
    public <T> T deserialize(String serialized, Class<T> clazz) {
        if (clazz.equals(String.class)) {
            return clazz.cast(serialized);
        } else {
            byte[] bytes = Base64Url.decode(serialized);
            try {
                return JsonSerialization.readValue(bytes, clazz);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }
}
