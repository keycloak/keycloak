/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.DroneUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page shown during processing of the PushButtonAuthenticator
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PushTheButtonPage extends AbstractPage {

    @FindBy(name = "submit1")
    private WebElement submitButton;

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCurrent() {
        return DroneUtils.getCurrentDriver().getTitle().equals("PushTheButton")
                && !driver.findElements(By.name("submit1")).isEmpty();
    }

    public void submit() {
        submitButton.click();
    }
}
