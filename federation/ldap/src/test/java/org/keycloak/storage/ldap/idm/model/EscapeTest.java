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

package org.keycloak.storage.ldap.idm.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.storage.ldap.idm.query.EscapeStrategy;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EscapeTest {

    @Test
    public void testNoAsciiOnlyEscaping() throws Exception {
        String text = "Véronique* Martin(john)second\\fff//eee\u0000";
        Assert.assertEquals(EscapeStrategy.NON_ASCII_CHARS_ONLY.escape(text), "V\\c3\\a9ronique* Martin(john)second\\fff//eee\u0000");

        text = "Hi This is a test #çà";
        Assert.assertEquals(EscapeStrategy.DEFAULT.escape(text), "Hi This is a test #\\c3\\a7\\c3\\a0");
    }

    @Test
    public void testEscaping() throws Exception {
        String text = "Véronique* Martin(john)second\\fff//eee\u0000";
        Assert.assertEquals(EscapeStrategy.DEFAULT.escape(text), "V\\c3\\a9ronique\\2a Martin\\28john\\29second\\5cfff//eee\\00");

        text = "Hi This is a test #çà";
        Assert.assertEquals(EscapeStrategy.DEFAULT.escape(text), "Hi This is a test #\\c3\\a7\\c3\\a0");

    }
}
