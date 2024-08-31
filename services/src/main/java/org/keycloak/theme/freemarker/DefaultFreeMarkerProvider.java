package org.keycloak.theme.freemarker;

import freemarker.cache.URLTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.KeycloakSanitizerMethod;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.NonceBean;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultFreeMarkerProvider implements FreeMarkerProvider {
    private final ConcurrentHashMap<String, Template> cache;
    private final KeycloakSanitizerMethod kcSanitizeMethod;
    private final KeycloakSession session;

    public DefaultFreeMarkerProvider(ConcurrentHashMap<String, Template> cache, KeycloakSanitizerMethod kcSanitizeMethod, KeycloakSession session) {
        this.cache = cache;
        this.kcSanitizeMethod = kcSanitizeMethod;
        this.session = session;
    }

    @Override
    public String processTemplate(Map<String, Object> attributes, String templateName, Theme theme) throws FreeMarkerException {
        attributes.put("kcSanitize", kcSanitizeMethod);

        if (!attributes.containsKey("nonce")) {
            attributes.put("nonce", new NonceBean(session));
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
            template.process(attributes, out);
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
