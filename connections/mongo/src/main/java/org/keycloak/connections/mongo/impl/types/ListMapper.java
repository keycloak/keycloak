package org.keycloak.connections.mongo.impl.types;

import com.mongodb.BasicDBList;
import org.keycloak.connections.mongo.api.types.Mapper;
import org.keycloak.connections.mongo.api.types.MapperContext;
import org.keycloak.connections.mongo.api.types.MapperRegistry;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ListMapper<T extends List> implements Mapper<T, BasicDBList> {

    private final MapperRegistry mapperRegistry;
    private final Class<T> listType;

    public ListMapper(MapperRegistry mapperRegistry, Class<T> listType) {
        this.mapperRegistry = mapperRegistry;
        this.listType = listType;
    }

    @Override
    public BasicDBList convertObject(MapperContext<T, BasicDBList> context) {
        T appObjectsList = context.getObjectToConvert();

        BasicDBList dbObjects = new BasicDBList();
        for (Object appObject : appObjectsList) {
            Object dbObject = mapperRegistry.convertApplicationObjectToDBObject(appObject, Object.class);

            dbObjects.add(dbObject);
        }
        return dbObjects;
    }

    @Override
    public Class<? extends T> getTypeOfObjectToConvert() {
        return listType;
    }

    @Override
    public Class<BasicDBList> getExpectedReturnType() {
        return BasicDBList.class;
    }
}
