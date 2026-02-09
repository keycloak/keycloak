/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.config;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class OptionBuilderTest {

    @Test
    public void testDeprecatedOptionOrdering() {
        String[] values = new String[] {"a", "d", "b", "1"};
        var option = new OptionBuilder<>("key", String.class).deprecatedValues("These are deprecated", values).build();
        assertArrayEquals(option.getDeprecatedMetadata().get().getDeprecatedValues().toArray(String[]::new), values);
    }

}
