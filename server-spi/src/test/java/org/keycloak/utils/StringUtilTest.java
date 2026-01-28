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

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class StringUtilTest {

    @Test
    public void testSanitize() {
        Assert.assertEquals("test1 test2 test3", StringUtil.sanitizeSpacesAndQuotes("test1 test2 test3", null));
        Assert.assertEquals("test1 test2 test3", StringUtil.sanitizeSpacesAndQuotes("test1\ntest2\ttest3", null));
        Assert.assertEquals("test1 test2 test3 \"test4\"", StringUtil.sanitizeSpacesAndQuotes("test1\ntest2\ttest3\r\"test4\"", null));
        Assert.assertEquals("teswith\\\"quotes", StringUtil.sanitizeSpacesAndQuotes("teswith\"quotes", '"'));
        Assert.assertEquals("test1 test2 test3 \\\"test4\\\"", StringUtil.sanitizeSpacesAndQuotes("test1\ntest2\ttest3\r\"test4\"", '"'));
        Assert.assertEquals(" \\\"test", StringUtil.sanitizeSpacesAndQuotes("\n\"test", '"'));
        Assert.assertEquals("\\\" test", StringUtil.sanitizeSpacesAndQuotes("\"\rtest", '"'));
    }
}
