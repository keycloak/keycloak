package org.keycloak.services.models.nosql.impl.types;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Helper class for caching of classNames to actual classes (Should help a bit to avoid expensive reflection calls)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClassCache {

    public static final String SPLIT =  "###";
    private static final ClassCache INSTANCE = new ClassCache();

    private ConcurrentMap<String, Class<?>> cache = new ConcurrentHashMap<String, Class<?>>();

    private ClassCache() {};

    public static ClassCache getInstance() {
        return INSTANCE;
    }

    public Class<?> getOrLoadClass(String className) {
        Class<?> clazz = cache.get(className);
        if (clazz == null) {
            try {
                clazz = Class.forName(className);
                cache.putIfAbsent(className, clazz);
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException(cnfe);
            }
        }
        return clazz;
    }

}
