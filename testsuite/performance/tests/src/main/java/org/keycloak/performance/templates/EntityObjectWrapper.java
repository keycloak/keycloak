package org.keycloak.performance.templates;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.collections.map.LRUMap;
import org.keycloak.performance.dataset.Entity;
import org.keycloak.performance.dataset.NestedEntity;
import org.keycloak.performance.util.Loggable;

/**
 *
 * @author tkyjovsk
 */
public class EntityObjectWrapper extends DefaultObjectWrapper implements Loggable {

    public static final int TEMPLATE_CACHE_SIZE = Integer.parseInt(System.getProperty("template.cache.size", "10000"));
    public static final EntityObjectWrapper INSTANCE = new EntityObjectWrapper();

    private final Map<Object, TemplateModel> modelCache = Collections.synchronizedMap(new LRUMap(TEMPLATE_CACHE_SIZE));

    @Override
    protected TemplateModel handleUnknownType(Object obj) throws TemplateModelException {
        if (obj instanceof NestedEntity) {
            return modelCache.computeIfAbsent(obj, t -> new NestedEntityTemplateModel((NestedEntity) obj, modelCache));
        }
        if (obj instanceof Entity) {
            return modelCache.computeIfAbsent(obj, t -> new EntityTemplateModel((Entity) obj));
        }
        return super.handleUnknownType(obj);
    }

}
