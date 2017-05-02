/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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
package org.keycloak.testsuite.console;

import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;

import static org.junit.Assert.assertEquals;

public class BasicConsoleTest extends AbstractConsoleTest {

    @Test
    // KEYCLOAK-4717
    public void testPostWindowMessage() throws InterruptedException {
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        executor.executeScript("window.check = 'check';");
        Object result = executor.executeScript("return window.check;");

        executor.executeScript("window.postMessage('hello', 'http://localhost:8180');");
        Thread.sleep(1000);
        result = executor.executeScript("return window.check;");
        assertEquals("Expected window not to have reloaded", "check", result);
    }

}
