package org.keycloak.client.admin.cli.v2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.keycloak.client.cli.util.OutputUtil;

public final class KcAdmV2DescriptorCache {

    public static final String REGISTRY_FILENAME = "registry.json";
    public static final String DESCRIPTOR_PREFIX = "descriptor-";

    private final Path cacheDir;

    public KcAdmV2DescriptorCache(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    public KcAdmV2CommandDescriptor loadForServer(String serverUrl) {
        if (!Files.isDirectory(cacheDir)) {
            return null;
        }
        Registry registry = readRegistry();
        if (registry == null) {
            return null;
        }
        ServerEntry entry = registry.servers.get(serverUrl);
        if (entry == null || entry.version == null) {
            return null;
        }
        Path descriptorFile = descriptorPath(entry.version);
        if (!Files.isRegularFile(descriptorFile)) {
            return null;
        }
        try {
            return OutputUtil.MAPPER.readValue(descriptorFile.toFile(), KcAdmV2CommandDescriptor.class);
        } catch (IOException e) {
            return null;
        }
    }

    public void save(String serverUrl, KcAdmV2CommandDescriptor descriptor) {
        String newVersion = descriptor.getVersion();
        if (newVersion == null || newVersion.isBlank()) {
            throw new IllegalArgumentException("Descriptor version must not be null or blank");
        }

        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create cache directory: " + cacheDir, e);
        }

        Registry registry = readRegistryOrEmpty();
        String oldVersion = versionForServer(registry, serverUrl);

        try {
            OutputUtil.MAPPER.writeValue(descriptorPath(newVersion).toFile(), descriptor);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write descriptor: " + descriptorPath(newVersion), e);
        }

        registry.servers.put(serverUrl, new ServerEntry(newVersion));
        writeRegistry(registry);

        if (oldVersion != null && !oldVersion.equals(newVersion)) {
            deleteOrphanedDescriptor(registry, oldVersion);
        }
    }

    private void deleteOrphanedDescriptor(Registry registry, String version) {
        boolean stillReferenced = registry.servers.values().stream().anyMatch(e -> version.equals(e.version));
        if (!stillReferenced) {
            try {
                Files.deleteIfExists(descriptorPath(version));
            } catch (IOException ignored) {
            }
        }
    }

    private Path descriptorPath(String version) {
        String sanitized = version.replaceAll("[^a-zA-Z0-9_-]", "_");
        return cacheDir.resolve(DESCRIPTOR_PREFIX + sanitized + ".json");
    }

    private String versionForServer(Registry registry, String serverUrl) {
        ServerEntry entry = registry.servers.get(serverUrl);
        return entry != null ? entry.version : null;
    }

    private Registry readRegistry() {
        Path registryFile = cacheDir.resolve(REGISTRY_FILENAME);
        if (!Files.isRegularFile(registryFile)) {
            return null;
        }
        try {
            Registry registry = OutputUtil.MAPPER.readValue(registryFile.toFile(), Registry.class);
            if (registry == null || registry.servers == null) {
                return null;
            }
            return registry;
        } catch (IOException e) {
            return null;
        }
    }

    private Registry readRegistryOrEmpty() {
        Registry registry = readRegistry();
        return registry != null ? registry : new Registry();
    }

    private void writeRegistry(Registry registry) {
        try {
            OutputUtil.MAPPER.writeValue(cacheDir.resolve(REGISTRY_FILENAME).toFile(), registry);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write registry: " + cacheDir.resolve(REGISTRY_FILENAME), e);
        }
    }

    static class Registry {
        public Map<String, ServerEntry> servers = new LinkedHashMap<>();
    }

    static class ServerEntry {
        public String version;

        ServerEntry() {}

        ServerEntry(String version) {
            this.version = version;
        }
    }
}
