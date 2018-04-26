package org.keycloak.performance.templates;

import freemarker.template.AdapterTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import org.apache.commons.lang.Validate;
import org.keycloak.performance.dataset.Entity;
import org.keycloak.performance.util.Loggable;

/**
 * Merges template models of entity object and representation into a single
 * model.
 *
 * @author tkyjovsk
 */
public class EntityTemplateModel implements TemplateHashModel, AdapterTemplateModel, Loggable {

    private static final DefaultObjectWrapperBuilder DOWB = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_26);
    private static final ObjectWrapper DEFAULT_OBJECT_WRAPPER = DOWB.build();

    private Entity entity;
    private TemplateHashModel entityModel;
    private TemplateHashModel representationModel;

    public EntityTemplateModel(Entity entity) {
//        logger().debug("model for: " + entity.simpleClassName() + ", r: " + entity.getRepresentation().getClass().getSimpleName());
        try {
            Validate.notNull(entity);
            this.entity = entity;
            this.entityModel = (TemplateHashModel) DEFAULT_OBJECT_WRAPPER.wrap(entity);
            this.representationModel = (TemplateHashModel) DEFAULT_OBJECT_WRAPPER.wrap(entity.getRepresentation());
        } catch (TemplateModelException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        TemplateModel m = null;
        if (m == null) {
            m = representationModel.get(key);
        }
        if (m == null) {
            m = entityModel.get(key);
        }
        if (m == null) {
            logger().error("key " + key + " not found for entity: " + entity);
        }
        return m;
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return entityModel.isEmpty() && representationModel.isEmpty();
    }

    @Override
    public Entity getAdaptedObject(Class<?> hint) {
        return entity;
    }

    public static String modelExToString(TemplateHashModelEx thme) {
        StringBuilder sb = new StringBuilder();
        TemplateModelIterator i;
        try {
            i = thme.keys().iterator();
            while (i.hasNext()) {
                TemplateModel k = i.next();
                TemplateModel v = thme.get(k.toString());
                sb.append(" - ").append(k).append("=").append(v).append('\n');
            }
        } catch (TemplateModelException ex) {
            throw new RuntimeException(ex);
        }
        return sb.toString();
    }

}
