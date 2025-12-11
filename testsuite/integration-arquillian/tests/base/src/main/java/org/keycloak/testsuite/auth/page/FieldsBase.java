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

package org.keycloak.testsuite.auth.page;

import org.keycloak.testsuite.page.Form;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class FieldsBase extends Form {
    protected boolean hasFieldError(WebElement field) {
        return field.findElement(By.xpath("../..")).getAttribute("class").contains("has-error");
    }
}
