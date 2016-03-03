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

package org.keycloak.testsuite.util;

import javax.ws.rs.core.UriBuilder;
import org.keycloak.testsuite.page.AbstractPage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.keycloak.testsuite.auth.page.login.PageWithLoginUrl;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author tkyjovsk
 */
public class URLAssert {

    public static void assertCurrentUrlEquals(AbstractPage page) {
        assertCurrentUrlEquals(page.getDriver(), page);
    }

    public static void assertCurrentUrlEquals(WebDriver driver, final AbstractPage page) {
//        WebDriverWait wait = new WebDriverWait(driver, 1);
//        ExpectedCondition<Boolean> urlStartsWith = new ExpectedCondition<Boolean>() {
//
//            @Override
//            public Boolean apply(WebDriver wd) {
//                return startsWithNormalized(wd.getCurrentUrl(), page.toString());
//            }
//        };
//        wait.until(urlStartsWith);
        assertEqualsNormalized(page.toString(), driver.getCurrentUrl());
    }

    public static void assertCurrentUrlStartsWith(AbstractPage page) {
        assertCurrentUrlStartsWith(page.getDriver(), page.toString());
    }

    public static void assertCurrentUrlStartsWith(WebDriver driver, final String url) {
//        WebDriverWait wait = new WebDriverWait(driver, 1);
//        ExpectedCondition<Boolean> urlStartsWith = new ExpectedCondition<Boolean>() {
//
//            @Override
//            public Boolean apply(WebDriver wd) {
//                return startsWithNormalized(wd.getCurrentUrl(), url);
//            }
//        };
//        wait.until(urlStartsWith);
        assertTrue("'" + driver.getCurrentUrl() + " does not start with '" + url + "'", startsWithNormalized(driver.getCurrentUrl(), url));
    }

    public static void assertCurrentUrlDoesntStartWith(AbstractPage page) {
        assertCurrentUrlDoesntStartWith(page.getDriver(), page.toString());
    }

    public static void assertCurrentUrlDoesntStartWith(WebDriver driver, final String url) {
//        WebDriverWait wait = new WebDriverWait(driver, 1, 250);
//        ExpectedCondition<Boolean> urlDoesntStartWith = new ExpectedCondition<Boolean>() {
//
//            @Override
//            public Boolean apply(WebDriver wd) {
//                return !startsWithNormalized(wd.getCurrentUrl(), url);
//            }
//        };
//        wait.until(urlDoesntStartWith);
        assertFalse(startsWithNormalized(driver.getCurrentUrl(), url));
    }

    // this normalization is needed because of slash-encoding in uri fragment (the part after #)
    public static String normalizeUri(String uri) {
        return UriBuilder.fromUri(uri).build().toASCIIString();
    }

    public static boolean startsWithNormalized(String str1, String str2) {
        String uri1 = normalizeUri(str1);
        String uri2 = normalizeUri(str2);
        return uri1.startsWith(uri2);
    }

    public static void assertEqualsNormalized(String str1, String str2) {
        assertEquals(normalizeUri(str1), normalizeUri(str2));
    }



    public static void assertCurrentUrlStartsWithLoginUrlOf(PageWithLoginUrl page) {
        assertCurrentUrlStartsWithLoginUrlOf(page.getDriver(), page);
    }
    
    public static void assertCurrentUrlStartsWithLoginUrlOf(WebDriver driver, PageWithLoginUrl page) {
        assertCurrentUrlStartsWith(driver, page.getOIDCLoginUrl().toString());
    }

}
