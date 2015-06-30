package org.keycloak.connections.mongo.impl.types;

import java.util.HashSet;
import java.util.Set;

import com.mongodb.BasicDBList;
import org.keycloak.connections.mongo.api.types.Mapper;
import org.keycloak.connections.mongo.api.types.MapperContext;
import org.keycloak.connections.mongo.api.types.MapperRegistry;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BasicDBListToSetMapper implements Mapper<BasicDBList, Set> {

    private final MapperRegistry mapperRegistry;

    public BasicDBListToSetMapper(MapperRegistry mapperRegistry) {
        this.mapperRegistry = mapperRegistry;
    }

    @Override
    public Set convertObject(MapperContext<BasicDBList, Set> context) {
        BasicDBList dbList = context.getObjectToConvert();
        Set<Object> appObjects = new HashSet<Object>();
        Class<?> expectedListElementType = (Class<?>) context.getGenericTypes().get(0);

        for (Object dbObject : dbList) {
            MapperContext<Object, Object> newContext = new MapperContext<Object, Object>(dbObject, expectedListElementType, null);
            appObjects.add(mapperRegistry.convertDBObjectToApplicationObject(newContext));
        }
        return appObjects;
    }

    @Override
    public Class<? extends BasicDBList> getTypeOfObjectToConvert() {
        return BasicDBList.class;
    }

    @Override
    public Class<Set> getExpectedReturnType() {
        return Set.class;
    }
}
