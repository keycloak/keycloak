package org.keycloak.guides.maven;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class FreeMarker {

    private final Map<String, Object> attributes;
    private final Configuration configuration;

    public FreeMarker(Path srcDir, Map<String, Object> attributes) throws IOException {
        this.attributes = attributes;

        configuration = new Configuration(Configuration.VERSION_2_3_31);
        configuration.setDirectoryForTemplateLoading(srcDir.toFile());
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
    }

    public void template(Path template, Path target) throws IOException, TemplateException {
        // We cannot use Path directly for the templateName as \ will be used on Windows
        String templateName = StreamSupport.stream(template.spliterator(), false)
              .map(p -> p.getFileName().toString())
              .collect(Collectors.joining("/"));

        Template t = configuration.getTemplate(templateName);
        Path out = target.resolve(template);

        Path parent = out.getParent();
        Files.createDirectories(parent);

        HashMap<String, Object> attrs = new HashMap<>(attributes);
        attrs.put("id", id(template));
        attrs.put("attributes", "../".repeat(template.getNameCount() - 1) + "attributes.adoc[]");
        attrs.put("parent", template.getNameCount() > 2 ? template.getName(1).toString() : "");

        try(Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            t.process(attrs, w);
        }
    }

    private String id(Path p) {
        p = p.getNameCount() > 2 ? p.subpath(1, p.getNameCount()) : p.getName(1);
        return Guide.toId(p.toString());
    }
}
