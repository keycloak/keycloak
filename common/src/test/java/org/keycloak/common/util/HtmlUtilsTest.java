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
}
