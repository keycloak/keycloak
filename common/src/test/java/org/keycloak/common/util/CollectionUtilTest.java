package org.keycloak.common.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

  @Test
  public void testEmptyCollection() {
    List<String> list = new ArrayList<>();

    assertThat(CollectionUtil.isEmpty(list), is(true));
    assertThat(CollectionUtil.isNotEmpty(list), is(false));

    list.add("something");

    assertThat(CollectionUtil.isEmpty(list), is(false));
    assertThat(CollectionUtil.isNotEmpty(list), is(true));

    Set<Object> set = new HashSet<>();

    assertThat(CollectionUtil.isEmpty(set), is(true));
    assertThat(CollectionUtil.isNotEmpty(set), is(false));

    set.add("something");

    assertThat(CollectionUtil.isEmpty(set), is(false));
    assertThat(CollectionUtil.isNotEmpty(set), is(true));
  }
}
