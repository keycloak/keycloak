package org.keycloak.guides.maven;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderManager;
import org.keycloak.quarkus.runtime.Providers;

public class ProtocolMappers {

    private static final String MESSAGES_RELATIVE_PATH = "js/apps/admin-ui/maven-resources/theme/keycloak.v2/admin/messages/messages_en.properties";

    private final Map<String, Map<String, List<ProtocolMapperInfo>>> mappers;
    private final Properties messages;

    public ProtocolMappers(Path projectRootDir) {
        messages = loadMessages(projectRootDir);
        ProviderManager providerManager = Providers.getProviderManager(Thread.currentThread().getContextClassLoader());

        mappers = providerManager.loadSpis().stream()
                .filter(spi -> spi.getName().equals("protocol-mapper"))
                .findFirst()
                .<Map<String, Map<String, List<ProtocolMapperInfo>>>>map(spi -> providerManager.load(spi).stream()
                        .map(ProtocolMapper.class::cast)
                        .sorted(Comparator.comparing(ProtocolMapper::getDisplayType))
                        .map(mapper -> new ProtocolMapperInfo(
                                mapper.getId(),
                                mapper.getClass().getName(),
                                mapper.getProtocol(),
                                mapper.getDisplayType(),
                                mapper.getDisplayCategory(),
                                mapper.getHelpText(),
                                mapper.getPriority(),
                                mapper.getConfigProperties()
                        ))
                        .collect(Collectors.groupingBy(
                                ProtocolMapperInfo::protocol,
                                LinkedHashMap::new,
                                Collectors.groupingBy(
                                        ProtocolMapperInfo::category,
                                        LinkedHashMap::new,
                                        Collectors.toList()
                                )
                        )))
                .orElse(Map.of());
    }

    public Map<String, Map<String, List<ProtocolMapperInfo>>> getMappersByProtocol(List<String> protocols) {
        Map<String, Map<String, List<ProtocolMapperInfo>>> ordered = new LinkedHashMap<>();
        for (String protocol : protocols) {
            Map<String, List<ProtocolMapperInfo>> categoryMap = mappers.get(protocol);
            if (categoryMap != null) {
                ordered.put(protocol, categoryMap);
            }
        }
        return ordered;
    }

    public String resolveLabel(String label) {
        if (label != null && label.endsWith(".label")) {
            return messages.getProperty(label, label);
        }
        return label;
    }

    public String resolveTooltip(String tooltip) {
        if (tooltip != null && tooltip.endsWith(".tooltip")) {
            return messages.getProperty(tooltip, tooltip);
        }
        return tooltip;
    }

    private static Properties loadMessages(Path projectRootDir) {
        Properties props = new Properties();
        Path messagesFile = projectRootDir.resolve(MESSAGES_RELATIVE_PATH);
        if (Files.exists(messagesFile)) {
            try (Reader reader = Files.newBufferedReader(messagesFile)) {
                props.load(reader);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load admin messages properties from " + messagesFile, e);
            }
        }
        return props;
    }

    public record ProtocolMapperInfo(String id, String implementationClass, String protocol, String displayType,
                                     String category, String helpText, int priority,
                                     List<ProviderConfigProperty> configProperties) {

    }
}
