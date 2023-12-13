package org.keycloak.guides.maven;

import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toDashCase;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import org.apache.commons.lang3.ArrayUtils;
import org.keycloak.config.ConfigSupportLevel;
import org.keycloak.config.DeprecatedMetadata;
import org.keycloak.config.OptionCategory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.Spi;
import org.keycloak.quarkus.runtime.Providers;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import java.util.ArrayList;
import java.util.Arrays;
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

    @SuppressWarnings("unchecked")
    public Options() {
        options = PropertyMappers.getMappers().stream()
                .filter(m -> !m.isHidden())
                .filter(propertyMapper -> Objects.nonNull(propertyMapper.getDescription()))
                .map(m -> new Option(m.getFrom(), m.getCategory(), m.isBuildTime(), null, m.getDescription(), (String) m.getDefaultValue().map(Object::toString).orElse(null), m.getExpectedValues(), (DeprecatedMetadata) m.getDeprecatedMetadata().orElse(null)))
                .sorted(Comparator.comparing(Option::getKey))
                .collect(Collectors.toMap(Option::getKey, o -> o, (o1, o2) -> o1, LinkedHashMap::new)); // Need to ignore duplicate keys??
        ProviderManager providerManager = Providers.getProviderManager(Thread.currentThread().getContextClassLoader());

        options.forEach((s, option) -> {
            option.description = option.description.replaceAll("'([^ ]*)'", "`$1`");
        });

        for (Spi loadSpi : providerManager.loadSpis().stream().sorted(Comparator.comparing(Spi::getName)).collect(Collectors.toList())) {
            for (ProviderFactory providerFactory : providerManager.load(loadSpi).stream().sorted(Comparator.comparing(ProviderFactory::getId)).collect(Collectors.toList())) {
                List<ProviderConfigProperty> configMetadata = providerFactory.getConfigMetadata();

                if (configMetadata == null) {
                    continue;
                }

                String optionPrefix = NS_KEYCLOAK_PREFIX + String.join(OPTION_PART_SEPARATOR, ArrayUtils.insert(0, new String[] {loadSpi.getName(), providerFactory.getId()}, "spi"));
                List<Option> options = configMetadata.stream()
                        .map(m -> new Option(Configuration.toDashCase(optionPrefix.concat("-") + m.getName()), OptionCategory.GENERAL, false,
                                m.getType(),
                                m.getHelpText(),
                                m.getDefaultValue() == null ? null : m.getDefaultValue().toString(),
                                m.getOptions() == null ? Collections.emptyList() : m.getOptions(),
                                null))
                        .sorted(Comparator.comparing(Option::getKey)).collect(Collectors.toList());

                ArrayList<String> booleanValues = new ArrayList<>();
                booleanValues.add("true");
                booleanValues.add("false");
                options.forEach(option -> {
                    if (option.type.equals("boolean")) {
                        option.expectedValues = booleanValues;
                    }
                    option.description = option.description.replaceAll("'([^ ]*)'", "`$1`");
                });

                if (!options.isEmpty()) {
                    providerOptions.computeIfAbsent(toDashCase(loadSpi.getName()), k -> new LinkedHashMap<>()).put(toDashCase(providerFactory.getId()), options);
                }
            }
        }
    }

    public List<OptionCategory> getCategories() {
        return Arrays.stream(OptionCategory.values())
                .filter(c -> c.getSupportLevel() != ConfigSupportLevel.EXPERIMENTAL)
                .collect(Collectors.toList());
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
        private String type;
        private String description;
        private String defaultValue;
        private List<String> expectedValues;
        private DeprecatedMetadata deprecated;

        public Option(String key, OptionCategory category, boolean build, String type, String description, String defaultValue, Iterable<String> expectedValues, DeprecatedMetadata deprecatedMetadata) {
            this.key = key;
            this.category = category;
            this.build = build;
            this.type = type;
            this.description = description;
            this.defaultValue = defaultValue;
            this.expectedValues = StreamSupport.stream(expectedValues.spliterator(), false).collect(Collectors.toList());
            this.deprecated = deprecatedMetadata;
        }

        public boolean isBuild() {
            return build;
        }

        public String getKey() {
            return key.substring(3);
        }

        public String getType() {
            return type;
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

        public DeprecatedMetadata getDeprecated() {
            return deprecated;
        }
    }

}
