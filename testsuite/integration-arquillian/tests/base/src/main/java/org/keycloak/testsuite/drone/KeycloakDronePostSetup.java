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

import java.util.concurrent.TimeUnit;

import io.appium.java_client.AppiumDriver;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.drone.spi.DroneContext;
import org.jboss.arquillian.drone.spi.DronePoint;
import org.jboss.arquillian.drone.spi.event.AfterDroneEnhanced;
import org.jboss.arquillian.graphene.proxy.GrapheneProxyInstance;
import org.jboss.arquillian.graphene.proxy.Interceptor;
import org.jboss.arquillian.graphene.proxy.InvocationContext;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakDronePostSetup {

    protected static final Logger log = org.jboss.logging.Logger.getLogger(KeycloakDronePostSetup.class);


    public void configureWebDriver(@Observes AfterDroneEnhanced event, DroneContext droneContext) {
        DronePoint<?> dronePoint = event.getDronePoint();
        Object drone = droneContext.get(dronePoint).getInstance();

        if (drone instanceof RemoteWebDriver) {
            RemoteWebDriver remoteWebDriver = (RemoteWebDriver) drone;
            log.infof("Detected browser: %s %s", remoteWebDriver.getCapabilities().getBrowserName(), remoteWebDriver.getCapabilities().getVersion());
        }


        if (drone instanceof WebDriver && !(drone instanceof AppiumDriver)) {
            WebDriver webDriver = (WebDriver) drone;
            configureDriverSettings(webDriver);
        } else {
            log.warn("Drone is not instanceof WebDriver for a desktop browser! Drone is " + drone);
        }

        if (drone instanceof GrapheneProxyInstance) {
            GrapheneProxyInstance droneProxy = (GrapheneProxyInstance) drone;
            if (drone instanceof HtmlUnitDriver) {
                droneProxy.registerInterceptor(new HtmlUnitInterceptor());
            }
        } else {
            log.warn("Drone is not instanceof GrapheneProxyInstance! Drone is " + drone);
        }
    }


    private void configureDriverSettings(WebDriver driver) {
        long implicitWaitMillis = WaitUtils.IMPLICIT_ELEMENT_WAIT_MILLIS;
        long pageLoadTimeoutMillis = WaitUtils.PAGELOAD_TIMEOUT_MILLIS;
        log.infof("Configuring driver settings. implicitWait=%d, pageLoadTimeout=%d", implicitWaitMillis, pageLoadTimeoutMillis);

        driver.manage().timeouts().implicitlyWait(implicitWaitMillis, TimeUnit.MILLISECONDS);
        driver.manage().timeouts().pageLoadTimeout(pageLoadTimeoutMillis, TimeUnit.MILLISECONDS);
        driver.manage().window().maximize();
    }


    public static class HtmlUnitInterceptor implements Interceptor {

        @Override
        public Object intercept(InvocationContext context) throws Throwable {
            if (context.getMethod().getName().equals("executeScript")) {

                String currentUrl = ((WebDriver) context.getTarget()).getCurrentUrl();
                int refreshCount = 0;

                while (true) {
                    try {
                        // htmlUnit is not able to run javascript on about:blank page
                        if ("about:blank".equals(currentUrl)) {
                            log.debug("Ignored JS as we are on about:blank page now");
                            return null;
                        }

                        return context.invoke();
                    } catch (UnsupportedOperationException e) {

                        // htmlUnit may require to refresh the page after the action
                        if ("Cannot execute JS against a plain text page".equals(e.getMessage())) {
                            refreshCount += 1;
                            if (refreshCount < 2) {
                                log.debugf("Will try to refresh current page: %s", currentUrl);
                                ((WebDriver) context.getProxy()).navigate().to(currentUrl);
                            } else {
                                log.debugf("Current page doesn't seem to support javascript. Current url: %s", currentUrl);
                                return null;
                            }
                        } else {
                            throw e;
                        }
                    }
                }


            } else {
                return context.invoke();
            }
        }

        @Override
        public int getPrecedence() {
            return -1;
        }
    }

}
