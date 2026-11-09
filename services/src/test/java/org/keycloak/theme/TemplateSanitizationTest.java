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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.keycloak.theme.beans.MessageFormatterMethod;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
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

    private KeycloakSanitizerMethod kcSanitize;
    private MessageFormatterMethod msg;

    @Before
    public void setUp() throws Exception {
        kcSanitize = new KeycloakSanitizerMethod();
        Properties props = new Properties();
        props.setProperty("identityProviderLinkBody", "Someone wants to link your {1} account with {0} account of user {2}. If this was you, click the link below to link accounts\n\n{3}\n\nThis link will expire within {5}.\n\nIgnore this message if you did not request this.");
        props.setProperty("identityProviderLinkBodyBeforeLink", "Someone wants to link your {1} account with {0} account of user {2}. If this was you, click the link below to link accounts\n\n");
        props.setProperty("identityProviderLinkBodyAfterLink", "\n\nThis link will expire within {5}.\n\nIgnore this message if you did not request this.");
        props.setProperty("accountUpdatedTitle", "Account updated");
        props.setProperty("nestedFirstBrokerFlowMessage", "Re-authenticating with {0} as {1}.");
        msg = new MessageFormatterMethod(Locale.US, props);
    }

    private Template loadFullTemplate(String themePath, String relativePath) throws Exception {
        Configuration fullCfg = new Configuration(Configuration.VERSION_2_3_32);
        fullCfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        fullCfg.setDefaultEncoding("UTF-8");
        String directory = relativePath.substring(0, relativePath.lastIndexOf('/') + 1);
        fullCfg.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[] {
                new ClassTemplateLoader(getClass(), "/sanitization-themes/" + themePath + "/" + directory),
                new ClassTemplateLoader(getClass(), "/sanitization-themes/base/" + directory)
        }));
        if (relativePath.endsWith("login/template.ftl")) {
            // Invoke the real layout macro, including its imports and conditional branches.
            return new Template("layout-test",
                    "<#import \"template.ftl\" as layout><@layout.registrationLayout; section></@layout.registrationLayout>",
                    fullCfg);
        }
        return fullCfg.getTemplate(relativePath.substring(relativePath.lastIndexOf('/') + 1));
    }

    private Map<String, Object> createFullModel() {
        Map<String, Object> model = new HashMap<>();
        model.put("kcSanitize", kcSanitize);
        model.put("msg", msg);
        model.put("lang", "en");
        model.put("pageId", "info");
        model.put("darkMode", false);
        model.put("locale", Map.of("language", "en"));
        model.put("ltr", true);
        model.put("message", Map.of("summary", "Account linking", "type", "error"));

        Map<String, Object> url = new HashMap<>();
        url.put("resourcesPath", "http://localhost/resources");
        url.put("resourcesCommonPath", "http://localhost/resources-common");
        url.put("ssoLoginInOtherTabsUrl", "http://localhost/sso");
        url.put("loginAction", "http://localhost/login-action");
        model.put("url", url);

        Map<String, Object> realm = new HashMap<>();
        realm.put("internationalizationEnabled", false);
        model.put("realm", realm);

        Map<String, Object> properties = new HashMap<>();
        properties.put("kcAlertTitleClass", "alert-title");
        model.put("properties", properties);

        return model;
    }

    @Test
    public void testIdentityProviderLinkFtlTemplateRendering() throws Exception {
        Template template = loadFullTemplate("base", "email/html/identity-provider-link.ftl");

        String[] testPayloads = new String[] {
                "NormalUser",
                "<script>alert(1)</script>",
                "<br>",
                "<img src=x onerror=alert(1)>John",
                "User \"Test\" & Admin",
                "user{0}with{1}tokens",
                "<a href=\"https://evil.example\">Click here</a>"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = createFullModel();
            Map<String, Object> idpCtx = new HashMap<>();
            idpCtx.put("username", payload);

            model.put("msg", msg);
            model.put("identityProviderDisplayName", "GitHub");
            model.put("realmName", "master");
            model.put("identityProviderContext", idpCtx);
            model.put("link", "https://keycloak.example/link?a=1&b=2");
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
            Assert.assertFalse("Payload <script> tag must not render as live HTML", result.contains("<script>alert(1)</script>"));
            Assert.assertTrue("HTML-looking username text must be escaped", !payload.contains("<") || result.contains("&lt;"));
            Assert.assertFalse("Payload anchor with evil URL must not render", result.contains("href=\"https://evil.example\""));
            Assert.assertFalse("Payload <img> tag must not render as live HTML", result.contains("<img"));
            Assert.assertTrue("Confirmation URL must be a clickable link", result.contains("<a href=\"https://keycloak.example/link?a=1&amp;b=2\">https://keycloak.example/link?a=1&amp;b=2</a>"));
            Assert.assertTrue("Localized body must be rendered", result.contains("Someone wants to link your"));
            Assert.assertTrue("The confirmation link must stay between the localized introduction and expiration", result.indexOf("click the link below") < result.indexOf("<a href=") && result.indexOf("<a href=") < result.indexOf("This link will expire"));
            Assert.assertFalse("Link must not contain onclick", result.contains("onclick"));
        }
    }

    @Test
    public void testInfoFtlResolvesMessageHeaderKey() throws Exception {
        Template template = loadFullTemplate("base", "login/info.ftl");
        Map<String, Object> model = createFullModel();
        model.put("messageHeader", "accountUpdatedTitle");

        StringWriter writer = new StringWriter();
        template.process(model, writer);

        Assert.assertTrue("Translation key should resolve to its localized header", writer.toString().contains("Account updated"));
        Assert.assertFalse("Translation key should not be rendered literally", writer.toString().contains("accountUpdatedTitle"));
    }

    @Test
    public void testInfoFtlMessageHeaderAutoEscaping() throws Exception {
        Template template = loadFullTemplate("base", "login/info.ftl");
        Map<String, Object> model = createFullModel();
        model.put("messageHeader", "<a href=\"https://evil.example\">Click</a><script>alert(1)</script>");

        StringWriter writer = new StringWriter();
        template.process(model, writer);
        String result = writer.toString();

        Assert.assertFalse("Untrusted message header must not render a live anchor", result.contains("<a href=\"https://evil.example\">"));
        Assert.assertFalse("Untrusted message header must not render a live script", result.contains("<script>alert(1)</script>"));
        Assert.assertTrue("Untrusted message header must be escaped", result.contains("&lt;a href="));
    }

    @Test
    public void testInfoFtlMessageBodySanitization() throws Exception {
        Template template = loadFullTemplate("base", "login/info.ftl");

        String[] testPayloads = new String[] {
                "Account linked with <script>alert(1)</script>",
                "Account linked with <br>next",
                "Account linked with <a href=\"https://evil.example\">Click</a>",
                "Account linked with <a href=\"javascript:alert(1)\">Click</a>",
                "Account linked with <img src=x onerror=alert(1)>",
                "Normal account linking message"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = createFullModel();
            Map<String, Object> message = new HashMap<>();
            message.put("type", "error");
            message.put("summary", payload);
            model.put("message", message);

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Body <script> tag must not render as live HTML", result.contains("<script>alert(1)</script>"));
            if (payload.contains("<br>")) {
                Assert.assertTrue("A br in a message must be escaped as text", result.contains("&lt;br&gt;"));
            }
            Assert.assertFalse("Body javascript URL must not render as an attribute", result.contains("href=\"javascript:"));
            Assert.assertFalse("Body image tag must not render as live HTML", result.contains("<img"));

            if (payload.contains("https://evil.example")) {
                Assert.assertTrue("Untrusted anchor must be rendered as escaped text", result.contains("&lt;a href="));
                Assert.assertFalse("Untrusted anchor must not render as a live link", result.contains("href=\"https://evil.example\""));
            }
        }
    }

    @Test
    public void testFullInfoFtlTemplateRendering() throws Exception {
        Template template = loadFullTemplate("base", "login/info.ftl");

        String[] testPayloads = new String[] {
                "NormalUser",
                "R&D Team",
                "User \"Test\" & Admin",
                "<script>alert(1)</script>",
                "user{0}tokens<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>John",
                "<a href=\"https://evil.example\">Click here</a>"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = createFullModel();
            model.put("messageHeader", "Confirm linking account");

            Map<String, Object> message = new HashMap<>();
            message.put("type", "error");
            message.put("summary", "Summary payload: " + payload);
            model.put("message", message);

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Full info.ftl must not render live <script> tag", result.contains("<script>alert(1)</script>"));
            Assert.assertFalse("Full info.ftl must not render live <a href=\"https://evil.example\"", result.contains("href=\"https://evil.example\""));
            Assert.assertFalse("Full info.ftl must not render live <img> tag", result.contains("<img"));
            Assert.assertFalse("Full info.ftl must not mutate ampersands into fullwidth homoglyphs", result.contains("＆"));

            if ("R&D Team".equals(payload)) {
                Assert.assertTrue("R&D Team payload must preserve standard ampersand escaping", result.contains("R&amp;D Team") || result.contains("R&D Team"));
            }
        }
    }

    @Test
    public void testTemplateFtlNestedBrokerSummarySanitization() throws Exception {
        Template template = loadFullTemplate("base", "login/template.ftl");

        String[] testPayloads = new String[] {
                "NormalUser",
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>John",
                "User \"Test\" & Admin",
                "user{0}with{1}tokens",
                "<a href=\"https://evil.example\">Click here</a>"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = createFullModel();
            Map<String, Object> message = new HashMap<>();
            message.put("type", "error");
            message.put("summary", msg.exec(List.of("nestedFirstBrokerFlowMessage", "corp{1}", payload)).toString());
            model.put("msg", msg);
            model.put("message", message);

            Map<String, Object> properties = new HashMap<>();
            properties.put("kcAlertTitleClass", "alert-title");
            model.put("properties", properties);

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Payload <script> tag must not render as live HTML in summary", result.contains("<script>alert(1)</script>"));
            Assert.assertFalse("Payload anchor with evil URL must not render in summary", result.contains("href=\"https://evil.example\""));
            Assert.assertFalse("Payload <img> tag must not render as live HTML in summary", result.contains("<img"));

            if ("user{0}with{1}tokens".equals(payload)) {
                Assert.assertTrue("Token-containing username must not be mutated", result.contains("user{0}with{1}tokens"));
            }

            // Verify alias renders correctly without double-escaping
            Assert.assertTrue("Alias 'corp{1}' must appear in rendered output",
                    result.contains("corp{1}") || result.contains("corp&#123;1}"));
            Assert.assertFalse("Alias must not be double-escaped to &amp;#123;",
                    result.contains("corp&amp;#123;"));
        }
    }

    @Test
    public void testKeycloakV2TemplateNestedBrokerSummarySanitization() throws Exception {
        Template template = loadFullTemplate("keycloak.v2", "login/template.ftl");

        String[] testPayloads = new String[] {
                "NormalUser",
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>John",
                "User \"Test\" & Admin",
                "user{0}with{1}tokens",
                "<a href=\"https://evil.example\">Click here</a>"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = createFullModel();
            Map<String, Object> message = new HashMap<>();
            message.put("type", "error");
            message.put("summary", msg.exec(List.of("nestedFirstBrokerFlowMessage", "corp{1}", payload)).toString());
            model.put("msg", msg);
            model.put("message", message);

            Map<String, Object> properties = new HashMap<>();
            properties.put("kcAlertTitleClass", "alert-title");
            model.put("properties", properties);

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Payload <script> tag must not render as live HTML in V2 summary", result.contains("<script>alert(1)</script>"));
            Assert.assertFalse("Payload anchor with evil URL must not render in V2 summary", result.contains("href=\"https://evil.example\""));
            Assert.assertFalse("Payload <img> tag must not render as live HTML in V2 summary", result.contains("<img"));

            if ("user{0}with{1}tokens".equals(payload)) {
                Assert.assertTrue("Token-containing username must not be mutated", result.contains("user{0}with{1}tokens"));
            }
        }
    }

    @Test
    public void testTemplateFtlNonNestedSummarySanitization() throws Exception {
        for (String theme : List.of("base", "keycloak.v2")) {
            Template template = loadFullTemplate(theme, "login/template.ftl");

            Map<String, Object> model = createFullModel();
            Map<String, Object> message = new HashMap<>();
            message.put("type", "error");
            message.put("summary", "Message with <script>alert(1)</script> and <a href=\"https://example.com\">Link</a>");
            model.put("message", message);

            Map<String, Object> properties = new HashMap<>();
            properties.put("kcAlertTitleClass", "alert-title");
            model.put("properties", properties);

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Non-nested summary <script> tag must not render as live HTML", result.contains("<script>alert(1)</script>"));
            Assert.assertTrue("Non-nested summary anchor must be auto-escaped", result.contains("&lt;a href="));
            Assert.assertFalse("Non-nested summary anchor must not render as a live link", result.contains("href=\"https://example.com\""));
        }
    }
}
