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
package org.keycloak.testsuite.console.page.fragment;

import org.keycloak.testsuite.page.AbstractAlert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class AdminConsoleAlert extends AbstractAlert {

    @FindBy(xpath = ".//button[@class='close']")
    protected WebElement closeButton;

    public boolean isInfo() {
        return checkAlertType("info");
    }

    public boolean isWarning() {
        return checkAlertType("waring");
    }

    public boolean isDanger() {
        return checkAlertType("danger");
    }

    public void close() {
        closeButton.click();
    }

}
