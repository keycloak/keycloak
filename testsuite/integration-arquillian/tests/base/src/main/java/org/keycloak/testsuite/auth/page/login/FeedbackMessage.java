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

package org.keycloak.testsuite.auth.page.login;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class FeedbackMessage {
    @FindBy(css = ".alert")
    private WebElement alertRoot;

    public boolean isPresent() {
        try {
            return alertRoot.isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getText() {
        return getTextFromElement(alertRoot.findElement(By.className("kc-feedback-text")));
    }

    public String getType() {
        String cssClass = alertRoot.getAttribute("class");
        Matcher classMatcher = Pattern.compile("alert-(.+)").matcher(cssClass);
        if (!classMatcher.find()) {
            throw new RuntimeException("Failed to identify feedback message type");
        }
        return classMatcher.group(1);
    }

    public boolean isSuccess() {
        return getType().equals("success");
    }

    public boolean isWarning() {
        return getType().equals("warning");
    }

    public boolean isError() {
        return getType().equals("error");
    }

    public boolean isInfo() {
        return getType().equals("info");
    }
}
