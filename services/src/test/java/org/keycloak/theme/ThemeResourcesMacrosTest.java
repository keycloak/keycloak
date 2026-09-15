/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import freemarker.core.HTMLOutputFormat;
import freemarker.core.NonBooleanException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThemeResourcesMacrosTest {

    @Test
    public void renderScriptWithDeferAndAsync() throws Exception {
        ThemeResourceDescriptor script = ThemeResourceDescriptor.builder("js/app.js")
                .defer("true")
                .async("async")
                .build();

        String output = renderScripts(List.of(script));

        assertTrue(output.contains(" defer"));
        assertTrue(output.contains(" async"));
        assertTrue(output.contains("src=\"/resources/js/app.js\""));
    }

    @Test
    public void hasDeferRequiresMethodCallInFreeMarker() throws Exception {
        ThemeResourceDescriptor script = ThemeResourceDescriptor.builder("js/app.js")
                .defer("true")
                .build();

        assertEquals("YES", processTemplate("<#if resource.hasDefer()>YES</#if>", script));
    }

    @Test(expected = NonBooleanException.class)
    public void hasDeferWithoutParenthesesFailsInFreeMarker() throws Exception {
        ThemeResourceDescriptor script = ThemeResourceDescriptor.builder("js/app.js")
                .defer("true")
                .build();

        processTemplate("<#if resource.hasDefer>YES</#if>", script);
    }

    @Test
    public void deferAndAsyncAreNotInterchangeable() {
        ThemeResourceDescriptor deferOnly = ThemeResourceDescriptor.builder("js/app.js")
                .defer("async")
                .build();
        ThemeResourceDescriptor asyncOnly = ThemeResourceDescriptor.builder("js/app.js")
                .async("defer")
                .build();

        assertFalse(deferOnly.hasDefer());
        assertFalse(asyncOnly.hasAsync());
    }

    private String renderScripts(List<ThemeResourceDescriptor> scripts) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "theme/base/login");

        Template template = new Template("wrapper",
                "<#import \"theme-resources.ftl\" as tags><@tags.renderScripts scripts pathPrefix defaultScriptType />",
                cfg);

        Writer out = new StringWriter();
        template.process(Map.of(
                "scripts", scripts,
                "pathPrefix", "/resources",
                "defaultScriptType", "text/javascript"
        ), out);
        return out.toString();
    }

    private String processTemplate(String source, ThemeResourceDescriptor resource) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        Template template = new Template("test", source, cfg);

        Writer out = new StringWriter();
        template.process(Map.of("resource", resource), out);
        return out.toString();
    }
}
