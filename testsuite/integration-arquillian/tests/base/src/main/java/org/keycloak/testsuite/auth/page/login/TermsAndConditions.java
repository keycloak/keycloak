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
package org.keycloak.testsuite.auth.page.login;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TermsAndConditions extends RequiredActions {

    @FindBy(id = "kc-accept")
    private WebElement acceptButton;

    @FindBy(id = "kc-decline")
    private WebElement declineButton;

    @FindBy(id = "kc-terms-text")
    private WebElement textElem;

    @Override
    public String getActionId() {
        return "terms_and_conditions";
    }

    public void acceptTerms() {
        clickLink(acceptButton);
    }
    public void declineTerms() {
        clickLink(declineButton);
    }

    public String getAcceptButtonText() {
        return acceptButton.getAttribute("value");
    }
    
    public String getDeclineButtonText() {
        return declineButton.getAttribute("value");
    }
    
    public String getText() {
        return getTextFromElement(textElem);
    }
    
}
