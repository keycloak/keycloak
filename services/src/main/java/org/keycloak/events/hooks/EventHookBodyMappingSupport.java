/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.events.hooks;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.util.JsonSerialization;

import freemarker.cache.StringTemplateLoader;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleCollection;
import freemarker.template.Template;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

final class EventHookBodyMappingSupport {

    static final String CUSTOM_BODY_MAPPING_TEMPLATE = "customBodyMappingTemplate";
    static final String PARSE_FAILED_STATUS_CODE = "PARSE_FAILED";

    private static final String TEMPLATE_NAME = "event-hook-body.ftl";

    private EventHookBodyMappingSupport() {
    }

    static boolean isEnabled(Map<String, Object> settings) {
        return optionalTemplate(settings) != null;
    }

    static void validateConfig(Map<String, Object> settings) {
        if (settings == null || !settings.containsKey(CUSTOM_BODY_MAPPING_TEMPLATE)) {
            return;
        }

        validateTemplate(requiredTemplate(settings), "custom body mapping template");
    }

    static void validateTemplate(String template, String description) {
        String normalizedTemplate = requireNonBlankTemplate(template, description);

        try {
            templateConfiguration(normalizedTemplate).getTemplate(TEMPLATE_NAME, "UTF-8");
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid " + description + ": " + exception.getMessage(), exception);
        }
    }

    static Object readPayload(String payload) throws EventHookBodyMappingException {
        try {
            return EventHookPayloadNormalizer.readPayload(payload);
        } catch (IOException exception) {
            throw new EventHookBodyMappingException("Failed to parse event hook payload", exception);
        }
    }

