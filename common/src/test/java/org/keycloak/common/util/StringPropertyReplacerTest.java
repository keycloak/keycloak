/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.common.util;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class StringPropertyReplacerTest {

    @Test
    public void testSystemProperties() throws NoSuchAlgorithmException {
        System.setProperty("prop1", "val1");
        Assert.assertEquals("foo-val1", replaceProperties("foo-${prop1}"));
        // non-matching scenarios
        Assert.assertEquals("foo-${prop1", replaceProperties("foo-${prop1"));
        Assert.assertEquals("foo-$prop1${", replaceProperties("foo-$prop1${"));

        Assert.assertEquals("foo-def", replaceProperties("foo-${prop2:def}"));
        System.setProperty("prop2", "val2");
        Assert.assertEquals("foo-val2", replaceProperties("foo-${prop2:def}"));

        // It looks for the property "prop3", then fallback to "prop4", then fallback to "prop5" and finally default value.
        // This syntax is supported by Quarkus (and underlying Microprofile)
        Assert.assertEquals("foo-def", replaceProperties("foo-${prop3:${prop4:${prop5:def}}}"));
        System.setProperty("prop5", "val5");
        Assert.assertEquals("foo-val5", replaceProperties("foo-${prop3:${prop4:${prop5:def}}}"));
        System.setProperty("prop4", "val4");
        Assert.assertEquals("foo-val4", replaceProperties("foo-${prop3:${prop4:${prop5:def}}}"));
        System.setProperty("prop3", "val3");
        Assert.assertEquals("foo-val3", replaceProperties("foo-${prop3:${prop4:${prop5:def}}}"));

        // It looks for the property "prop6", then fallback to "prop7" then fallback to value "def" .
        // This syntax is not supported by Quarkus (microprofile), however Wildfly probably supports this
        Assert.assertEquals("foo-def", replaceProperties("foo-${prop6,prop7:def}"));
        System.setProperty("prop7", "val7");
        Assert.assertEquals("foo-val7", replaceProperties("foo-${prop6,prop7:def}"));
        System.setProperty("prop6", "val6");
        Assert.assertEquals("foo-val6", replaceProperties("foo-${prop6,prop7:def}"));
    }

    @Test
    public void testStackOverflow() {
        System.setProperty("prop", "${prop}");
        IllegalStateException ise = Assert.assertThrows(IllegalStateException.class, () -> replaceProperties("${prop}"));
        Assert.assertEquals("Infinite recursion happening when replacing properties on '${prop}'", ise.getMessage());
    }

    @Test
    public void testEnvironmentVariables() throws NoSuchAlgorithmException {
        Map<String, String> env = System.getenv();

        for (Map.Entry<String, String> entry : env.entrySet()) {
            String value = entry.getValue();
            if ( !(value == null || "".equals(value)) ) {
                Assert.assertEquals("foo-" + value, replaceProperties("foo-${env." + entry.getKey() + "}"));
                break;
            }
        }
    }

    private String replaceProperties(String key) {
        return StringPropertyReplacer.replaceProperties(key, SystemEnvProperties.UNFILTERED::getProperty);
    }
}
