/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author hmlnarik
 */
public class TextFileCheckerTest {

    private TextFileChecker tfc;
    private File tempFile;

    private Consumer<Stream<String>> collector(Collection<String> target) {
        return (Stream<String> s) -> s.forEachOrdered(target::add);
    }

    @Before
    public void before() throws IOException {
        tempFile = File.createTempFile("TextFileCheckerTest-", ".tmp");
        tfc = new TextFileChecker(tempFile.toPath());
    }

    @After
    public void after() throws IOException {
        tempFile.delete();
    }

    @Test
    public void testFileChecker() throws Exception {
        try (FileWriter fw = new FileWriter(tempFile)) {
            assertCheckedOutputIs();

            fw.write("Hello, Dolly\n");
            fw.flush();
            assertCheckedOutputIs("Hello, Dolly");

            fw.write("Well, hello, Dolly\n");
            fw.flush();
            assertCheckedOutputIs("Hello, Dolly", "Well, hello, Dolly");

            fw.write("It's so nice to have you back where you belong\n");
            fw.write("You're lookin' swell, Dolly\n");
            fw.flush();
            assertCheckedOutputIs("Hello, Dolly", "Well, hello, Dolly", "It's so nice to have you back where you belong", "You're lookin' swell, Dolly");

            tfc.updateLastCheckedPositionsOfAllFilesToEndOfFile();

            fw.write("I can tell, Dolly\n");
            fw.write("You're still glowin', you're still crowin'\n");
            fw.flush();
            assertCheckedOutputIs("I can tell, Dolly", "You're still glowin', you're still crowin'");

            tfc.updateLastCheckedPositionsOfAllFilesToEndOfFile();
            assertCheckedOutputIs();
        }
    }

    public void assertCheckedOutputIs(String... expectedOutput) throws IOException {
        List<String> target = new LinkedList<>();
        tfc.checkFiles(false, collector(target));
        assertThat(target,
          expectedOutput == null || expectedOutput.length == 0
            ? Matchers.empty()
            : Matchers.contains(expectedOutput));
    }

}
