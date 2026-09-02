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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.drone.spi.DroneContext;
import org.jboss.arquillian.drone.spi.event.BeforeDroneInstantiated;
import org.jboss.arquillian.drone.webdriver.configuration.WebDriverConfiguration;
import org.jboss.arquillian.drone.webdriver.spi.BrowserCapabilities;
import org.jboss.arquillian.drone.webdriver.spi.BrowserCapabilitiesRegistry;
import org.jboss.logging.Logger;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class KeycloakWebDriverConfigurator {

    protected final Logger log = Logger.getLogger(KeycloakWebDriverConfigurator.class);

    @Inject
    private Instance<BrowserCapabilitiesRegistry> registryInstance;

    public void createConfiguration(@Observes BeforeDroneInstantiated event, DroneContext droneContext) {
        WebDriverConfiguration webDriverCfg = droneContext.get(event.getDronePoint()).getConfigurationAs(WebDriverConfiguration.class);

        DesiredCapabilities capabilitiesToAdd = new DesiredCapabilities();
        updateCapabilityKeys("htmlUnit", webDriverCfg, capabilitiesToAdd);
        acceptAllSSLCerts(webDriverCfg, capabilitiesToAdd);

        BrowserCapabilities browserCap = registryInstance.get().getEntryFor(webDriverCfg.getBrowser());
        webDriverCfg.setBrowserInternal(new KcBrowserCapabilities(capabilitiesToAdd, browserCap));
    }

    private void acceptAllSSLCerts(WebDriverConfiguration webDriverCfg, DesiredCapabilities capabilitiesToAdd) {
        capabilitiesToAdd.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
    }

    // This is to ensure that default value of capabilities like "version" will be used just for the HtmlUnitDriver, but not for other drivers.
    // Hence in configs we have "htmlUnit.version" instead of "version"
    private void updateCapabilityKeys(String browser, WebDriverConfiguration webDriverCfg, DesiredCapabilities capabilitiesToAdd, String... exclude) {
        if (!webDriverCfg.getBrowser().toLowerCase().equals(browser.toLowerCase())) {
            return;
        }

        List excludeList = Arrays.asList(exclude);

        String key = browser + ".";
        int keyLength = key.length();
        for (Map.Entry<String, ?> capability : webDriverCfg.getCapabilities().asMap().entrySet()) {
            if (!excludeList.contains(capability.getKey()) && capability.getKey().startsWith(key)) {
                capabilitiesToAdd.setCapability(capability.getKey().substring(keyLength), capability.getValue());
            }
        }
    }

    public static class KcBrowserCapabilities implements BrowserCapabilities {
        private Capabilities capabilitiesToAdd;
        private BrowserCapabilities origBrowserCapabilities;

        public KcBrowserCapabilities(Capabilities capabilitiesToAdd, BrowserCapabilities origBrowserCapabilities) {
            this.capabilitiesToAdd = capabilitiesToAdd;
            this.origBrowserCapabilities = origBrowserCapabilities;
        }

        @Override
        public String getImplementationClassName() {
            return origBrowserCapabilities.getImplementationClassName();
        }

        @Override
        public Map<String, ?> getRawCapabilities() {
            Map<String, Object> ret = new HashMap<>(origBrowserCapabilities.getRawCapabilities());
            ret.putAll(capabilitiesToAdd.asMap());
            return ret;
        }

        @Override
        public String getReadableName() {
            return origBrowserCapabilities.getReadableName();
        }

        @Override
        public int getPrecedence() {
            return origBrowserCapabilities.getPrecedence();
        }
    }
}
