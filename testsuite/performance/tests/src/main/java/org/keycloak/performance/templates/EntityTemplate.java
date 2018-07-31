package org.keycloak.performance.templates;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import org.keycloak.performance.util.Loggable;
import org.keycloak.performance.dataset.Entity;
import static org.keycloak.performance.util.StringUtil.firstLetterToLowerCase;

/**
 *
 * @author tkyjovsk
 * @param <E> entity
 * @param <R> representation
 */
public abstract class EntityTemplate<E extends Entity<R>, R> implements Loggable {

    public static final freemarker.template.Configuration FREEMARKER_CONFIG;

    public static final TypeReference MAP_TYPE_REFERENCE = new TypeReference<Map<String, Object>>() {
    };
    public static final ObjectMapper OBJECT_MAPPER;

    static {
        FREEMARKER_CONFIG = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_26);
        FREEMARKER_CONFIG.setBooleanFormat("true,false");
        FREEMARKER_CONFIG.setNumberFormat("computer");
        FREEMARKER_CONFIG.setObjectWrapper(EntityObjectWrapper.INSTANCE);
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        OBJECT_MAPPER.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    private final Configuration configuration;
    private final Map<String, Template> attributeTemplates = new LinkedHashMap<>();
    String configPrefix;

    public EntityTemplate(Configuration configuration) {
        this.configuration = configuration;
        this.configPrefix = firstLetterToLowerCase(this.getClass().getSimpleName().replaceFirst("Template$", ""));
        registerAttributeTemplates();
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    private void registerAttributeTemplates() {
        Iterator<String> configKeys = getConfiguration().getKeys(configPrefix);
        while (configKeys.hasNext()) {
            String configKey = configKeys.next();
            String attributeName = configKey.replaceFirst(configPrefix + ".", "");
            String attributeTemplateDefinition = getConfiguration().getString(configKey);
            logger().trace("template: " + configPrefix + " -> " + attributeName + ": " + attributeTemplateDefinition);
            try {
                attributeTemplates.put(attributeName, new Template(configKey, attributeTemplateDefinition, FREEMARKER_CONFIG));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    protected void processAtributeTemplates(E entity) {
        Map<String, Object> updateMap = new HashMap<>();
        attributeTemplates.keySet().forEach((attributeName) -> {
            updateMap.clear();
            try (StringWriter stringWriter = new StringWriter()) {
                logger().trace("processing template for " + attributeName);
                attributeTemplates.get(attributeName).process(entity, stringWriter);
                updateMap.put(attributeName, stringWriter.toString());
                OBJECT_MAPPER.updateValue(entity.getRepresentation(), updateMap);
            } catch (IOException | TemplateException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public E processEntity(E entity) {
        processAttributes(entity);
        processMappings(entity);
        return entity;
    }

    public synchronized E produce() {
        return processEntity(newEntity());
    }

    public abstract E newEntity();

    public E processAttributes(E entity) {
        processAtributeTemplates(entity);
        return entity;
    }

    public abstract void processMappings(E entity);

    public abstract void validateConfiguration();

}
