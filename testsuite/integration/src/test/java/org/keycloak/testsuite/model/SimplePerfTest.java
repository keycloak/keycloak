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

package org.keycloak.testsuite.model;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.KeyPair;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Ignore
public class SimplePerfTest {

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    public WebDriver driver;

    public static final String PORT = "8080";
    public static final int WARMUP = 1000;
    public static final int COUNT = 10000;

    @Test
    public void simplePerf() {
        // Warm-up
        for (int i = 0; i < WARMUP; i++) {
            doLoginLogout(PORT);
        }

        long start = System.currentTimeMillis();
        long s = start;

        for (int i = 0; i < COUNT; i++) {
            doLoginLogout(PORT);

            if (i % 100 == 0) {
                System.out.println(i + " " + (System.currentTimeMillis() - s) + " ms");
            }
            s = System.currentTimeMillis();
        }

        System.out.println("");
        System.out.println("Average: " + ((System.currentTimeMillis() - start) / COUNT)  + " ms");
        System.out.println("Total: " + ((System.currentTimeMillis() - start))  + " ms");

    }

    private void doLoginLogout(String port) {
        driver.navigate().to("http://localhost:" + port + "/auth/realms/master/account/");

        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("admin");
        driver.findElement(By.name("login")).click();

        assertEquals("http://localhost:" + port + "/auth/realms/master/account/", driver.getCurrentUrl());

        driver.findElement(By.linkText("Sign Out")).click();
    }

}
