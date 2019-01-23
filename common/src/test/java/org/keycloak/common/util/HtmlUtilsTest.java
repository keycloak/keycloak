package org.keycloak.common.util;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.util.HtmlUtils;

public class HtmlUtilsTest {

  @Test
  public void escapeAttribute() {
    Assert.assertEquals(HtmlUtils.escapeAttribute("1<2"), "1&lt;2");
    Assert.assertEquals(HtmlUtils.escapeAttribute("2<3&&3>2"), "2&lt;3&amp;&amp;3&gt;2");
    Assert.assertEquals(HtmlUtils.escapeAttribute("test"), "test");
    Assert.assertEquals(HtmlUtils.escapeAttribute("\'test\'"), "&apos;test&apos;");
    Assert.assertEquals(HtmlUtils.escapeAttribute("\"test\""), "&quot;test&quot;");
  }
}
