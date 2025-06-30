package org.keycloak.guides.maven;

import freemarker.template.TemplateException;
import org.apache.maven.plugin.logging.Log;
import org.keycloak.common.Version;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class GuideBuilder {

    private final FreeMarker freeMarker;
    private final File srcDir;
    private final File targetDir;
    private final Log log;

    public GuideBuilder(File srcDir, File targetDir, Log log, Properties properties) throws IOException {
        this.srcDir = srcDir;
        this.targetDir = targetDir;
        this.log = log;

        Map<String, Object> globalAttributes = new HashMap<>();
        globalAttributes.put("ctx", new Context(srcDir));
        globalAttributes.put("version", Version.VERSION);
        globalAttributes.put("properties", properties);

        this.freeMarker = new FreeMarker(srcDir.getParentFile(), globalAttributes);
    }

    public void build() throws TemplateException, IOException {
        if (!srcDir.isDirectory()) {
            if (!srcDir.mkdir()) {
                throw new RuntimeException("Can't create folder " + srcDir);
            }
        }

        for (String t : srcDir.list((dir, name) -> name.endsWith(".adoc"))) {
            freeMarker.template(srcDir.getName() + "/" + t, targetDir.getParentFile());
            if (log != null) {
                log.info("Templated: " + srcDir.getName() + "/" + t);
            }
        }

        File templatesDir = new File(srcDir, "templates");
        if (templatesDir.isDirectory()) {
            for (String t : templatesDir.list((dir, name) -> name.endsWith(".adoc"))) {
                freeMarker.template(srcDir.getName() + "/" + templatesDir.getName() + "/" + t, targetDir.getParentFile());
                if (log != null) {
                    log.info("Templated: " + templatesDir.getName() + "/" + t);
                }
            }
        }
    }

}
