package org.keycloak.performance.templates;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Map;
import org.keycloak.performance.dataset.Entity;
import org.keycloak.performance.dataset.NestedEntity;
import static org.keycloak.performance.util.StringUtil.firstLetterToLowerCase;

/**
 *
 * @author tkyjovsk
 */
public class NestedEntityTemplateModel extends EntityTemplateModel {

    private TemplateModel parentEntityModel;
    private String parentKey = null;

    public NestedEntityTemplateModel(NestedEntity entity, Map<Object, TemplateModel> modelCache) {
        super(entity);
        try {
            Entity parent = ((NestedEntity) entity).getParentEntity();
            this.parentEntityModel = (modelCache == null || !modelCache.containsKey(parent))
                    ? EntityObjectWrapper.INSTANCE.wrap(parent)
                    : modelCache.get(parent);
            this.parentKey = firstLetterToLowerCase(parent.simpleClassName());
        } catch (TemplateModelException ex) {
            throw new RuntimeException(ex);
        }
    }

    public NestedEntityTemplateModel(NestedEntity entity) {
        this(entity, null);
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        TemplateModel m = super.get(key);
        if (key.equals(parentKey)) {
            m = parentEntityModel;
        }
        return m;
    }

}
