package org.keycloak.common.util;

import org.junit.Assert;
import org.junit.Test;

public class HtmlUtilsTest {

  @Test
  public void escapeAttribute() {
    Assert.assertEquals("1&lt;2", HtmlUtils.escapeAttribute("1<2"));
    Assert.assertEquals("2&lt;3&amp;&amp;3&gt;2", HtmlUtils.escapeAttribute("2<3&&3>2") );
    Assert.assertEquals("test", HtmlUtils.escapeAttribute("test"));
    Assert.assertEquals("&apos;test&apos;", HtmlUtils.escapeAttribute("\'test\'"));
    Assert.assertEquals("&quot;test&quot;", HtmlUtils.escapeAttribute("\"test\""));
  }
}
