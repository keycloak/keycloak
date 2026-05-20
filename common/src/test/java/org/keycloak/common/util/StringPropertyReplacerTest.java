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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class StringPropertyReplacerTest {

    @Test
    public void testSystemProperties() throws NoSuchAlgorithmException {
        System.setProperty("prop1", "val1");
        Assertions.assertEquals("foo-val1", replaceProperties("foo-${prop1}"));
        // non-matching scenarios
        Assertions.assertEquals("foo-${prop1", replaceProperties("foo-${prop1"));
        Assertions.assertEquals("foo-$prop1${", replaceProperties("foo-$prop1${"));

        Assertions.assertEquals("foo-def", replaceProperties("foo-${prop2:def}"));
        System.setProperty("prop2", "val2");
        Assertions.assertEquals("foo-val2", replaceProperties("foo-${prop2:def}"));

        // nesting curly braces
        Assertions.assertEquals("This is a default text with a {0} substitute", replaceProperties("${prop3:This is a default text with a {0} substitute}"));
        // stops at first unmatched, then the rest is regular text - matches the original implementation
        Assertions.assertEquals("This is a default text with a {0} substitute}", replaceProperties("${prop3:This is a default text with a {0}} substitute}"));

        // It looks for the property "prop3", then fallback to "prop4", then fallback to "prop5" and finally default value.
        // This syntax is supported by Quarkus (and underlying Microprofile)
        Assertions.assertEquals("foo-def", replaceProperties("foo-${prop3:${prop4:${prop5:def}}}"));
        System.setProperty("prop5", "val5");
        Assertions.assertEquals("foo-val5", replaceProperties("foo-${prop3:${prop4:${prop5:def}}}"));
        System.setProperty("prop4", "val4");
        Assertions.assertEquals("foo-val4", replaceProperties("foo-${prop3:${prop4:${prop5:def}}}"));
        System.setProperty("prop3", "val3");
        Assertions.assertEquals("foo-val3", replaceProperties("foo-${prop3:${prop4:${prop5:def}}}"));

        // It looks for the property "prop6", then fallback to "prop7" then fallback to value "def" .
        // This syntax is not supported by Quarkus (microprofile), however Wildfly probably supports this
        Assertions.assertEquals("foo-def", replaceProperties("foo-${prop6,prop7:def}"));
        System.setProperty("prop7", "val7");
        Assertions.assertEquals("foo-val7", replaceProperties("foo-${prop6,prop7:def}"));
        System.setProperty("prop6", "val6");
        Assertions.assertEquals("foo-val6", replaceProperties("foo-${prop6,prop7:def}"));
    }

    @Test
    public void testStackOverflow() {
        System.setProperty("prop", "${prop}");
        IllegalStateException ise = Assertions.assertThrows(IllegalStateException.class, () -> replaceProperties("${prop}"));
        Assertions.assertEquals("Infinite recursion happening when replacing properties on '${prop}'", ise.getMessage());
    }

    @Test
    public void testEnvironmentVariables() throws NoSuchAlgorithmException {
        Map<String, String> env = System.getenv();

        for (Map.Entry<String, String> entry : env.entrySet()) {
            String value = entry.getValue();
            if ( !(value == null || "".equals(value)) ) {
                Assertions.assertEquals("foo-" + value, replaceProperties("foo-${env." + entry.getKey() + "}"));
                break;
            }
        }
    }

    private String replaceProperties(String key) {
        return StringPropertyReplacer.replaceProperties(key, SystemEnvProperties.UNFILTERED::getProperty);
    }
}
