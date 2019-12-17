/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.theme;

import freemarker.cache.URLTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.keycloak.Config;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerUtil {

    private ConcurrentHashMap<String, Template> cache;
    private final KeycloakSanitizerMethod kcSanitizeMethod = new KeycloakSanitizerMethod();

    public FreeMarkerUtil() {
        if (Config.scope("theme").getBoolean("cacheTemplates", true)) {
            cache = new ConcurrentHashMap<>();
        }
    }

    public String processTemplate(Object data, String templateName, Theme theme) throws FreeMarkerException {
        if (data instanceof Map) {
            ((Map)data).put("kcSanitize", kcSanitizeMethod);
        }
        
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
        
        // Assume *.ftl files are html.  This lets freemarker know how to
        // sanitize and prevent XSS attacks.
        if (templateName.toLowerCase().endsWith(".ftl")) {
            cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        }
        
        cfg.setTemplateLoader(new ThemeTemplateLoader(theme));
        return cfg.getTemplate(templateName, "UTF-8");
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
