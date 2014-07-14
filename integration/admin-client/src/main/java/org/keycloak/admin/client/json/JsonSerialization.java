package org.keycloak.admin.client.json;

import org.codehaus.jackson.map.type.CollectionType;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class JsonSerialization extends org.keycloak.util.JsonSerialization {

    @SuppressWarnings("RedundantCast")
    public static <T> T readValue(InputStream bytes, CollectionType collectionType) throws IOException {
        return (T) mapper.readValue(bytes, collectionType);
    }

    @SuppressWarnings("RedundantCast")
    public static <T> T readValue(InputStream bytes, TypeReference typeReference) throws IOException {
        return (T) mapper.readValue(bytes, typeReference);
    }

}
