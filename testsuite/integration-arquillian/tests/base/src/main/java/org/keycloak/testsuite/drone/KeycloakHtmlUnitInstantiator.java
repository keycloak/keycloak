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

package org.keycloak.testsuite.drone;

import java.lang.reflect.Method;

import com.gargoylesoftware.htmlunit.WebClient;
import org.jboss.arquillian.drone.spi.Instantiator;
import org.jboss.arquillian.drone.webdriver.configuration.WebDriverConfiguration;
import org.jboss.arquillian.drone.webdriver.factory.HtmlUnitDriverFactory;
import org.jboss.logging.Logger;
import org.keycloak.common.util.reflections.Reflections;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakHtmlUnitInstantiator extends HtmlUnitDriverFactory implements Instantiator<HtmlUnitDriver, WebDriverConfiguration> {

    protected final Logger log = Logger.getLogger(KeycloakHtmlUnitInstantiator.class);

    @Override
    public int getPrecedence() {
        return 1;
    }


    @Override
    public HtmlUnitDriver createInstance(WebDriverConfiguration configuration) {
        HtmlUnitDriver htmlUnitDriver = super.createInstance(configuration);

        // Disable CSS
        Method getWebClient = Reflections.findDeclaredMethod(HtmlUnitDriver.class, "getWebClient");
        WebClient webClient = (WebClient) Reflections.invokeMethod(true, getWebClient, htmlUnitDriver);
        webClient.getOptions().setCssEnabled(false);

        return htmlUnitDriver;
    }

}
