package org.keycloak.guides.maven;

import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toDashCase;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import org.apache.commons.lang3.ArrayUtils;
import org.keycloak.config.OptionCategory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.Spi;
import org.keycloak.quarkus.runtime.Providers;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Options {

    private final Map<String, Option> options;
    private final Map<String, Map<String, List<Option>>> providerOptions = new LinkedHashMap<>();

    public Options() {
        options = PropertyMappers.getMappers().stream()
                .filter(m -> !m.isHidden())
                .filter(propertyMapper -> Objects.nonNull(propertyMapper.getDescription()))
                .map(m -> new Option(m.getFrom(), m.getCategory(), m.isBuildTime(), m.getDescription(), (String) m.getDefaultValue().map(Object::toString).orElse(null), m.getExpectedValues()))
                .sorted(Comparator.comparing(Option::getKey))
                .collect(Collectors.toMap(Option::getKey, o -> o, (o1, o2) -> o1, LinkedHashMap::new)); // Need to ignore duplicate keys??
        ProviderManager providerManager = Providers.getProviderManager(Thread.currentThread().getContextClassLoader());

        for (Spi loadSpi : providerManager.loadSpis().stream().sorted(Comparator.comparing(Spi::getName)).collect(Collectors.toList())) {
            for (ProviderFactory providerFactory : providerManager.load(loadSpi).stream().sorted(Comparator.comparing(ProviderFactory::getId)).collect(Collectors.toList())) {
                List<ProviderConfigProperty> configMetadata = providerFactory.getConfigMetadata();

                if (configMetadata == null) {
                    continue;
                }

                String optionPrefix = NS_KEYCLOAK_PREFIX + String.join(OPTION_PART_SEPARATOR, ArrayUtils.insert(0, new String[] {loadSpi.getName(), providerFactory.getId()}, "spi"));
                List<Option> options = configMetadata.stream()
                        .map(m -> new Option(Configuration.toDashCase(optionPrefix.concat("-") + m.getName()), OptionCategory.GENERAL, false,
                                m.getHelpText(),
                                m.getDefaultValue() == null ? "none" : m.getDefaultValue().toString(),
                                m.getOptions() == null ? (m.getType() == null ? Collections.emptyList() : Collections.singletonList(m.getType())) : m.getOptions()))
                        .sorted(Comparator.comparing(Option::getKey)).collect(Collectors.toList());

                if (!options.isEmpty()) {
                    providerOptions.computeIfAbsent(toDashCase(loadSpi.getName()), k -> new LinkedHashMap<>()).put(toDashCase(providerFactory.getId()), options);
                }
            }
        }
    }

    public OptionCategory[] getCategories() {
        return OptionCategory.values();
    }

    public Collection<Option> getValues() {
        return options.values();
    }

    public Collection<Option> getValues(OptionCategory category) {
        return options.values().stream().filter(o -> o.category.equals(category)).collect(Collectors.toList());
    }

    public Option getOption(String key) {
        return options.get(key);
    }

    public List<Option> getOptions(String includeOptions) {
        final String r = includeOptions.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*").replace(' ', '|');
        return this.options.values().stream().filter(o -> o.getKey().matches(r)).collect(Collectors.toList());
    }

    public Map<String, Map<String, List<Option>>> getProviderOptions() {
        return providerOptions;
    }

    public class Option {

        private String key;
        private OptionCategory category;
        private boolean build;
        private String description;
        private String defaultValue;
        private List<String> expectedValues;

        public Option(String key, OptionCategory category, boolean build, String description, String defaultValue, Iterable<String> expectedValues) {
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
