/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.saml.common.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 *
 * @author hmlnarik
 */
public class StringUtilTest {

    public StringUtilTest() {
    }

    @Test
    public void testGetSystemPropertyAsString() {
        System.setProperty("StringUtilTest.prop1", "value1");
        System.setProperty("StringUtilTest.prop2", "value2");

        assertThat(StringUtil.getSystemPropertyAsString("a"), is("a"));
        assertThat(StringUtil.getSystemPropertyAsString("a ${StringUtilTest.prop1}"), is("a value1"));
        assertThat(
          StringUtil.getSystemPropertyAsString("a" + "${StringUtilTest.prop1}" + "StringUtilTest.prop1"),
          is("a" + "value1" + "StringUtilTest.prop1")
        );
        assertThat(
          StringUtil.getSystemPropertyAsString("a" + "${StringUtilTest.prop1}" + "StringUtilTest.prop1" + "${StringUtilTest.prop2}"),
          is("a" + "value1" + "StringUtilTest.prop1" + "value2")
        );
        assertThat(
          StringUtil.getSystemPropertyAsString("a" + "${StringUtilTest.prop1}" + "StringUtilTest.prop1" + "${StringUtilTest.prop2}" + "${StringUtilTest.prop3::abc}"),
          is("a" + "value1" + "StringUtilTest.prop1" + "value2" + "abc")
        );
        assertThat(
          StringUtil.getSystemPropertyAsString("a" + "${StringUtilTest.prop1}" + "StringUtilTest.prop1" + "${StringUtilTest.prop2}" + "${StringUtilTest.prop3::abc}" + "end"),
          is("a" + "value1" + "StringUtilTest.prop1" + "value2" + "abc" + "end")
        );
    }

}
