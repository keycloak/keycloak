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

import java.net.URL;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class ClasspathThemeTemplateTest {

    @Test
    public void classLoaderThemeContainsTemplatePath() throws Exception {
        URL template = new URL("file:/template.ftl");
        URL forbidden = new URL("file:/forbidden.ftl");
        // The classloader resolves the traversal too, as a real classpath does (see ResourceLoaderTest).
        ClassLoader classLoader = new StubClassLoader(Map.of(
                "theme/test/login/login.ftl", template,
                "theme/test/login/../../../forbidden.ftl", forbidden));

        ClassLoaderTheme theme = new ClassLoaderTheme("test", Theme.Type.LOGIN, classLoader);

        Assert.assertSame(template, theme.getTemplate("login.ftl"));
        Assert.assertNull(theme.getTemplate("../../../forbidden.ftl"));
    }

    @Test
    public void classpathResourceProviderContainsTemplatePath() throws Exception {
        URL template = new URL("file:/template.ftl");
        URL forbidden = new URL("file:/forbidden.ftl");
        ClassLoader classLoader = new StubClassLoader(Map.of(
                "theme-resources/templates/page.ftl", template,
                "theme-resources/templates/../forbidden.ftl", forbidden));

        ClasspathThemeResourceProviderFactory provider =
                new ClasspathThemeResourceProviderFactory("classpath", classLoader);

        Assert.assertSame(template, provider.getTemplate("page.ftl"));
        Assert.assertNull(provider.getTemplate("../forbidden.ftl"));
    }

    private static class StubClassLoader extends ClassLoader {

        private final Map<String, URL> resources;

        StubClassLoader(Map<String, URL> resources) {
            super(null);
            this.resources = resources;
        }

        @Override
        public URL getResource(String name) {
            return resources.get(name);
        }
    }
}
