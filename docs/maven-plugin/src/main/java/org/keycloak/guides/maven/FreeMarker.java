package org.keycloak.guides.maven;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class FreeMarker {

    private File targetDir;
    private Map<String, Object> attributes;
    private Configuration configuration;

    public FreeMarker(File srcDir, File targetDir, Map<String, Object> attributes) throws IOException {
        this.targetDir = targetDir;
        this.attributes = attributes;

        configuration = new Configuration(Configuration.VERSION_2_3_31);
        configuration.setDirectoryForTemplateLoading(srcDir);
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
    }

    public void template(String template) throws IOException, TemplateException {
        Template t = configuration.getTemplate(template);
        File out = targetDir.toPath().resolve(template).toFile();

        File parent = out.getParentFile();
        if (!parent.isDirectory()) {
            parent.mkdir();
        }

        HashMap<String, Object> attrs = new HashMap<>(attributes);
        attrs.put("id", template.split("/")[1].replace(".adoc", ""));

        Writer w = new FileWriter(out);
        t.process(attrs, w);
    }

}
