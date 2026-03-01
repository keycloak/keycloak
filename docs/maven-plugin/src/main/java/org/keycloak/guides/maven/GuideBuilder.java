package org.keycloak.guides.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.keycloak.common.Version;

import freemarker.template.TemplateException;
import org.apache.maven.plugin.logging.Log;

public class GuideBuilder {

    private final FreeMarker freeMarker;
    private final Path srcDir;
    private final Path targetDir;
    private final Log log;

    public GuideBuilder(Path srcDir, Path targetDir, Log log, Properties properties) throws IOException {
        this.srcDir = srcDir;
        this.targetDir = targetDir;
        this.log = log;

        Map<String, Object> globalAttributes = new HashMap<>();
        globalAttributes.put("ctx", new Context(srcDir));
        globalAttributes.put("version", Version.VERSION);
        globalAttributes.put("properties", properties);

        this.freeMarker = new FreeMarker(srcDir.getParent(), globalAttributes);
    }

    public void build() throws TemplateException, IOException {
        Files.createDirectories(srcDir);
        Path partials = srcDir.resolve("partials");
        List<Path> templatePaths;
        try (Stream<Path> files = Files.walk(srcDir)) {
            templatePaths = files
                  .filter(Files::isRegularFile)
                  .filter(p -> !p.startsWith(partials))
                  .filter(p -> p.getFileName().toString().endsWith(".adoc"))
                  .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to discover templates in " + srcDir, e);
        }

        for (Path path : templatePaths) {
            Path relativePath = srcDir.getParent().relativize(path);
            freeMarker.template(relativePath, targetDir.getParent());
            if (log != null) {
                log.info("Templated: " + relativePath);
            }
        }
    }
}
