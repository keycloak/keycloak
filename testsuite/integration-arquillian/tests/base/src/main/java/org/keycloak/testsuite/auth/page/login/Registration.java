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

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.AccountFields;
import org.keycloak.testsuite.auth.page.PasswordFields;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

import static org.keycloak.testsuite.admin.Users.getPasswordOf;
import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * @author Filip Kiss
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class Registration extends LoginActions {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("registration");
    }

    @Page
    private AccountFields accountFields;

    @Page
    private PasswordFields passwordFields;

    @FindBy(xpath = "//a[contains(., 'Back to Login')]")
    private WebElement backToLoginLink;

    public void register(UserRepresentation user) {
        setValues(user);
        submit();
    }
    
    public void setValues(UserRepresentation user) {
        setValues(user, getPasswordOf(user));
    }

    public void setValues(UserRepresentation user, String confirmPassword) {
        accountFields.setValues(user);
        passwordFields.setPassword(getPasswordOf(user));
        passwordFields.setConfirmPassword(confirmPassword);
    }

    public boolean isUsernamePresent() {
        return accountFields.isUsernamePresent();
    }
    
    public boolean isConfirmPasswordPresent() {
        return passwordFields.isConfirmPasswordPresent();
    }

    public AccountFields accountFields() {
        return accountFields;
    }

    public PasswordFields passwordFields() {
        return passwordFields;
    }

    public void backToLogin() {
        clickLink(backToLoginLink);
    }
}
