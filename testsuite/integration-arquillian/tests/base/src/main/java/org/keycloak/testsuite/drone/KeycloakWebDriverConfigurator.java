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

import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.drone.spi.Configurator;
import org.jboss.arquillian.drone.spi.DronePoint;
import org.jboss.arquillian.drone.webdriver.configuration.WebDriverConfiguration;
import org.jboss.arquillian.drone.webdriver.factory.BrowserCapabilitiesList;
import org.jboss.arquillian.drone.webdriver.factory.WebDriverFactory;
import org.jboss.logging.Logger;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakWebDriverConfigurator extends WebDriverFactory implements Configurator<WebDriver, WebDriverConfiguration> {

    protected final Logger log = Logger.getLogger(KeycloakWebDriverConfigurator.class);

    @Override
    public int getPrecedence() {
        return 1;
    }

    @Override
    public WebDriverConfiguration createConfiguration(ArquillianDescriptor descriptor, DronePoint<WebDriver> dronePoint) {
        WebDriverConfiguration webDriverCfg = super.createConfiguration(descriptor, dronePoint);

        if (webDriverCfg.getBrowser().equals("htmlUnit")) {
            updateCapabilities(webDriverCfg);
        }

        return webDriverCfg;
    }


    // This is to ensure that default value of capabilities like "version" will be used just for the HtmlUnitDriver, but not for other drivers.
    // Hence in configs we have "htmlUnit.version" instead of "version"
    protected void updateCapabilities(WebDriverConfiguration configuration) {
        Map<String, Object> newCapabilities = new HashMap<>();

        for (Map.Entry<String, ?> capability : configuration.getCapabilities().asMap().entrySet()) {
            if (capability.getKey().startsWith("htmlUnit.")) {
                newCapabilities.put(capability.getKey().substring(9), capability.getValue());
            }
        }

        log.debug("Adding new capabilities for HtmlUnitDriver: " + newCapabilities);

        KcHtmlUnitCapabilities mergedBrowser = new KcHtmlUnitCapabilities(newCapabilities);
        configuration.setBrowserInternal(mergedBrowser);
    }


    private static class KcHtmlUnitCapabilities extends BrowserCapabilitiesList.HtmlUnit {

        private final Map<String, Object> newCapabilities;

        public KcHtmlUnitCapabilities(Map<String, Object> newCapabilities) {
            this.newCapabilities = newCapabilities;
        }

        @Override
        public Map<String, ?> getRawCapabilities() {
            Map<String, ?> parent = super.getRawCapabilities();

            Map<String, Object> merged = new HashMap<>(parent);
            merged.putAll(newCapabilities);

            return merged;
        }

    }
}
