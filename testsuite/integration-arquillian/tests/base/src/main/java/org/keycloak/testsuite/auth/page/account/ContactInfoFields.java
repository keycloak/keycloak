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

package org.keycloak.testsuite.auth.page.account;

import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class ContactInfoFields extends Form {

    @FindBy(id = "user.attributes.street")
    private WebElement streetInput;
    @FindBy(id = "user.attributes.locality")
    private WebElement localityInput;
    @FindBy(id = "user.attributes.region")
    private WebElement regionInput;
    @FindBy(id = "user.attributes.postal_code")
    private WebElement postalCodeInput;
    @FindBy(id = "user.attributes.country")
    private WebElement counryInput;

}
