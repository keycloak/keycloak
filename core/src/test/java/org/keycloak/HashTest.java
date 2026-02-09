/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.keycloak.jose.jws.crypto.HashUtils;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HashTest {

    @Test
    public void testSha256Hash() throws Exception {
        testSha256Hash("myCodeVerifier", StandardCharsets.ISO_8859_1, "rxrlnYFwTggx1TzJeKXupM_TAJia_vbHD35PIlq9tRg");
        testSha256Hash("myCodeVerifier", StandardCharsets.UTF_8, "rxrlnYFwTggx1TzJeKXupM_TAJia_vbHD35PIlq9tRg");

        testSha256Hash("Some1[^&*$#", StandardCharsets.ISO_8859_1, "lXO5GHk4DoCxiStRrpJgQ-cOnJQmJTb2gh3HJ3Ueq9U");
        testSha256Hash("Some1[^&*$#", StandardCharsets.UTF_8, "lXO5GHk4DoCxiStRrpJgQ-cOnJQmJTb2gh3HJ3Ueq9U");

        // This will differ between ISO-8851-1 and UTF8 due the special character
        testSha256Hash("krák", StandardCharsets.ISO_8859_1, "XD3Gb_rLS49onF_rOsTWc7SLa27Vny8AHgmoEOmJx5s");
        testSha256Hash("krák", StandardCharsets.UTF_8, "QKvM6HItSe5Yi0rQqQgIFhyKhQJCNr4H60eP3YgcjpU");
    }

    private void testSha256Hash(String codeVerifier, Charset charset, String expectedHash) {
        String hash1 = HashUtils.sha256UrlEncodedHash(codeVerifier, charset);
        Assert.assertEquals(hash1, expectedHash);
    }
}
