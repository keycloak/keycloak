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

package org.keycloak.testsuite.page;

import com.google.common.base.Predicate;
import java.util.Arrays;
import static org.jboss.arquillian.graphene.Graphene.waitModel;
import org.jboss.arquillian.graphene.fragment.Root;
import org.jboss.logging.Logger;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAlert {

    protected final Logger log = Logger.getLogger(this.getClass());

    @Root
    protected WebElement root;

    public void waitUntilPresent() {
        waitUntilElement(root, "Flash message should be present.").is().present();
    }

    public void waitUntilPresentAndClassSet() {
        waitUntilPresent();
        waitModel().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                return !Arrays.asList(getAttributeClass().split(" ")).contains("alert-");
            }
        });
    }

    public String getText() {
        return root.getText();
    }

    public String getAttributeClass() {
        String attrClass = root.getAttribute("class");
        log.debug("Alert @class = '" + attrClass + "'");
        return attrClass;
    }

    public boolean isSuccess() {
        log.debug("Alert.isSuccess()");
        return getAttributeClass().contains("alert-success");
    }

}
