package org.keycloak.admin.client.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.CollectionType;

import java.util.List;
import java.util.Set;

public final class JsonUtils {

    private JsonUtils() {}

    public static CollectionType getTypedList(Class<?> clazz){
        return new ObjectMapper().getTypeFactory().constructCollectionType(List.class, clazz);
    }

    public static CollectionType getTypedSet(Class<?> clazz){
        return new ObjectMapper().getTypeFactory().constructCollectionType(Set.class, clazz);
    }

}
