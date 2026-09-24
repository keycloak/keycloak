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

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateMethodModelEx;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ButtonsMacrosTest {

    @Test
    public void preservesLegacyThemeHooks() throws Exception {
        String output = render("<@buttons.actionGroup group=false>"
                + "<@buttons.button label='Cancel' type='secondary' fullWidth=false/>"
                + "<@buttons.buttonLink href='/back' label='Back'/>"
                + "</@buttons.actionGroup>", Map.of(
                        "kcFormButtonsClass", "legacy-actions",
                        "kcButtonDefaultClass", "legacy-secondary"));

        assertTrue(output.contains("class=\"legacy-actions"));
        assertTrue(output.contains("class=\" legacy-secondary\" name="));
        assertTrue(output.contains("href=\"/back\" class=\" legacy-secondary "));
    }

    @Test
    public void inheritedLegacyThemeRespectsChildOverrides() throws Exception {
        Properties parent = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("theme/keycloak/login/theme.properties")) {
            assertTrue(input != null);
            parent.load(input);
        }
        assertFalse(parent.containsKey("kcFormActionGroupClass"));
        assertFalse(parent.containsKey("kcButtonSecondaryClass"));

        Map<String, String> merged = new HashMap<>();
        for (String key : parent.stringPropertyNames()) {
            merged.put(key, parent.getProperty(key));
        }
        merged.put("kcFormButtonsClass", "child-actions");
        merged.put("kcButtonDefaultClass", "child-secondary");
        String output = render("<@buttons.actionGroup group=false>"
                + "<@buttons.button label='Cancel' type='secondary' fullWidth=false/>"
                + "<@buttons.buttonLink href='/back' label='Back'/>"
                + "</@buttons.actionGroup>", merged);

        assertTrue(output.contains("class=\"child-actions"));
        assertTrue(output.contains("child-secondary"));
        assertFalse(output.contains("legacy-actions"));
    }

    @Test
    public void prefersExplicitThemeHooks() throws Exception {
        String output = render("<@buttons.actionGroup horizontal=true group=false>"
                + "<@buttons.button label='Cancel' type='secondary' fullWidth=false/>"
                + "</@buttons.actionGroup>", Map.of(
                        "kcFormActionGroupClass", "actions",
                        "kcFormButtonsClass", "legacy-actions",
                        "kcFormActionGroupHorizontalClass", "horizontal",
                        "kcButtonSecondaryClass", "secondary",
                        "kcButtonDefaultClass", "legacy-secondary"));

        assertTrue(output.contains("class=\"actions horizontal\""));
        assertTrue(output.contains("class=\" secondary\""));
        assertFalse(output.contains("legacy-"));
    }

    @Test
    public void escapesLabelsAndPreservesSubmissionAttributes() throws Exception {
        String output = render("<@buttons.button label='Save' labelText='Save & continue' "
                + "id='save' name='action' value='save' formnovalidate='formnovalidate' "
                + "attributes={'data-callback': 'onSubmitRecaptcha'} />", Map.of());

        assertTrue(output.contains("Save &amp; continue"));
        assertTrue(output.contains("name=\"action\""));
        assertTrue(output.contains("id=\"save\""));
        assertTrue(output.contains("value=\"save\""));
        assertTrue(output.contains("formnovalidate=\"formnovalidate\""));
        assertTrue(output.contains("data-callback=\"onSubmitRecaptcha\""));
    }

    private String render(String source, Map<String, String> properties) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setOutputFormat(HTMLOutputFormat.INSTANCE);
        configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "theme/base/login");
        Template template = new Template("wrapper", "<#import 'buttons.ftl' as buttons>" + source, configuration);
        TemplateMethodModelEx message = arguments -> arguments.get(0).toString();
        StringWriter output = new StringWriter();
        template.process(Map.of("properties", properties, "msg", message), output);
        return output.toString();
    }
}
