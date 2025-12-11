package org.keycloak.theme.freemarker;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.KeycloakSanitizerMethod;
import org.keycloak.theme.Theme;

import freemarker.cache.URLTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class DefaultFreeMarkerProvider implements FreeMarkerProvider {
    private final ConcurrentHashMap<String, Template> cache;
    private final KeycloakSanitizerMethod kcSanitizeMethod;

    public DefaultFreeMarkerProvider(ConcurrentHashMap<String, Template> cache, KeycloakSanitizerMethod kcSanitizeMethod) {
        this.cache = cache;
        this.kcSanitizeMethod = kcSanitizeMethod;
    }

    @Override
    public String processTemplate(Object data, String templateName, Theme theme) throws FreeMarkerException {
        if (data instanceof Map) {
            ((Map)data).put("kcSanitize", kcSanitizeMethod);
        }

        try {
            Template template;
            if (cache != null) {
                String key = theme.getType().toString().toLowerCase() + "/" + theme.getName() + "/" + templateName;
                template = cache.get(key);
                if (template == null) {
                    template = getTemplate(templateName, theme);
                    if (cache.putIfAbsent(key, template) != null) {
                        template = cache.get(key);
                    }
                }
            } else {
                template = getTemplate(templateName, theme);
            }

            Writer out = new StringWriter();
            template.process(data, out);
            return out.toString();
        } catch (Exception e) {
            throw new FreeMarkerException("Failed to process template " + templateName, e);
        }
    }

    private Template getTemplate(String templateName, Theme theme) throws IOException {
        Configuration cfg = new Configuration();

        // Assume *.ftl files are html.  This lets freemarker know how to
        // sanitize and prevent XSS attacks.
        if (templateName.toLowerCase().endsWith(".ftl")) {
            cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        }

        cfg.setTemplateLoader(new ThemeTemplateLoader(theme));
        return cfg.getTemplate(templateName, "UTF-8");
    }

    static class ThemeTemplateLoader extends URLTemplateLoader {

        private Theme theme;

        public ThemeTemplateLoader(Theme theme) {
            this.theme = theme;
        }

        @Override
        protected URL getURL(String name) {
            try {
                return theme.getTemplate(name);
            } catch (IOException e) {
                return null;
            }
        }

    }

    @Override
    public void close() {

    }
}
