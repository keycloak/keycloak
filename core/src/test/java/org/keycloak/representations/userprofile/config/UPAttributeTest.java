/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.userprofile.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class UPAttributeTest {

    @Test
    public void testConvertersKeepDeclarationOrder() {
        UPAttribute attribute = new UPAttribute("foo");
        List<String> ids = Arrays.asList("z", "a", "m", "b", "y", "c", "x", "d", "w", "e", "v", "f", "u", "g", "t", "h", "s", "i");

        for (String id : ids) {
            attribute.addConverter(id, Collections.emptyMap());
        }

        Assert.assertEquals(ids, new ArrayList<>(attribute.getConverters().keySet()));
        Assert.assertEquals(ids, new ArrayList<>(attribute.clone().getConverters().keySet()));
    }

    @Test
    public void testConverterOrderIsPartOfEquality() {
        UPAttribute first = new UPAttribute("foo");
        first.addConverter("a", Collections.emptyMap());
        first.addConverter("b", Collections.emptyMap());

        UPAttribute same = new UPAttribute("foo");
        same.addConverter("a", Collections.emptyMap());
        same.addConverter("b", Collections.emptyMap());

        UPAttribute reordered = new UPAttribute("foo");
        reordered.addConverter("b", Collections.emptyMap());
        reordered.addConverter("a", Collections.emptyMap());

        Assert.assertEquals(first, same);
        Assert.assertEquals(first, first.clone());
        Assert.assertNotEquals(first, reordered);
    }
}
