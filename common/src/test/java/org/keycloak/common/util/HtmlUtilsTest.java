package org.keycloak.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HtmlUtilsTest {

  @Test
  public void escapeAttribute() {
    Assertions.assertEquals("1&lt;2", HtmlUtils.escapeAttribute("1<2"));
    Assertions.assertEquals("2&lt;3&amp;&amp;3&gt;2", HtmlUtils.escapeAttribute("2<3&&3>2") );
    Assertions.assertEquals("test", HtmlUtils.escapeAttribute("test"));
    Assertions.assertEquals("&apos;test&apos;", HtmlUtils.escapeAttribute("\'test\'"));
    Assertions.assertEquals("&quot;test&quot;", HtmlUtils.escapeAttribute("\"test\""));
  }

  /**
   * Verifies that attacker-controlled IdP usernames containing HTML anchor tags
   * are escaped server-side before being embedded in message strings.
   *
   * <p>Custom themes that apply {@code kcSanitize()} permit safe-looking anchors
   * such as {@code <a href="...">} to pass through. By escaping at the Java layer,
   * the anchor tag is rendered as plain text in all theme paths.
   *
   * <p>Expected: the value MUST NOT be renderable as an active HTML link.
   */
  @Test
  public void escapeAttribute_idpUsernameXssPrevention() {
    // Attacker-controlled IdP username: an anchor that passes kcSanitize
    String maliciousUsername = "<a href=\"https://evil.example\">Click</a>";
    String escaped = HtmlUtils.escapeAttribute(maliciousUsername);

    // Must NOT contain any unescaped HTML tags
    Assertions.assertFalse(escaped.contains("<a "),
        "Escaped output must not contain an opening anchor tag");
    Assertions.assertFalse(escaped.contains("</a>"),
        "Escaped output must not contain a closing anchor tag");
    Assertions.assertFalse(escaped.contains("href="),
        "Escaped output must not contain an href attribute");

    // Must contain escaped equivalents so display still shows the raw value as plain text
    Assertions.assertTrue(escaped.contains("&lt;a "),
        "Escaped output must contain the encoded opening tag");
    Assertions.assertTrue(escaped.contains("&lt;/a&gt;"),
        "Escaped output must contain the encoded closing tag");
    Assertions.assertEquals(
        "&lt;a href=&quot;https://evil.example&quot;&gt;Click&lt;/a&gt;",
        escaped,
        "Full escaped value must match expected HTML-encoded string");
  }

  @Test
  public void escapeAttribute_idpAliasXssPrevention() {
    // Attacker-controlled IdP alias with inline event handler
    // After escaping, the angle brackets are encoded so the tag cannot execute,
    // even though the word "onerror" remains as inert plain text.
    String maliciousAlias = "<img src=x onerror=alert(1)>";
    String escaped = HtmlUtils.escapeAttribute(maliciousAlias);

    // The raw opening tag syntax must be broken
    Assertions.assertFalse(escaped.contains("<img"),
        "Escaped alias must not contain raw img opening tag");
    Assertions.assertFalse(escaped.contains(">") && escaped.contains("<"),
        "Escaped alias must not contain raw angle brackets that could form a tag");

    // The tag must be present as encoded plain text (harmless)
    Assertions.assertTrue(escaped.contains("&lt;img"),
        "Escaped alias must encode the img opening tag");
    Assertions.assertTrue(escaped.contains("&gt;"),
        "Escaped alias must encode the closing angle bracket");
    Assertions.assertEquals(
        "&lt;img src=x onerror=alert(1)&gt;",
        escaped,
        "Full escaped alias must match expected HTML-encoded string");
  }
}
