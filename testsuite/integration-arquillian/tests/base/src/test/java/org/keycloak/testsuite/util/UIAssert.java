/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import org.openqa.selenium.WebElement;

import static org.keycloak.testsuite.util.UIUtils.ARIA_INVALID_ATTR_NAME;
import static org.keycloak.testsuite.util.UIUtils.isElementDisabled;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;
import static org.keycloak.testsuite.util.UIUtils.isInputElementValid;

import static org.junit.Assert.assertEquals;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class UIAssert {
    public static void assertInputElementValid(boolean expected, WebElement element) {
        String msg = String.format("Expected %s to be %b; actual %s",
                ARIA_INVALID_ATTR_NAME, expected, element.getAttribute(ARIA_INVALID_ATTR_NAME));
        assertEquals(msg, expected, isInputElementValid(element));
    }

    public static void assertElementDisabled(boolean expected, WebElement element) {
        boolean actual = isElementDisabled(element);
        String msg = "Element should" + (!expected ? " not" : "") + " be disabled";
        assertEquals(msg, expected, actual);
    }

    public static void assertElementVisible(boolean expected, WebElement element) {
        boolean actual = isElementVisible(element);
        String msg = "Element should" + (!expected ? " not" : "") + " be visible";
        assertEquals(msg, expected, actual);
    }
}
