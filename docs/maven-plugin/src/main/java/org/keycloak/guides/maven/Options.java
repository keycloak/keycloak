package org.keycloak.guides.maven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.config.ConfigSupportLevel;
import org.keycloak.config.DeprecatedMetadata;
import org.keycloak.config.OptionCategory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.Spi;
import org.keycloak.quarkus.runtime.Providers;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;
import org.keycloak.utils.StringUtil;

import static org.keycloak.quarkus.runtime.configuration.Configuration.OPTION_PART_SEPARATOR;
import static org.keycloak.quarkus.runtime.configuration.Configuration.toDashCase;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

public class Options {

    private final Map<OptionCategory, Set<Option>> options;
    private final Map<String, Map<String, List<Option>>> providerOptions = new LinkedHashMap<>();

    public Options() {
        this.options = new EnumMap<>(OptionCategory.class);
        var mappers = PropertyMappers.getMappers();
        mappers.addAll(PropertyMappers.getWildcardMappers());
        mappers.stream()
                .filter(m -> !m.isHidden())
                .filter(propertyMapper -> Objects.nonNull(propertyMapper.getDescription()))
                .map(m -> new Option(m.getFrom(),
                        m.getCategory(),
                        m.isBuildTime(),
                        m.getType().getSimpleName(),
                        m.getDescription(),
                        m.getDefaultValue().orElse(null),
                        m.getExpectedValues(),
                        m.isStrictExpectedValues(),
                        m.getEnabledWhen().orElse(""),
                        m.getDeprecatedMetadata().orElse(null),
                        m.getOption().getWildcardKey().orElse(null)))
                .forEach(o -> options.computeIfAbsent(o.category, k -> new TreeSet<>(Comparator.comparing(Option::getKey))).add(o));

        ProviderManager providerManager = Providers.getProviderManager(Thread.currentThread().getContextClassLoader());

        options.values()
                .stream()
                .flatMap(Collection::stream)
                .forEach(option -> option.description = option.description.replaceAll("'([^ ]*)'", "`$1`"));

        ArrayList<String> booleanValues = new ArrayList<>();
        booleanValues.add("true");
        booleanValues.add("false");

        for (Spi loadSpi : providerManager.loadSpis().stream().sorted(Comparator.comparing(Spi::getName)).toList()) {
            for (ProviderFactory<?> providerFactory : providerManager.load(loadSpi).stream().sorted(Comparator.comparing(ProviderFactory::getId)).toList()) {
                List<ProviderConfigProperty> configMetadata = providerFactory.getConfigMetadata();

                if (configMetadata == null) {
                    continue;
                }

                String spiKey = toDashCase(loadSpi.getName());
                String providerKey = toDashCase(providerFactory.getId());
                String optionPrefix = NS_KEYCLOAK_PREFIX + "spi" + OPTION_PART_SEPARATOR + spiKey + OPTION_PART_SEPARATOR + OPTION_PART_SEPARATOR + providerKey + OPTION_PART_SEPARATOR + OPTION_PART_SEPARATOR;
                List<Option> options = configMetadata.stream()
                        .map(m -> new Option(optionPrefix + toDashCase(m.getName()), OptionCategory.GENERAL, false,
                                m.getType(),
                                m.getHelpText(),
                                m.getDefaultValue(),
                                m.getOptions() == null ? Collections.emptyList() : m.getOptions(),
                                true,
                                "",
                                null,
                                null))
                        .sorted(Comparator.comparing(Option::getKey)).collect(Collectors.toList());

                options.forEach(option -> {
                    if (option.type.equals("boolean")) {
                        option.expectedValues = booleanValues;
                    }
                    option.description = option.description.replaceAll("'([^ ]*)'", "`$1`");
                });

                if (!options.isEmpty()) {
                    providerOptions.computeIfAbsent(spiKey, k -> new LinkedHashMap<>()).put(providerKey, options);
                }
            }
        }
    }

    public List<OptionCategory> getCategories() {
        return Arrays.stream(OptionCategory.values())
                .filter(c -> c.getSupportLevel() != ConfigSupportLevel.EXPERIMENTAL)
                .collect(Collectors.toList());
    }

