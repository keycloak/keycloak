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
package org.keycloak.client.admin.cli.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class ReturnFieldsTest {

    @Test
    public void testBasic() {
        String spec = "field1,field2,field3";
        ReturnFields fspec = new ReturnFields(spec);

        StringBuilder val = new StringBuilder();
        for (String field : fspec) {
            if (val.length() > 0)
                val.append(',');
            val.append(field);
        }
        Assert.assertEquals(spec, val.toString());

        // check catching errors

        String[] specs = {
                "",
                null,
                ",",
                "field1,",
                ",field2"
        };

        for (String filter : specs) {
            try {
                fspec = new ReturnFields(filter);
                Assert.fail("Parsing of fields spec should have failed! : " + filter);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    @Test
    public void testExclude() {
        ReturnFields spec = new ReturnFields("*,-name,dog(*,-color)");

        Assert.assertTrue(spec.included("foo"));
        Assert.assertTrue(spec.included("bar"));
        Assert.assertFalse(spec.included("name"));
        Assert.assertTrue(spec.included("dog"));
        Assert.assertTrue(spec.child("dog").included("breed"));
        Assert.assertFalse(spec.child("dog").included("color"));

        Assert.assertTrue(spec.excluded("name"));
        Assert.assertFalse(spec.excluded("foo"));
        Assert.assertFalse(spec.excluded("bar"));
        Assert.assertTrue(spec.child("dog").excluded("color"));
        Assert.assertFalse(spec.child("dog").excluded("breed"));
    }

    @Test
    public void testNestedWithGlob() {
        ReturnFields spec = new ReturnFields("name,dog(*)");

        Assert.assertTrue(spec.included("name"));
        Assert.assertFalse(spec.included("tacos"));

        Assert.assertNotNull(spec.child("dog"));
        Assert.assertTrue(spec.child("dog").included("dogname"));

        Assert.assertNotNull(spec.child("cat"));
        Assert.assertFalse(spec.child("cat").included("name"));
    }

    @Test
    public void testNested() {
        String spec = "field1,field2(sub1,sub2(subsub1)),field3";
        ReturnFields fspec = new ReturnFields(spec);

        String val = traverse(fspec);
        Assert.assertEquals(spec, val.toString());


        // check catching errors

        String[] specs = {
                "(",
                ")",
                "field1,(",
                "field1,)",
                "field1,field2(",
                "field1,field2)",
                "field1,field2()",
                "field1,field2(sub1)(",
                "field1,field2(sub1))",
                "field1,field2(sub1),"
        };

        for (String filter : specs) {
            try {
                fspec = new ReturnFields(filter);
                Assert.fail("Parsing of fields spec should have failed! : " + filter);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    private String traverse(ReturnFields fspec) {
        StringBuilder buf = new StringBuilder();
        for (String field : fspec) {
            if (buf.length() > 0)
                buf.append(',');
            buf.append(field);

            ReturnFields cspec = fspec.child(field);
            if (cspec != null && cspec != ReturnFields.NONE) {
                buf.append('(');
                buf.append(traverse(cspec));
                buf.append(')');
            }
        }
        return buf.toString();
    }
}
