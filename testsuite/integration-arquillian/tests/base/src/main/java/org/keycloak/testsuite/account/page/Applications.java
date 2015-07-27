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
package org.keycloak.testsuite.account.page;

import java.util.List;
import javax.ws.rs.core.UriBuilder;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author Petr Mensik
 */
public class Applications extends AccountManagement {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("applications");
    }

    public static final String XPATH_APP_TABLE = "//table[./thead[//td[text()='Application']]]";

    @FindBy(xpath = XPATH_APP_TABLE)
    protected WebElement appTable;

    @FindBy(xpath = XPATH_APP_TABLE + "//a")
    protected List<WebElement> applicationLinks;
    
    public boolean containsApplication(String application) {
        boolean contains = false;
        for (WebElement appLink : applicationLinks) {
            if (appLink.getText().equals(application)) {
                contains = true;
                break;
            }
        }
        return contains;
    }
    
    public void clickApplication(String application) {
        for (WebElement appLink : applicationLinks) {
            if (appLink.getText().equals(application)) {
                appLink.click();
            }
        }
    }

}
