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
package org.keycloak.testsuite.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.graphene.wait.ElementBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public final class WaitUtils {

    public static final String PAGELOAD_TIMEOUT_PROP = "pageload.timeout";

    public static final Integer PAGELOAD_TIMEOUT = Integer.parseInt(System.getProperty(PAGELOAD_TIMEOUT_PROP, "60000"));

    public static ElementBuilder<Void> waitUntilElement(By by) {
        return waitGui().until().element(by);
    }

    public static ElementBuilder<Void> waitUntilElement(WebElement element) {
        return waitGui().until().element(element);
    }

    public static ElementBuilder<Void> waitUntilElement(WebElement element, String failMessage) {
        return waitGui().until(failMessage).element(element);
    }

    public static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Logger.getLogger(WaitUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
