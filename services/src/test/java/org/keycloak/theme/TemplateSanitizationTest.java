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

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.keycloak.theme.beans.MessageFormatterMethod;

import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test verifying FreeMarker FTL template parsing, HTML escaping, and sanitization for identity provider link email and info templates (Fixes #51277).
 */
public class TemplateSanitizationTest {

    private Configuration cfg;
    private KeycloakSanitizerMethod kcSanitize;
    private MessageFormatterMethod msg;

    @Before
    public void setUp() throws Exception {
        cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);

        kcSanitize = new KeycloakSanitizerMethod();

        Properties props = new Properties();
        props.setProperty("identityProviderLinkBodyHtml", "<p>Someone wants to link your account <b>{1}</b> with identity provider <b>{0}</b> as user <b>{2}</b>.</p><p><a href=\"{3}\">Link account</a></p>");
        props.setProperty("confirmAccountLinking", "Confirm linking account {0} of identity provider {1} with your account.");
        props.setProperty("nestedFirstBrokerFlowMessage", "Re-authenticating with {0} as {1}.");
        msg = new MessageFormatterMethod(Locale.US, props);
    }

    private File getThemeFile(String themePath, String relativePath) {
        File file = new File("../themes/src/main/resources/theme/" + themePath + "/" + relativePath);
        if (!file.exists()) {
            file = new File("themes/src/main/resources/theme/" + themePath + "/" + relativePath);
        }
        return file;
    }

    private String getMarkerExpression(String themePath, String relativePath, String marker) throws Exception {
        File templateFile = getThemeFile(themePath, relativePath);
        String ftlSource = new String(Files.readAllBytes(templateFile.toPath()), StandardCharsets.UTF_8);
        return Arrays.stream(ftlSource.split("\\R"))
                .map(String::trim)
                .filter(line -> line.contains(marker) && line.contains("kcSanitize("))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Sanitization marker '" + marker + "' not found in " + relativePath));
    }

    @Test
    public void testIdentityProviderLinkFtlTemplateRendering() throws Exception {
        String sinkLine = getMarkerExpression("base", "email/html/identity-provider-link.ftl", "identityProviderLinkBodyHtml");
        Template template = new Template("identity-provider-link", sinkLine, cfg);

        String[] testPayloads = new String[] {
                "NormalUser",
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>John",
                "User \"Test\" & Admin",
                "user{0}with{1}tokens"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = new HashMap<>();
            Map<String, Object> idpCtx = new HashMap<>();
            idpCtx.put("username", payload);

            model.put("kcSanitize", kcSanitize);
            model.put("msg", msg);
            model.put("identityProviderDisplayName", "GitHub");
            model.put("realmName", "master");
            model.put("identityProviderContext", idpCtx);
            model.put("link", "https://keycloak.example/link");
            model.put("linkExpiration", "5");
            model.put("linkExpirationFormatter", new TemplateMethodModelEx() {
                @Override
                public Object exec(List arguments) throws TemplateModelException {
                    return arguments.isEmpty() ? "5 minutes" : arguments.get(0) + " minutes";
                }
            });

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Payload <script> tag must not render as live HTML", result.contains("<script>"));
            Assert.assertFalse("Payload <img> tag must not render as live HTML", result.contains("<img"));
            Assert.assertTrue("Link href must be preserved", result.contains("href=\"https://keycloak.example/link\""));
            Assert.assertTrue("Link text must be preserved", result.contains("Link account"));
            Assert.assertTrue("Link must have rel attribute for safety", result.contains("rel="));
            Assert.assertFalse("Link must not contain onclick", result.contains("onclick"));
        }
    }

    @Test
    public void testInfoFtlMessageHeaderSanitization() throws Exception {
        String sinkLine = getMarkerExpression("base", "login/info.ftl", "messageHeaderUsername");
        Template template = new Template("info-header", sinkLine, cfg);

        String[] testPayloads = new String[] {
                "NormalUser",
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>John",
                "User \"Test\" & Admin",
                "user{0}with{1}tokens"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = new HashMap<>();
            model.put("kcSanitize", kcSanitize);
            model.put("msg", msg);
            model.put("messageHeader", "confirmAccountLinking");
            model.put("messageHeaderUsername", payload);
            model.put("messageHeaderAlias", "corp{0}");

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Payload <script> tag must not render as live HTML in info header", result.contains("<script>"));
            Assert.assertFalse("Payload <img> tag must not render as live HTML in info header", result.contains("<img"));

            if ("user{0}with{1}tokens".equals(payload)) {
                Assert.assertTrue("Token-containing username must not be mutated", result.contains("user{0}with{1}tokens"));
                Assert.assertFalse("Replacement order regression: username must not be corrupted", result.contains("usergithubwith"));
            }
        }
    }

    @Test
    public void testTemplateFtlNestedBrokerSummarySanitization() throws Exception {
        String sinkLine = getMarkerExpression("base", "login/template.ftl", "nestedIdpUsername");
        Template template = new Template("template-summary", sinkLine, cfg);

        String[] testPayloads = new String[] {
                "NormalUser",
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>John",
                "User \"Test\" & Admin",
                "user{0}with{1}tokens"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("summary", "nestedFirstBrokerFlowMessage");
            model.put("kcSanitize", kcSanitize);
            model.put("msg", msg);
            model.put("message", message);
            model.put("nestedIdpHeader", "nestedFirstBrokerFlowMessage");
            model.put("nestedIdpAlias", "corp{1}");
            model.put("nestedIdpUsername", payload);

            Map<String, Object> properties = new HashMap<>();
            properties.put("kcAlertTitleClass", "alert-title");
            model.put("properties", properties);

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Payload <script> tag must not render as live HTML in summary", result.contains("<script>"));
            Assert.assertFalse("Payload <img> tag must not render as live HTML in summary", result.contains("<img"));

            if ("user{0}with{1}tokens".equals(payload)) {
                Assert.assertTrue("Token-containing username must not be mutated", result.contains("user{0}with{1}tokens"));
            }
        }
    }

    @Test
    public void testKeycloakV2TemplateNestedBrokerSummarySanitization() throws Exception {
        String sinkLine = getMarkerExpression("keycloak.v2", "login/template.ftl", "nestedIdpUsername");
        Template template = new Template("v2-template-summary", sinkLine, cfg);

        String[] testPayloads = new String[] {
                "NormalUser",
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>John",
                "User \"Test\" & Admin",
                "user{0}with{1}tokens"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("summary", "nestedFirstBrokerFlowMessage");
            model.put("kcSanitize", kcSanitize);
            model.put("msg", msg);
            model.put("message", message);
            model.put("nestedIdpHeader", "nestedFirstBrokerFlowMessage");
            model.put("nestedIdpAlias", "corp{1}");
            model.put("nestedIdpUsername", payload);

            Map<String, Object> properties = new HashMap<>();
            properties.put("kcAlertTitleClass", "alert-title");
            model.put("properties", properties);

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Payload <script> tag must not render as live HTML in V2 summary", result.contains("<script>"));
            Assert.assertFalse("Payload <img> tag must not render as live HTML in V2 summary", result.contains("<img"));

            if ("user{0}with{1}tokens".equals(payload)) {
                Assert.assertTrue("Token-containing username must not be mutated", result.contains("user{0}with{1}tokens"));
            }
        }
    }
}
