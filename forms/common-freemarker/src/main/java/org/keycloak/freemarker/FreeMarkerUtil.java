package org.keycloak.freemarker;

import freemarker.cache.URLTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.keycloak.Config;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerUtil {

    private ConcurrentHashMap<String, Template> cache;

    public FreeMarkerUtil() {
        if (Config.scope("theme").getBoolean("cacheTemplates", true)) {
            cache = new ConcurrentHashMap<String, Template>();
        }
    }

    public String processTemplate(Object data, String templateName, Theme theme) throws FreeMarkerException {
        try {
            Template template;
            if (cache != null) {
                String key = theme.getName() + "/" + templateName;
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
        cfg.setTemplateLoader(new ThemeTemplateLoader(theme));
        return cfg.getTemplate(templateName);
    }

    class ThemeTemplateLoader extends URLTemplateLoader {

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

}
