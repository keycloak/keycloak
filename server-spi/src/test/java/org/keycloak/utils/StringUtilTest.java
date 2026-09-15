/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 * @author rmartinc
 */
public class StringUtilTest {

    @Test
    public void testSanitize() {
        assertEquals("test1 test2 test3", StringUtil.sanitizeSpacesAndQuotes("test1 test2 test3", null));
        assertEquals("test1 test2 test3", StringUtil.sanitizeSpacesAndQuotes("test1\ntest2\ttest3", null));
        assertEquals("test1 test2 test3 \"test4\"", StringUtil.sanitizeSpacesAndQuotes("test1\ntest2\ttest3\r\"test4\"", null));
        assertEquals("teswith\\\"quotes", StringUtil.sanitizeSpacesAndQuotes("teswith\"quotes", '"'));
        assertEquals("test1 test2 test3 \\\"test4\\\"", StringUtil.sanitizeSpacesAndQuotes("test1\ntest2\ttest3\r\"test4\"", '"'));
        assertEquals(" \\\"test", StringUtil.sanitizeSpacesAndQuotes("\n\"test", '"'));
        assertEquals("\\\" test", StringUtil.sanitizeSpacesAndQuotes("\"\rtest", '"'));
    }

    @Test
    public void testRemoveControlCharacters() {
        assertEquals("THIS_IS_RED", StringUtil.removeControlCharacters("%1B[31mTHIS_IS_RED%1B[0m"));
        // URL-encoded characters are NOT decoded, only control chars are removed
        assertEquals("fake_client[FORGED+INFO]+User+admin+logged+in+successfully",
                StringUtil.removeControlCharacters("fake_client%0D%0A%1b[32m[FORGED+INFO]+User+admin+logged+in+successfully%1b[0m"));

        assertEquals("fake_client[FORGED INFO]",
                StringUtil.removeControlCharacters("fake_client\r\n\u001b[32m[FORGED INFO]\u001b[0m"));

        assertEquals("test", StringUtil.removeControlCharacters("te\u0001st"));
        assertEquals("test", StringUtil.removeControlCharacters("te\u007Fst"));

        assertNull(StringUtil.removeControlCharacters(null));
        assertEquals("", StringUtil.removeControlCharacters(""));

        assertEquals("normal text", StringUtil.removeControlCharacters("normal text"));
        
        assertEquals("foo%20bar", StringUtil.removeControlCharacters("foo%20bar"));
        assertEquals("foo%2Fbar", StringUtil.removeControlCharacters("foo%2Fbar"));
        assertEquals("test%2092", StringUtil.removeControlCharacters("test%2092"));
    }
}
