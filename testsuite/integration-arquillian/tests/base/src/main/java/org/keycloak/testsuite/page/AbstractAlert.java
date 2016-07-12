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

package org.keycloak.testsuite.page;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.fragment.Root;
import org.jboss.logging.Logger;

import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAlert {

    protected final Logger log = Logger.getLogger(this.getClass());

    @Root
    protected WebElement root;

    @Drone
    protected WebDriver driver;

    public String getText() {
        return root.getText();
    }

    public boolean isSuccess() {
        log.debug("Alert.isSuccess()");
        return checkAlertType("success");
    }

    protected boolean checkAlertType(String type) {
        WaitUtils.waitForPageToLoad(driver);
        try {
            (new WebDriverWait(driver, 1)).until(ExpectedConditions.attributeContains(root, "class", "alert-" + type));
        }
        catch (TimeoutException e) {
            return false;
        }
        return true;
    }

}
