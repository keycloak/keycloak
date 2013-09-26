package org.keycloak.testsuite.rule;

import java.lang.reflect.Field;

import org.junit.rules.ExternalResource;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.Page;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.PageFactory;

import com.gargoylesoftware.htmlunit.WebClient;

public class WebRule extends ExternalResource {

    private WebDriver driver;
    private OAuthClient oauth;
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
            d.getWebClient().getOptions().setCssEnabled(false);
            driver = d;
        } else if (browser.equals("chrome")) {
            driver = new ChromeDriver();
        } else if (browser.equals("firefox")) {
            driver = new FirefoxDriver();
        } else {
            throw new RuntimeException("Unsupported browser " + browser);
        }

        oauth = new OAuthClient(driver);

        initWebResources(test);
    }

    protected void initWebResources(Object o) {
        Class<?> c = o.getClass();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getAnnotation(WebResource.class) != null) {
                    Class<?> type = f.getType();
                    if (type.equals(WebDriver.class)) {
                        set(f, o, driver);
                    } else if (Page.class.isAssignableFrom(type)) {
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
    protected void after() {
        driver.manage().deleteAllCookies();
        driver.close();
    }

    public class HtmlUnitDriver extends org.openqa.selenium.htmlunit.HtmlUnitDriver {
        
        @Override
        public WebClient getWebClient() {
            return super.getWebClient();
        }
        
    }
    
}
