package org.keycloak.testsuite.rule;

import java.lang.reflect.Field;

import org.junit.rules.ExternalResource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.PageFactory;

import com.gargoylesoftware.htmlunit.WebClient;

public class WebRule extends ExternalResource {

    private WebDriver driver;
    private Object test;

    public WebRule(Object test) {
        this.test = test;
    }

    @Override
    protected void before() throws Throwable {
        String browser = "htmlunit";
        if (System.getProperty("browser") != null) {
            browser = System.getProperty("browser");
        }

        if (browser.equals("htmlunit")) {
            HtmlUnitDriver d = new HtmlUnitDriver();
            d.getWebClient().setCssEnabled(false);
            driver = d;
        } else if (browser.equals("chrome")) {
            driver = new ChromeDriver();
        } else if (browser.equals("firefox")) {
            driver = new FirefoxDriver();
        } else {
            throw new RuntimeException("Unsupported browser " + browser);
        }

        initDriver(test);
        initPages(test);
    }

    protected void initDriver(Object o) {
        Class<?> c = o.getClass();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getAnnotation(Driver.class) != null) {
                    set(f, o, driver);
                }
            }

            c = c.getSuperclass();
        }
    }

    protected void initPages(Object o) {
        Class<?> c = o.getClass();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getAnnotation(Page.class) != null) {
                    set(f, o, getPage(f.getType()));
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
            initDriver(instance);
            PageFactory.initElements(driver, instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void after() {
        driver.close();
    }

    public class HtmlUnitDriver extends org.openqa.selenium.htmlunit.HtmlUnitDriver {
        
        @Override
        public WebClient getWebClient() {
            return super.getWebClient();
        }
        
    }
    
}
