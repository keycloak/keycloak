package org.keycloak.models.jpa.entities;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AttributeMap {
    Map<String, String>  attributes = new HashMap<String, String>();

    public void set(String key, String value) {
        attributes.put(key, value);
    }

    public void set(String key, Boolean value) {
        attributes.put(key, value.toString());
    }

    public void set(String key, Integer value) {
        attributes.put(key, value.toString());
    }

    public String get(String key) {
        return attributes.get(key);
    }

    public String get(String key, String defaultValue) {
        String value = attributes.get(key);
        return value == null ? defaultValue : value;
    }

    public String[] getArray(String key) {
        String value = get(key);
        if (value != null) {
            String[] a = value.split(",");
            for (int i = 0; i < a.length; i++) {
                a[i] = a[i].trim();
            }
            return a;
        } else {
            return null;
        }
    }

    public Integer getInt(String key) {
        return getInt(key, null);
    }

    public Integer getInt(String key, Integer defaultValue) {
        String v = get(key, null);
        return v != null ? Integer.parseInt(v) : defaultValue;
    }

    public Long getLong(String key) {
        return getLong(key, null);
    }

    public Long getLong(String key, Long defaultValue) {
        String v = get(key, null);
        return v != null ? Long.parseLong(v) : defaultValue;
    }

    public Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        String v = get(key, null);
        return v != null ? Boolean.parseBoolean(v) : defaultValue;
    }}
