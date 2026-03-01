package org.keycloak.representations.workflows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.reflections.Reflections;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.keycloak.common.util.reflections.Reflections.isArrayType;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_WITH;

public abstract class AbstractWorkflowComponentRepresentation {

    private String id;

    @JsonProperty(CONFIG_WITH)
    private MultivaluedHashMap<String, String> config;

    public AbstractWorkflowComponentRepresentation(String id, MultivaluedHashMap<String, String> config) {
        this.id = id;
        this.setConfig(config);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MultivaluedHashMap<String, String> getConfig() {
        return config;
    }

    public void setConfig(MultivaluedHashMap<String, String> config) {
        if (config != null) {
            if (this.config == null) {
                this.config = new MultivaluedHashMap<>();
            }
            this.config.putAll(config);
        }
    }

    public void setConfig(String key, String value) {
        setConfig(key, Collections.singletonList(value));
    }

    @JsonAnySetter
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public void setConfig(String key, List<String> values) {
        if (this.config == null) {
            this.config = new MultivaluedHashMap<>();
        }
        this.config.put(key, values);
    }

    protected <T> T getConfigValue(String key, Class<T> type) {
        if (config == null) {
            return null;
        }

        return Reflections.convertValueToType(config.getFirst(key), type);
    }

    protected List<String> getConfigValues(String key) {
        if (config == null) {
            return null;
        }

        try {
            return config.get(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void setConfigValue(String key, Object... values) {
        if (values == null || values.length == 0) {
            return;
        }

        if (this.config == null) {
            this.config = new MultivaluedHashMap<>();
        }

        if (isArrayType(values.getClass())) {
            this.config.put(key, Arrays.stream(values).filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList()));
        } else {
            this.config.putSingle(key, values[0].toString());
        }
    }

    protected void setConfigValue(String key, List<String> values) {
        if (values == null) {
            return;
        }
        if (this.config == null) {
            this.config = new MultivaluedHashMap<>();
        }
        this.config.put(key, values);
    }

    protected void addConfigValue(String key, String value) {
        if (value == null) {
            return;
        }
        if (this.config == null) {
            this.config = new MultivaluedHashMap<>();
        }

        this.config.add(key, value);
    }
}
