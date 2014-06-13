package org.keycloak.exportimport;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.models.utils.reflection.Property;
import org.keycloak.models.utils.reflection.PropertyCriteria;
import org.keycloak.models.utils.reflection.PropertyQueries;

/**
 * Not thread safe (objectProperties). Assumption is that export/import is executed once per JVM
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportImportPropertiesManager {

    private static final Logger logger = Logger.getLogger(ExportImportPropertiesManager.class);

    private Map<Class<?>, Map<String, Property<Object>>> objectProperties = new HashMap<Class<?>, Map<String, Property<Object>>>();

    // Add properties of class to objectProperties
    protected void initClassProperties(Class<?> clazz) {
        Map<String, Property<Object>> classProps = PropertyQueries.createQuery(clazz).addCriteria(new NonEmptyGetterCriteria()).getWritableResultList();
        this.objectProperties.put(clazz, classProps);
    }

    public void setBasicPropertiesFromModel(Object model, Object entity) {
        Class<?> modelClass = getModelClass(model);

        // Lazy init of properties
        checkPropertiesAvailable(modelClass, entity.getClass());

        Map<String, Property<Object>> modelProps = this.objectProperties.get(modelClass);
        Map<String, Property<Object>> entityProps = this.objectProperties.get(entity.getClass());

        Map<String, Property<Object>> entityPropsCopy = new HashMap<String, Property<Object>>(entityProps);

        logger.debugf("Properties of entity %s: %s", entity, entityProps.keySet());
        for (Property<Object> modelProperty : modelProps.values()) {
            Property<Object> entityProperty = entityPropsCopy.get(modelProperty.getName());

            entityPropsCopy.remove(modelProperty.getName());

            if (entityProperty != null) {
                Object propertyValue = modelProperty.getValue(model);

                // Workaround needed because model classes have many getters/setters with "Set", but for entity classes, there are usually "List"
                if (propertyValue instanceof Set) {
                    Set propValueAsSet = (Set)propertyValue;
                    entityProperty.setValue(entity, new ArrayList(propValueAsSet));
                }  else {
                    entityProperty.setValue(entity, propertyValue);
                }
                if (logger.isTraceEnabled()) {
                    logger.tracef("Property %s successfully set in JSON to entity %s", modelProperty.getName(), entity);
                }
            } else {
                logger.debugf("Property %s not known in JSON for entity %s", modelProperty.getName(), entity);
            }
        }

        logger.debugf("Entity properties for manual setup: %s", entityPropsCopy.keySet());
    }

    private void checkPropertiesAvailable(Class<?> modelClass, Class<?> entityClass) {
        if (!objectProperties.containsKey(modelClass)) {
            initClassProperties(modelClass);
        }
        if (!objectProperties.containsKey(entityClass)) {
            initClassProperties(entityClass);
        }
    }

    public void setBasicPropertiesToModel(Object model, Object entity) {
        Class<?> modelClass = getModelClass(model);

        // Lazy init of properties
        checkPropertiesAvailable(modelClass, entity.getClass());

        Map<String, Property<Object>> modelProps = this.objectProperties.get(modelClass);
        Map<String, Property<Object>> entityProps = this.objectProperties.get(entity.getClass());

        Map<String, Property<Object>> entityPropsCopy = new HashMap<String, Property<Object>>(entityProps);

        logger.debugf("Properties of exported entity %s: %s", entity, entityProps.keySet());

        for (Property<Object> modelProperty : modelProps.values()) {
            Property<Object> entityProperty = entityPropsCopy.get(modelProperty.getName());

            entityPropsCopy.remove(modelProperty.getName());

            if (entityProperty != null) {
                Object propertyValue = entityProperty.getValue(entity);

                // Workaround needed because model classes have many getters/setters with "Set", but for entity classes, there are usually "List"
                if (propertyValue instanceof List && Set.class.isAssignableFrom(modelProperty.getJavaClass())) {
                    List propValueAsList = (List)propertyValue;
                    modelProperty.setValue(model, new HashSet(propValueAsList));
                }  else {
                    modelProperty.setValue(model, propertyValue);
                }

                if (logger.isTraceEnabled()) {
                    logger.tracef("Property %s successfully set in model from entity %s", modelProperty.getName(), entity);
                }
            } else {
                logger.debugf("Property %s not known for entity %s", modelProperty.getName(), entity);
            }
        }

        logger.debugf("Entity properties for manual setup: %s", entityPropsCopy.keySet());
    }

    protected Class<?> getModelClass(Object model) {
        Class<?> modelClass = model.getClass();
        Class<?>[] interfaces = modelClass.getInterfaces();

        // Bit unsafe, but looks that it works for all "model adapters" so far
        if (interfaces.length == 0) {
            return modelClass;
        } else {
            return interfaces[0];
        }
    }

    public static class NonEmptyGetterCriteria implements PropertyCriteria {

        private static final List<String> IGNORED_METHODS = Arrays.asList("getPasswordPolicy", "getAuthenticationProviders", "getAuthenticationLink");

        @Override
        public boolean methodMatches(Method m) {
            // Ignore non-empty getters
            if (m.getParameterTypes().length > 0) {
                return false;
            }

            // Ignore some "known" getters (for example incompatible types between model and entity)
            if (IGNORED_METHODS.contains(m.getName())) {
                return false;
            }

            return true;
        }
    }
}
