package org.keycloak.common.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class CollectionUtilTest {

  @Test
  public void joinInputNoneOutputEmpty() {
    final ArrayList<String> strings = new ArrayList<>();
    final String retval = CollectionUtil.join(strings, ",");
    Assert.assertEquals("", retval);
  }

  @Test
  public void joinInput2SeparatorNull() {
    final ArrayList<String> strings = new ArrayList<>();
    strings.add("foo");
    strings.add("bar");
    final String retval = CollectionUtil.join(strings, null);
    Assert.assertEquals("foonullbar", retval);
  }

  @Test
  public void joinInput1SeparatorNotNull() {
    final ArrayList<String> strings = new ArrayList<>();
    strings.add("foo");
    final String retval = CollectionUtil.join(strings, ",");
    Assert.assertEquals("foo", retval);
  }

  @Test
  public void joinInput2SeparatorNotNull() {
    final ArrayList<String> strings = new ArrayList<>();
    strings.add("foo");
    strings.add("bar");
    final String retval = CollectionUtil.join(strings, ",");
    Assert.assertEquals("foo,bar", retval);
  }
}
