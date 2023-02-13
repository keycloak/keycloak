/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.util;

import org.jboss.arquillian.graphene.context.GrapheneContext;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.page.AbstractPage;
import org.openqa.selenium.WebDriver;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class DroneUtils {
    private static Queue<WebDriver> driverQueue = new LinkedList<>();

    public static WebDriver getCurrentDriver() {
        if (driverQueue.isEmpty()) {
            return GrapheneContext.lastContext().getWebDriver();
        }

        return driverQueue.peek();
    }

    public static void addWebDriver(WebDriver driver) {
        driverQueue.add(driver);
    }

    public static void removeWebDriver() {
        driverQueue.poll();
    }

    public static void resetQueue() {
        driverQueue = new LinkedList<>();
    }

    /**
     * This static method can be used to change default {@ode WebDriver} to different one if the test is able to run
     * with only one {@code WebDriver}.
     *
     * {@code target} parameter is supposed be of type {@code AbstractKeycloakTest} or {@link AbstractPage}.
     *
     * @param target target class on which one needs to change {@code WebDriver}
     * @param driver the new {@code WebDriver}
     */
    public static void replaceDefaultWebDriver(Object target, WebDriver driver) {
        DroneUtils.addWebDriver(driver);
        List<Field> allFields = new ArrayList<>();
        // Add all fields of this class and superclasses
        Class<?> targetClass = target.getClass();
        while (targetClass != null) {
            allFields.addAll(Arrays.asList(targetClass.getDeclaredFields()));
            allFields.addAll(Arrays.asList(targetClass.getFields()));
            targetClass = targetClass.getSuperclass();
        }

        for (Field f : allFields) {
            if (f.getAnnotation(Page.class) != null
                    && AbstractPage.class.isAssignableFrom(f.getType())) {
                try {
                    if (!f.isAccessible())
                        f.setAccessible(true);
                    Object o = f.get(target);
                    AbstractPage page = (AbstractPage) o;
                    page.setDriver(driver);
                    replaceDefaultWebDriver(page, driver);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Could not replace the driver in " + f, e);
                }
            } else if (f.getName().equals("driver") && WebDriver.class.isAssignableFrom(f.getType())) {
                try {
                    if (!f.isAccessible())
                        f.setAccessible(true);
                    f.set(target, driver);

                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Could not replace the " + f.getName() + " in class "
                            + target.getClass().getName(), e);
                }
            } else if (f.getName().equals("oauth") && OAuthClient.class.isAssignableFrom(f.getType())) {
                try {
                    if (!f.isAccessible())
                        f.setAccessible(true);
                    Object o = f.get(target);
                    ((OAuthClient) o).setDriver(driver);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Could not replace the " + f.getName() + " in class "
                            + target.getClass().getName(), e);
                }
            }
        }
        DroneUtils.removeWebDriver();
    }

}