    public Collection<Option> getValues(OptionCategory category) {
        return options.getOrDefault(category, Collections.emptySet());
    }

    public Option getOption(String key) {
        Set<Option> foundOptions = options.values().stream().flatMap(Collection::stream).filter(f -> f.getKey().equals(key)).collect(Collectors.toSet());
        if (foundOptions.size() > 1) {
            final var categories = foundOptions.stream().map(f -> f.category).map(OptionCategory::getHeading).collect(Collectors.joining(","));
            throw new IllegalArgumentException(String.format("Ambiguous options '%s' with categories: %s\n", key, categories));
        }
        return foundOptions.iterator().next();
    }

    /**
     * Get denied categories for guide options
     * <p>
     * Used in cases when multiple options can be found under the same name
     * By providing 'deniedCategories' parameter, we will not search for the option in these categories
     * <p>
     * f.e. when we specify {@code includedOptions="hostname"}, we should provide also {@code deniedCategories="hostname_v2"}
     * In that case, we will use the option from the old hostname provider
     *
     * @return denied categories, otherwise an empty set
     */
    public Set<OptionCategory> getDeniedCategories(String deniedCategories) {
        return Optional.ofNullable(deniedCategories)
                .filter(StringUtil::isNotBlank)
                .map(f -> f.split(" "))
                .map(Arrays::asList)
                .map(f -> f.stream()
                        .map(g -> {
                            try {
                                return OptionCategory.valueOf(g.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                throw new IllegalArgumentException("You have specified wrong category name in the 'deniedCategories' property", e);
                            }
                        })
                        .collect(Collectors.toSet()))
                .orElseGet(Collections::emptySet);
    }

    public List<Option> getOptions(String includeOptions, String excludedOptions, String deniedCategories) {
        final String include = replaceSpecialCharsInOptions(includeOptions);
        final String exclude = replaceSpecialCharsInOptions(excludedOptions);
        final Set<OptionCategory> denied = getDeniedCategories(deniedCategories);

        return options.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(f -> !denied.contains(f.category))
                .filter(f -> f.getKey().matches(include) && (exclude == null || !f.getKey().matches(exclude)))
                .toList();
    }

    private String replaceSpecialCharsInOptions(String options) {
        if (options != null) {
            return options.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*").replace(' ', '|');
        } else {
            return null;
        }
    }

    public Map<String, Map<String, List<Option>>> getProviderOptions() {
        return providerOptions;
    }

    public static class Option {

        private final String key;
        private final OptionCategory category;
        private final boolean build;
        private final String type;
        private String description;
        private final String defaultValue;
        private List<String> expectedValues;

        private final boolean strictExpectedValues;

        private final String enabledWhen;
        private final DeprecatedMetadata deprecated;

        private final String wildcardKey;

        public Option(String key,
                      OptionCategory category,
                      boolean build,
                      String type,
                      String description,
                      Object defaultValue,
                      Iterable<String> expectedValues,
                      boolean strictExpectedValues,
                      String enabledWhen,
                      DeprecatedMetadata deprecatedMetadata,
                      String wildcardKey) {
            this.key = key;
            this.category = category;
            this.build = build;
            this.type = type;
            this.description = description;
            this.defaultValue = org.keycloak.config.Option.getDefaultValueString(defaultValue);
            this.expectedValues = StreamSupport.stream(expectedValues.spliterator(), false).collect(Collectors.toList());
            this.strictExpectedValues = strictExpectedValues;
            this.enabledWhen = enabledWhen;
            this.deprecated = deprecatedMetadata;
            this.wildcardKey = wildcardKey;
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

        public boolean isStrictExpectedValues() {
            return strictExpectedValues;
        }

        public String getEnabledWhen() {
            if (StringUtil.isBlank(enabledWhen)) {
                return null;
            }
            return enabledWhen;
        }

        public DeprecatedMetadata getDeprecated() {
            return deprecated;
        }

        public String getWildcardKey() {
            return wildcardKey;
        }
    }

}
