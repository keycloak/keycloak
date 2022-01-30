package org.keycloak.guides.maven;

import freemarker.template.TemplateException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GuideBuilder {

    private final FreeMarker freeMarker;
    private final File srcDir;
    private final File targetDir;
    private final Log log;

    public GuideBuilder(File srcDir, File targetDir, Log log) throws IOException {
        this.srcDir = srcDir;
        this.targetDir = targetDir;
        this.log = log;

        Map<String, Object> globalAttributes = new HashMap<>();
        globalAttributes.put("ctx", new Context(srcDir));

        this.freeMarker = new FreeMarker(srcDir, targetDir, globalAttributes);
    }

    public void server() throws TemplateException, IOException {
        File serverGuidesDir = new File(srcDir, "server");
        if (!serverGuidesDir.isDirectory()) {
            serverGuidesDir.mkdir();
        }

        for (String t : serverGuidesDir.list((dir, name) -> name.endsWith(".adoc"))) {
            freeMarker.template("server/" + t);
            if (log != null) {
                log.info("Templated: server/" + t);
            }
        }
    }

}
