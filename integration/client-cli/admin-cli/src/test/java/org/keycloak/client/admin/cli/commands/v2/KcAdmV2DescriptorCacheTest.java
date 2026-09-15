package org.keycloak.client.admin.cli.commands.v2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.CommandDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor.ResourceDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2DescriptorCache;
import org.keycloak.client.cli.config.FileConfigHandler;
import org.keycloak.client.cli.util.ConfigUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.client.admin.cli.v2.KcAdmV2DescriptorCache.DESCRIPTOR_PREFIX;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KcAdmV2DescriptorCacheTest {

    private Path tempDir;
    private KcAdmV2DescriptorCache cache;

    @Before
    public void setUp() throws IOException {
        ConfigUtil.setHandler(null);
        FileConfigHandler.setConfigFile(null);
        tempDir = Files.createTempDirectory("kcadm-cache-test");
        cache = new KcAdmV2DescriptorCache(tempDir);
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            deleteRecursively(tempDir);
        }
        ConfigUtil.setHandler(null);
        FileConfigHandler.setConfigFile(null);
    }

    @Test
    public void loadReturnsNullForUnknownServer() {
        assertNull(cache.loadForServer("http://unknown:8080"));
    }

    @Test
    public void saveAndLoadRoundTrip() {
        KcAdmV2CommandDescriptor descriptor = descriptorWithVersion("26.0.0");

        cache.save("http://localhost:8080", descriptor);

        KcAdmV2CommandDescriptor loaded = cache.loadForServer("http://localhost:8080");
        assertNotNull(loaded);
        assertEquals("26.0.0", loaded.getVersion());
        assertEquals(1, loaded.getResources().size());
        assertEquals("client", loaded.getResources().get(0).getName());
    }

    @Test
    public void twoServersWithSameVersionShareOneDescriptorFile() {
        KcAdmV2CommandDescriptor descriptor = descriptorWithVersion("26.0.0");

        cache.save("http://server-a:8080", descriptor);
        cache.save("http://server-b:8080", descriptor);

        KcAdmV2CommandDescriptor loadedA = cache.loadForServer("http://server-a:8080");
        KcAdmV2CommandDescriptor loadedB = cache.loadForServer("http://server-b:8080");
        assertNotNull(loadedA);
        assertNotNull(loadedB);
        assertEquals("26.0.0", loadedA.getVersion());
        assertEquals("26.0.0", loadedB.getVersion());

        long descriptorFileCount = countDescriptorFiles();
        assertEquals("two servers of same version should share one descriptor file",
                1, descriptorFileCount);
    }

    @Test
    public void differentVersionsCreateSeparateDescriptorFiles() {
        cache.save("http://server-a:8080", descriptorWithVersion("26.0.0"));
        cache.save("http://server-b:8080", descriptorWithVersion("27.0.0-SNAPSHOT"));

        assertEquals(2, countDescriptorFiles());

        assertEquals("26.0.0", cache.loadForServer("http://server-a:8080").getVersion());
        assertEquals("27.0.0-SNAPSHOT", cache.loadForServer("http://server-b:8080").getVersion());
    }

    @Test
    public void saveUpdatesVersionWhenServerUpgraded() {
        cache.save("http://localhost:8080", descriptorWithVersion("26.0.0"));
        assertEquals("26.0.0", cache.loadForServer("http://localhost:8080").getVersion());
        assertEquals(1, countDescriptorFiles());

        cache.save("http://localhost:8080", descriptorWithVersion("27.0.0"));
        assertEquals("27.0.0", cache.loadForServer("http://localhost:8080").getVersion());
        assertEquals("old descriptor file should be removed", 1, countDescriptorFiles());
    }

    @Test
    public void saveKeepsOldDescriptorIfReferencedByOtherServer() {
        cache.save("http://server-a:8080", descriptorWithVersion("26.0.0"));
        cache.save("http://server-b:8080", descriptorWithVersion("26.0.0"));
        assertEquals(1, countDescriptorFiles());

        cache.save("http://server-a:8080", descriptorWithVersion("27.0.0"));
        assertEquals("old descriptor kept because server-b still uses it",
                2, countDescriptorFiles());
    }

    @Test
    public void versionIsSanitizedInFilename() {
        String maliciousVersion = "../../etc/passwd";
        Path sanitizedFile = tempDir.resolve(DESCRIPTOR_PREFIX + "______etc_passwd.json");

        assertFalse("sanitized file should not exist before save", Files.exists(sanitizedFile));

        cache.save("http://localhost:8080", descriptorWithVersion(maliciousVersion));

        assertTrue("descriptor should be saved with sanitized filename", Files.exists(sanitizedFile));
        assertNotNull(cache.loadForServer("http://localhost:8080"));
    }

    @Test
    public void loadReturnsNullWhenCacheDirDoesNotExist() throws IOException {
        deleteRecursively(tempDir);

        assertNull(cache.loadForServer("http://localhost:8080"));
    }

    @Test
    public void saveCreatesDirectoryIfNeeded() {
        Path nested = tempDir.resolve("sub").resolve("dir");
        KcAdmV2DescriptorCache nestedCache = new KcAdmV2DescriptorCache(nested);

        nestedCache.save("http://localhost:8080", descriptorWithVersion("26.0.0"));

        assertTrue(Files.isDirectory(nested));
        assertNotNull(nestedCache.loadForServer("http://localhost:8080"));
    }

    private KcAdmV2CommandDescriptor descriptorWithVersion(String version) {
        CommandDescriptor cmd = new CommandDescriptor();
        cmd.setName("list");
        cmd.setResourceName("client");
        cmd.setHttpMethod("GET");
        cmd.setPath("/admin/api/{realmName}/clients/{version}");
        cmd.setDescription("List clients");
        cmd.setOptions(List.of());

        ResourceDescriptor resource = new ResourceDescriptor();
        resource.setName("client");
        resource.setCommands(List.of(cmd));

        KcAdmV2CommandDescriptor descriptor = new KcAdmV2CommandDescriptor();
        descriptor.setVersion(version);
        descriptor.setResources(List.of(resource));
        return descriptor;
    }

    private long countDescriptorFiles() {
        try (var files = Files.list(tempDir)) {
            return files
                    .filter(p -> p.getFileName().toString().startsWith(DESCRIPTOR_PREFIX))
                    .count();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list descriptor files", e);
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        }
    }

}
