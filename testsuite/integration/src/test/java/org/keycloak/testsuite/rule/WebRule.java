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
package org.keycloak.testsuite.rule;

import com.gargoylesoftware.htmlunit.WebClient;
import org.junit.rules.ExternalResource;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.PageFactory;

import java.lang.reflect.Field;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class WebRule extends ExternalResource {

    private WebDriver driver;
    private OAuthClient oauth;
    private Object test;

    public WebRule(Object test) {
        this.test = test;
    }

    public void initProperties() {
        driver = createWebDriver();
        oauth = new OAuthClient(driver);
    }

    @Override
    public void before() throws Throwable {
        initProperties();
        initWebResources(test);
    }

    public static WebDriver createWebDriver() {
        WebDriver driver;
        String browser = "htmlunit";
        if (System.getProperty("browser") != null) {
            browser = System.getProperty("browser");
        }

        if (browser.equals("htmlunit")) {
            HtmlUnitDriver d = new HtmlUnitDriver();
            d.getWebClient().getOptions().setJavaScriptEnabled(true);
            d.getWebClient().getOptions().setCssEnabled(false);
            d.getWebClient().getOptions().setTimeout(1000000);
            driver = d;
        } else if (browser.equals("chrome")) {
            driver = new ChromeDriver();
        } else if (browser.equals("firefox")) {
            driver = new FirefoxDriver();
        } else {
            throw new RuntimeException("Unsupported browser " + browser);
        }
        return driver;
    }

    protected void initWebResources(Object o) {
        Class<?> c = o.getClass();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getAnnotation(WebResource.class) != null) {
                    Class<?> type = f.getType();
                    if (type.equals(WebDriver.class)) {
                        set(f, o, driver);
                    } else if (AbstractPage.class.isAssignableFrom(type)) {
                        set(f, o, getPage(f.getType()));
                    } else if (type.equals(OAuthClient.class)) {
                        set(f, o, oauth);
                    } else {
                        throw new RuntimeException("Unsupported type " + f);
                    }
                }
            }

            c = c.getSuperclass();
        }
    }

    protected void set(Field f, Object o, Object v) {
        f.setAccessible(true);
        try {
            f.set(o, v);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public WebDriver getDriver() {
        return driver;
    }

    public <T> T getPage(Class<T> pageClass) {
        try {
            T instance = pageClass.newInstance();
            initWebResources(instance);
            PageFactory.initElements(driver, instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void after() {
        driver.manage().deleteAllCookies();
        driver.close();
    }

    public static class HtmlUnitDriver extends org.openqa.selenium.htmlunit.HtmlUnitDriver {
        
        @Override
        public WebClient getWebClient() {
            return super.getWebClient();
        }
        
    }
    
}
