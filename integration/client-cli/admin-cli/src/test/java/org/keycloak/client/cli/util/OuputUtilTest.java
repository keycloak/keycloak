/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.client.cli.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OuputUtilTest {

    @Test
    public void testConversionToCsv() throws IOException {
        HashMap<Object, Object> map1 = new HashMap<>();
        map1.put("not-x", "omit");
        map1.put("y", "v1");
        HashMap<Object, Object> map2 = new HashMap<>();
        map2.put("x", "v2");
        JsonNode node = OutputUtil.convertToJsonNode(Arrays.asList(map1, map2));
        ArrayList<String> result = new ArrayList<>();
        OutputUtil.printAsCsv(node, new ReturnFields("x,y"), false, result::add);
        assertEquals(",\"v1\"", result.get(0));
        assertEquals("\"v2\",", result.get(1));
    }

}