    static Map<String, Object> singleEventModel(Object eventPayload) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("event", eventPayload);
        model.put("events", List.of(eventPayload));
        model.put("payload", eventPayload);
        model.put("batch", false);
        model.put("eventCount", 1);
        if (eventPayload instanceof Map<?, ?> payloadMap) {
            payloadMap.forEach((key, value) -> model.put(String.valueOf(key), value));
        }
        return model;
    }

    static Map<String, Object> batchEventModel(List<Object> eventPayloads) {
        Map<String, Object> model = new LinkedHashMap<>();
        Object singleEvent = eventPayloads.size() == 1 ? eventPayloads.get(0) : null;
        model.put("event", singleEvent);
        model.put("events", List.copyOf(eventPayloads));
        model.put("payload", singleEvent);
        model.put("batch", eventPayloads.size() > 1);
        model.put("eventCount", eventPayloads.size());
        if (singleEvent instanceof Map<?, ?> payloadMap) {
            payloadMap.forEach((key, value) -> model.put(String.valueOf(key), value));
        }
        return model;
    }

    static Map<String, Object> withPullMetadata(Map<String, Object> model, Object entry, List<?> entries, boolean hasMoreEvents) {
        Map<String, Object> enriched = new LinkedHashMap<>(model);
        enriched.put("entry", entry);
        enriched.put("entries", entries == null ? List.of() : entries);
        enriched.put("hasMoreEvents", hasMoreEvents);
        return enriched;
    }

    static RenderedBody render(Map<String, Object> settings, Map<String, Object> model) throws EventHookBodyMappingException {
        String template = requiredTemplate(settings);

        try {
            String rendered = renderTemplate(template, model).trim();
            if (rendered.isEmpty()) {
                throw new EventHookBodyMappingException("Custom body mapping rendered an empty body");
            }

            Object jsonBody = JsonSerialization.readValue(rendered, Object.class);
            return new RenderedBody(rendered, jsonBody);
        } catch (EventHookBodyMappingException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new EventHookBodyMappingException("Custom body mapping must render valid JSON", exception);
        } catch (Exception exception) {
            throw new EventHookBodyMappingException("Failed to render custom body mapping: " + exception.getMessage(), exception);
        }
    }

    static String renderTemplate(String template, Map<String, Object> model) throws EventHookBodyMappingException {
        String normalizedTemplate = requireNonBlankTemplate(template, "template");

        try {
            Template compiledTemplate = templateConfiguration(normalizedTemplate).getTemplate(TEMPLATE_NAME, "UTF-8");
            StringWriter writer = new StringWriter();
            compiledTemplate.process(model, writer);
            return writer.toString();
        } catch (Exception exception) {
            throw new EventHookBodyMappingException("Failed to render template: " + exception.getMessage(), exception);
        }
    }

    private static Configuration templateConfiguration(String template) {
        StringTemplateLoader templateLoader = new StringTemplateLoader();
        templateLoader.putTemplate(TEMPLATE_NAME, template);

        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setTemplateLoader(templateLoader);
        configuration.setObjectWrapper(EventHookTemplateObjectWrapper.INSTANCE);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        configuration.setFallbackOnNullLoopVariable(false);
        configuration.setLocalizedLookup(false);
        return configuration;
    }

    private static String requiredTemplate(Map<String, Object> settings) {
        Object configured = settings == null ? null : settings.get(CUSTOM_BODY_MAPPING_TEMPLATE);
        if (configured == null) {
            throw new IllegalArgumentException("Missing required setting: " + CUSTOM_BODY_MAPPING_TEMPLATE);
        }

        return requireNonBlankTemplate(configured.toString(), CUSTOM_BODY_MAPPING_TEMPLATE);
    }

    private static String optionalTemplate(Map<String, Object> settings) {
        Object configured = settings == null ? null : settings.get(CUSTOM_BODY_MAPPING_TEMPLATE);
        if (configured == null) {
            return null;
        }

        String template = configured.toString().trim();
        return template.isEmpty() ? null : template;
    }

    private static String requireNonBlankTemplate(String template, String description) {
        String normalizedTemplate = template == null ? null : template.trim();
        if (normalizedTemplate == null || normalizedTemplate.isEmpty()) {
            throw new IllegalArgumentException("Missing required setting: " + description);
        }
        return normalizedTemplate;
    }

    static final class RenderedBody {

        private final String rawBody;
        private final Object jsonBody;

        RenderedBody(String rawBody, Object jsonBody) {
            this.rawBody = rawBody;
            this.jsonBody = jsonBody;
        }

        String rawBody() {
            return rawBody;
        }

        Object jsonBody() {
            return jsonBody;
        }
    }

    static final class EventHookBodyMappingException extends Exception {

        EventHookBodyMappingException(String message) {
            super(message);
        }

        EventHookBodyMappingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class EventHookTemplateObjectWrapper implements ObjectWrapper {

        private static final EventHookTemplateObjectWrapper INSTANCE = new EventHookTemplateObjectWrapper();

        @Override
        public TemplateModel wrap(Object object) throws TemplateModelException {
            if (object instanceof TemplateModel templateModel) {
                return templateModel;
            }

            if (object == null) {
                return null;
            }

            Object normalized = normalize(object);
            if (normalized instanceof Map<?, ?> mapValue) {
                return new JsonHashTemplateModel(mapValue);
            }
            if (normalized instanceof List<?> listValue) {
                return new JsonSequenceTemplateModel(listValue);
            }
            if (normalized instanceof Boolean booleanValue) {
                return new JsonBooleanTemplateModel(booleanValue);
            }
            if (normalized instanceof Number numberValue) {
                return new JsonNumberTemplateModel(numberValue);
            }
            return new JsonScalarTemplateModel(normalized);
        }

        private Object normalize(Object value) {
            if (value instanceof Map<?, ?> || value instanceof List<?> || value instanceof String || value instanceof Number || value instanceof Boolean) {
                return value;
            }

            if (value instanceof Enum<?> enumValue) {
                return enumValue.name();
            }

            if (value.getClass().isArray()) {
                int length = Array.getLength(value);
                List<Object> arrayValues = new ArrayList<>(length);
                for (int index = 0; index < length; index++) {
                    arrayValues.add(Array.get(value, index));
                }
                return arrayValues;
            }

            if (value instanceof Collection<?> collectionValue) {
                return List.copyOf(collectionValue);
            }

            Object converted = JsonSerialization.mapper.convertValue(value, Object.class);
            return converted == null ? value : converted;
        }
    }

    private abstract static class AbstractJsonTemplateModel implements TemplateScalarModel, AdapterTemplateModel, WrapperTemplateModel {

        private final Object value;

        private AbstractJsonTemplateModel(Object value) {
            this.value = value;
        }

        @Override
        public final Object getAdaptedObject(Class<?> hint) {
            return value;
        }

        @Override
        public final Object getWrappedObject() {
            return value;
        }

        @Override
        public String getAsString() throws TemplateModelException {
            if (value == null) {
                return null;
            }

            if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                return value.toString();
            }

            try {
                return JsonSerialization.writeValueAsString(value);
            } catch (IOException exception) {
                throw new TemplateModelException("Failed to serialize template value as JSON", exception);
            }
        }

        protected final TemplateModel wrapValue(Object nestedValue) throws TemplateModelException {
            return EventHookTemplateObjectWrapper.INSTANCE.wrap(nestedValue);
        }
    }

    private static final class JsonScalarTemplateModel extends AbstractJsonTemplateModel {

        private JsonScalarTemplateModel(Object value) {
            super(value);
        }
    }

    private static final class JsonBooleanTemplateModel extends AbstractJsonTemplateModel implements TemplateBooleanModel {

        private final Boolean value;

        private JsonBooleanTemplateModel(Boolean value) {
            super(value);
            this.value = value;
        }

        @Override
        public boolean getAsBoolean() {
            return value;
        }
    }

    private static final class JsonNumberTemplateModel extends AbstractJsonTemplateModel implements TemplateNumberModel {

        private final Number value;

        private JsonNumberTemplateModel(Number value) {
            super(value);
            this.value = value;
        }

        @Override
        public Number getAsNumber() {
            return value;
        }
    }

    private static final class JsonSequenceTemplateModel extends AbstractJsonTemplateModel implements TemplateSequenceModel, TemplateCollectionModel {

        private final List<?> values;

        private JsonSequenceTemplateModel(List<?> values) {
            super(values);
            this.values = values;
        }

        @Override
        public TemplateModel get(int index) throws TemplateModelException {
            if (index < 0 || index >= values.size()) {
                return null;
            }
            return wrapValue(values.get(index));
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public TemplateModelIterator iterator() {
            return new TemplateModelIterator() {
                private int index;

                @Override
                public TemplateModel next() throws TemplateModelException {
                    return get(index++);
                }

                @Override
                public boolean hasNext() {
                    return index < values.size();
                }
            };
        }
    }

    private static final class JsonHashTemplateModel extends AbstractJsonTemplateModel implements TemplateHashModelEx2 {

        private final Map<?, ?> values;

        private JsonHashTemplateModel(Map<?, ?> values) {
            super(values);
            this.values = values;
        }

        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            return wrapValue(values.get(key));
        }

        @Override
        public boolean isEmpty() {
            return values.isEmpty();
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public TemplateCollectionModel keys() {
            return new SimpleCollection(values.keySet(), EventHookTemplateObjectWrapper.INSTANCE);
        }

        @Override
        public TemplateCollectionModel values() {
            return new SimpleCollection(values.values(), EventHookTemplateObjectWrapper.INSTANCE);
        }

        @Override
        public KeyValuePairIterator keyValuePairIterator() {
            java.util.Iterator<? extends Map.Entry<?, ?>> iterator = values.entrySet().iterator();
            return new KeyValuePairIterator() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public KeyValuePair next() {
                    Map.Entry<?, ?> entry = iterator.next();
                    return new KeyValuePair() {
                        @Override
                        public TemplateModel getKey() throws TemplateModelException {
                            return wrapValue(String.valueOf(entry.getKey()));
                        }

                        @Override
                        public TemplateModel getValue() throws TemplateModelException {
                            return wrapValue(entry.getValue());
                        }
                    };
                }
            };
        }
    }
}
