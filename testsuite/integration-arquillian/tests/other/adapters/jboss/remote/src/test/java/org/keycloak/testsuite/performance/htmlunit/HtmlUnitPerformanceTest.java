package org.keycloak.testsuite.performance.htmlunit;

import com.gargoylesoftware.htmlunit.WebClient;
import org.keycloak.testsuite.performance.PerformanceTest;

/**
 *
 * @author tkyjovsk
 */
public abstract class HtmlUnitPerformanceTest extends PerformanceTest {

    public abstract class Runnable extends PerformanceTest.Runnable {

        protected HtmlUnitDriver driver;

        public Runnable() {
            driver = new HtmlUnitDriver(true, false);
        }

        public final class HtmlUnitDriver extends org.openqa.selenium.htmlunit.HtmlUnitDriver {

            public HtmlUnitDriver(boolean javaScriptEnabled, boolean cssEnabled) {
                getWebClient().getOptions().setJavaScriptEnabled(javaScriptEnabled);
                getWebClient().getOptions().setCssEnabled(cssEnabled);
            }

            @Override
            public WebClient getWebClient() {
                return super.getWebClient();
            }

        }

    }

}
