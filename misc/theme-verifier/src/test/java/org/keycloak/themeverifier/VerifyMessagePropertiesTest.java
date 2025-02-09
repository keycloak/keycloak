/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.themeverifier;

import org.apache.maven.plugin.MojoExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

class VerifyMessagePropertiesTest {

    @Test
    void verifyDuplicateKeysDetected() throws MojoExecutionException {
        List<String> verify = getFile("duplicate_keys.properties").verify();
        MatcherAssert.assertThat(verify, Matchers.contains(Matchers.containsString("Duplicate keys in file")));
    }

    private static VerifyMessageProperties getFile(String fixture) {
        URL resource = VerifyMessageProperties.class.getResource("/" + fixture);
        if (resource == null) {
            throw new RuntimeException("Resource not found: " + fixture);
        }
        return new VerifyMessageProperties(new File(resource.getFile()));
    }

}
