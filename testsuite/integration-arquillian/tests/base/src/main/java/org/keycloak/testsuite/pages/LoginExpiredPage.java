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

package org.keycloak.testsuite.pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LoginExpiredPage extends AbstractPage {

    @FindBy(id = "loginRestartLink")
    private WebElement loginRestartLink;

    @FindBy(id = "loginContinueLink")
    private WebElement loginContinueLink;


    public void clickLoginRestartLink() {
        clickLink(loginRestartLink);
    }

    public void clickLoginContinueLink() {
        clickLink(loginContinueLink);
    }


    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).equals("Page has expired");
    }

    public void open() {
        throw new UnsupportedOperationException();
    }
}
