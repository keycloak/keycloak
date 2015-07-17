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

package org.keycloak.testsuite.admin.fragment;

import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.graphene.fragment.Root;

import static org.keycloak.testsuite.admin.util.SeleniumUtils.waitGuiForElement;
import org.openqa.selenium.WebElement;

/**
 *
 * @author Petr Mensik
 */
public class FlashMessage {

    @Root
    private WebElement root;

    public boolean isSuccess() {
		waitGui().until("Flash message should be success")
			.element(root)
			.attribute("class")
			.contains("success");
		return root.getAttribute("class").contains("success");
    }

    public boolean isError() {
		waitGui().until("Flash message should be error")
			.element(root)
			.attribute("class")
			.contains("error");
        return root.getAttribute("class").contains("error");
    }

    public boolean isDanger() {
		waitGui().until("Flash message should be danger")
			.element(root)
			.attribute("class")
			.contains("danger");
		return root.getAttribute("class").contains("danger"); 
	}

    public String getText() {
        return root.getText();
    }

    public void waitUntilPresent() {
		waitGuiForElement(root, "Flash message should be visible.");
    }
}
