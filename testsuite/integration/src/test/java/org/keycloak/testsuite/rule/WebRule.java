/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

    @Override
    public void before() throws Throwable {
        driver = createWebDriver();
        oauth = new OAuthClient(driver);
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
