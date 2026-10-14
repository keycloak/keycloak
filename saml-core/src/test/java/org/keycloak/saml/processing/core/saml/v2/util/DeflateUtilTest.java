/*
 * JBoss, Home of Professional Open Source
 * Copyright 2026 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.saml.processing.core.saml.v2.util;

import java.io.IOException;

import org.keycloak.saml.processing.api.util.DeflateUtil;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
public class DeflateUtilTest {

    @Test
    public void test() throws IOException {
        // Use much higher number and run the test manually to see real leaks.
        for (int i = 0; i < 10; ++i) {
            funcTest();
        }
    }

    private void funcTest() throws IOException {
        String msgString = "String To Encode";
        byte[] msg = msgString.getBytes();
        byte[] encoded = DeflateUtil.encode(msg);
        String decoded = new String(DeflateUtil.decode(encoded).readAllBytes());
        Assert.assertEquals(msgString, decoded);
    }

}
