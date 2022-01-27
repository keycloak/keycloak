package org.keycloak.guides.maven;

import org.keycloak.quarkus.runtime.configuration.mappers.ConfigCategory;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Options {

    private final Map<String, Option> options;

    public Options() {
        options = PropertyMappers.getMappers().stream()
                .filter(m -> !m.isHidden())
                .map(m -> new Option(m.getFrom(), m.getCategory(), m.isBuildTime(), m.getDescription(), m.getDefaultValue(), m.getExpectedValues()))
                .sorted(Comparator.comparing(Option::getKey))
                .collect(Collectors.toMap(Option::getKey, o -> o, (o1, o2) -> o1, LinkedHashMap::new)); // Need to ignore duplicate keys??
    }

    public ConfigCategory[] getCategories() {
        return ConfigCategory.values();
    }

    public Collection<Option> getValues() {
        return options.values();
    }

    public Collection<Option> getValues(ConfigCategory category) {
        return options.values().stream().filter(o -> o.category.equals(category)).collect(Collectors.toList());
    }

    public Option getOption(String key) {
        return options.get(key);
    }

    public List<Option> getOptions(String includeOptions) {
        final String r = includeOptions.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*").replace(' ', '|');
        return this.options.values().stream().filter(o -> o.getKey().matches(r)).collect(Collectors.toList());
    }

    public class Option {

        private String key;
        private ConfigCategory category;
        private boolean build;
        private String description;
        private String defaultValue;
        private List<String> expectedValues;

        public Option(String key, ConfigCategory category, boolean build, String description, String defaultValue, Iterable<String> expectedValues) {
            this.key = key;
            this.category = category;
            this.build = build;
            this.description = description;
            this.defaultValue = defaultValue;
            this.expectedValues = StreamSupport.stream(expectedValues.spliterator(), false).collect(Collectors.toList());
        }

        public boolean isBuild() {
            return build;
        }

        public String getKey() {
            return key.substring(3);
        }

        public String getKeyCli() {
            return "--" + key.substring(3).replace('.', '-');
        }

        public String getKeyEnv() {
            return key.toUpperCase().replace('.', '_').replace('-', '_');
        }

        public String getDescription() {
            int i = description.indexOf('.');
            if (i == -1) {
                return description;
            } else {
                return description.substring(0, i + 1).trim();
            }
        }

        public String getDescriptionExtended() {
            int i = description.indexOf('.');
            if (i == -1) {
                return null;
            } else {
                String extended = description.substring(i + 1).trim();
                return extended.length() > 0 ? extended : null;
            }
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public List<String> getExpectedValues() {
            return expectedValues;
        }
    }

}
