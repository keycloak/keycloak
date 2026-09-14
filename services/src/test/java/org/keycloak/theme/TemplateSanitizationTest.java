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
                .filter(line -> line.contains(marker) && line.contains("${"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Sanitization marker '" + marker + "' not found in " + relativePath));
    }

    private Template loadFullTemplate(String themePath, String relativePath) throws Exception {
        File file = getThemeFile(themePath, relativePath);
        Configuration fullCfg = new Configuration(Configuration.VERSION_2_3_32);
        fullCfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        fullCfg.setDirectoryForTemplateLoading(file.getParentFile());
        return fullCfg.getTemplate(file.getName());
    }

    private Map<String, Object> createFullModel() {
        Map<String, Object> model = new HashMap<>();
        model.put("kcSanitize", kcSanitize);
        model.put("msg", msg);
        model.put("lang", "en");
        model.put("pageId", "info");

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
        String sinkLine = getMarkerExpression("base", "email/html/identity-provider-link.ftl", "identityProviderLinkBodyHtml");
        Template template = new Template("identity-provider-link", sinkLine, cfg);

        String[] testPayloads = new String[] {
                "NormalUser",
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>John",
                "User \"Test\" & Admin",
                "user{0}with{1}tokens",
                "<a href=\"https://evil.example\">Click here</a>"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = new HashMap<>();
            Map<String, Object> idpCtx = new HashMap<>();
            idpCtx.put("username", payload);

            model.put("kcSanitize", kcSanitize);
            model.put("msg", msg);
            model.put("identityProviderUsernameSentinel", java.util.UUID.randomUUID().toString());
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
            Assert.assertFalse("Payload anchor with evil URL must not render", result.contains("href=\"https://evil.example\""));
            Assert.assertFalse("Payload <img> tag must not render as live HTML", result.contains("<img"));
            Assert.assertTrue("Link href must be preserved", result.contains("href=\"https://keycloak.example/link\""));
            Assert.assertTrue("Link text must be preserved", result.contains("Link account"));
            Assert.assertTrue("Link must have rel attribute for safety", result.contains("rel="));
            Assert.assertFalse("Link must not contain onclick", result.contains("onclick"));
        }
    }

    @Test
    public void testIdentityProviderLinkSentinelCollisionPrevention() throws Exception {
        String sinkLine = getMarkerExpression("base", "email/html/identity-provider-link.ftl", "identityProviderLinkBodyHtml");
        Template template = new Template("identity-provider-link", sinkLine, cfg);

        String testUUID = java.util.UUID.randomUUID().toString();
        String fullMarker = "__KC_SENTINEL_" + testUUID + "__";

        Map<String, Object> model = new HashMap<>();
        Map<String, Object> idpCtx = new HashMap<>();
        // Include the complete marker in the IdP username payload as requested by Copilot
        idpCtx.put("username", "user_" + fullMarker + "_<a href=\"https://evil.example\">Click</a>");

        model.put("kcSanitize", kcSanitize);
        model.put("msg", msg);
        model.put("identityProviderUsernameSentinel", testUUID);
        model.put("identityProviderDisplayName", "GitHub_IdP");
        model.put("realmName", "master_realm");
        model.put("identityProviderContext", idpCtx);
        model.put("link", "https://keycloak.example/link");
        model.put("linkExpiration", "5");
        model.put("linkExpirationFormatter", new TemplateMethodModelEx() {
            @Override
            public Object exec(List arguments) throws TemplateModelException {
                return "5 minutes";
            }
        });

        StringWriter writer = new StringWriter();
        template.process(model, writer);
        String result = writer.toString();

        Assert.assertTrue("Username containing full marker must survive replacement intact", result.contains("user_" + fullMarker));
        Assert.assertFalse("Malicious anchor href in username must not be rendered as live HTML", result.contains("href=\"https://evil.example\""));
        Assert.assertTrue("Escaped username text must be rendered safely", result.contains("&lt;a href="));
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
                "user{0}with{1}tokens",
                "<a href=\"https://evil.example\">Click here</a>"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = new HashMap<>();
            model.put("kcSanitize", kcSanitize);
            model.put("msg", msg);
            model.put("messageHeader", "Confirm linking account");
            model.put("messageHeaderKey", "confirmAccountLinking");
            model.put("messageHeaderUsername", payload);
            model.put("messageHeaderAlias", "corp{0}");
            model.put("messageHeaderSentinel", java.util.UUID.randomUUID().toString());

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Payload <script> tag must not render as live HTML in info header", result.contains("<script>"));
            Assert.assertFalse("Payload anchor with evil URL must not render in info header", result.contains("href=\"https://evil.example\""));
            Assert.assertFalse("Payload <img> tag must not render as live HTML in info header", result.contains("<img"));

            if ("user{0}with{1}tokens".equals(payload)) {
                Assert.assertTrue("Token-containing username must not be mutated", result.contains("user{0}with{1}tokens"));
                Assert.assertFalse("Replacement order regression: username must not be corrupted", result.contains("usergithubwith"));
            }
        }
    }

    @Test
    public void testInfoFtlSentinelCollisionPrevention() throws Exception {
        String sinkLine = getMarkerExpression("base", "login/info.ftl", "messageHeaderUsername");
        Template template = new Template("info-header-sentinel", sinkLine, cfg);

        String testUUID = java.util.UUID.randomUUID().toString();
        String aliasSentinelMarker = "__KC_SENTINEL1_" + testUUID + "__";

        Map<String, Object> model = new HashMap<>();
        model.put("kcSanitize", kcSanitize);
        model.put("msg", msg);
        model.put("messageHeaderKey", "confirmAccountLinking");
        model.put("messageHeaderUsername", "user_" + aliasSentinelMarker + "_<script>alert(1)</script>");
        model.put("messageHeaderAlias", "my_idp_alias");
        model.put("messageHeaderSentinel", testUUID);

        StringWriter writer = new StringWriter();
        template.process(model, writer);
        String result = writer.toString();

        Assert.assertTrue("Username containing full alias sentinel marker must survive intact without being overwritten by alias",
                result.contains("user_" + aliasSentinelMarker));
        Assert.assertFalse("Malicious script in username must not be rendered as live HTML", result.contains("<script>"));
    }

    @Test
    public void testInfoFtlMessageBodySanitization() throws Exception {
        String sinkLine = getMarkerExpression("base", "login/info.ftl", "class=\"instruction\"");
        Template template = new Template("info-body", sinkLine, cfg);

        String[] testPayloads = new String[] {
                "Account linked with <script>alert(1)</script>",
                "Account linked with <a href=\"https://evil.example\">Click</a>",
                "Account linked with <img src=x onerror=alert(1)>",
                "Normal account linking message"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("summary", payload);
            model.put("kcSanitize", kcSanitize);
            model.put("message", message);

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Body <script> tag must not render as live HTML", result.contains("<script>"));
            Assert.assertFalse("Body anchor tag must not render as live HTML", result.contains("<a href"));
            Assert.assertFalse("Body <img> tag must not render as live HTML", result.contains("<img"));
        }
    }

    @Test
    public void testFullInfoFtlTemplateRendering() throws Exception {
        Template template = loadFullTemplate("base", "login/info.ftl");

        String[] testPayloads = new String[] {
                "NormalUser",
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>John",
                "<a href=\"https://evil.example\">Click here</a>"
        };

        for (String payload : testPayloads) {
            Map<String, Object> model = createFullModel();
            model.put("messageHeader", "Confirm linking account");
            model.put("messageHeaderKey", "confirmAccountLinking");
            model.put("messageHeaderUsername", payload);
            model.put("messageHeaderAlias", "corp{0}");
            model.put("messageHeaderSentinel", java.util.UUID.randomUUID().toString());

            Map<String, Object> message = new HashMap<>();
            message.put("summary", "Summary payload: " + payload);
            model.put("message", message);

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Full info.ftl must not render live <script> tag", result.contains("<script>"));
            Assert.assertFalse("Full info.ftl must not render live <a href=\"https://evil.example\"", result.contains("href=\"https://evil.example\""));
            Assert.assertFalse("Full info.ftl must not render live <img> tag", result.contains("<img"));
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
                "user{0}with{1}tokens",
                "<a href=\"https://evil.example\">Click here</a>"
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
            model.put("nestedIdpSentinel", java.util.UUID.randomUUID().toString());

            Map<String, Object> properties = new HashMap<>();
            properties.put("kcAlertTitleClass", "alert-title");
            model.put("properties", properties);

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Payload <script> tag must not render as live HTML in summary", result.contains("<script>"));
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
        String sinkLine = getMarkerExpression("keycloak.v2", "login/template.ftl", "nestedIdpUsername");
        Template template = new Template("v2-template-summary", sinkLine, cfg);

        String[] testPayloads = new String[] {
                "NormalUser",
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>John",
                "User \"Test\" & Admin",
                "user{0}with{1}tokens",
                "<a href=\"https://evil.example\">Click here</a>"
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
            model.put("nestedIdpSentinel", java.util.UUID.randomUUID().toString());

            Map<String, Object> properties = new HashMap<>();
            properties.put("kcAlertTitleClass", "alert-title");
            model.put("properties", properties);

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String result = writer.toString();

            Assert.assertFalse("Payload <script> tag must not render as live HTML in V2 summary", result.contains("<script>"));
            Assert.assertFalse("Payload anchor with evil URL must not render in V2 summary", result.contains("href=\"https://evil.example\""));
            Assert.assertFalse("Payload <img> tag must not render as live HTML in V2 summary", result.contains("<img"));

            if ("user{0}with{1}tokens".equals(payload)) {
                Assert.assertTrue("Token-containing username must not be mutated", result.contains("user{0}with{1}tokens"));
            }
        }
    }

    @Test
    public void testTemplateFtlNonNestedSummarySanitization() throws Exception {
        String sinkLine = getMarkerExpression("base", "login/template.ftl", "message.summary");
        Template template = new Template("template-non-nested-summary", sinkLine, cfg);

        Map<String, Object> model = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("summary", "Message with <script>alert(1)</script> and <a href=\"https://example.com\">Link</a>");
        model.put("kcSanitize", kcSanitize);
        model.put("message", message);

        Map<String, Object> properties = new HashMap<>();
        properties.put("kcAlertTitleClass", "alert-title");
        model.put("properties", properties);

        StringWriter writer = new StringWriter();
        template.process(model, writer);
        String result = writer.toString();

        Assert.assertFalse("Non-nested summary <script> tag must be stripped by kcSanitize", result.contains("<script>"));
        Assert.assertTrue("Non-nested summary safe link must be preserved with rel=nofollow by kcSanitize", result.contains("href=\"https://example.com\"") && result.contains("rel=\"nofollow\""));
    }
}
