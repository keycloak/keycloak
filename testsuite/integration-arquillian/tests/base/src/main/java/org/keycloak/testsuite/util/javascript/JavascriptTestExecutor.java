package org.keycloak.testsuite.util.javascript;

import org.jboss.logging.Logger;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;


/**
 * @author mhajas
 */
public class JavascriptTestExecutor {
    protected WebDriver jsDriver;
    protected JavascriptExecutor jsExecutor;
    private WebElement output;
    protected WebElement events;
    private OIDCLogin loginPage;
    protected boolean configured;

    private static final Logger logger = Logger.getLogger(JavascriptTestExecutor.class);

    public static JavascriptTestExecutor create(WebDriver driver, OIDCLogin loginPage) {
        return new JavascriptTestExecutor(driver, loginPage);
    }

    protected JavascriptTestExecutor(WebDriver driver, OIDCLogin loginPage) {
        this.jsDriver = driver;
        driver.manage().timeouts().setScriptTimeout(WaitUtils.PAGELOAD_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        jsExecutor = (JavascriptExecutor) driver;
        events = driver.findElement(By.id("events"));
        output = driver.findElement(By.id("output"));
        this.loginPage = loginPage;
        configured = false;
    }

    public JavascriptTestExecutor login() {
        return login((String)null, null);
    }
    
    public JavascriptTestExecutor login(JavascriptStateValidator validator) {
        return login((String)null, validator);
    }

    /**
     * Attaches a MutationObserver that sends a message from iframe to main window with incorrect data when the iframe is loaded
     */
    public JavascriptTestExecutor attachCheck3pCookiesIframeMutationObserver() {
        jsExecutor.executeScript("// Select the node that will be observed for mutations\n" +
                "    const targetNode = document.body;" +
                "" +
                "    // Options for the observer (which mutations to observe)\n" +
                "    const config = {attributes: true, childList: true, subtree: true};" +
                "" +
                "    // Callback function to execute when mutations are observed\n" +
                "    const callback = function (mutationsList, observer) {" +
                "        console.log(\"Mutation found\");" +
                "        var iframeNode = mutationsList[0].addedNodes[0];" +
        "                if (iframeNode && iframeNode.localName === 'iframe') {" +
        "                    var s = document.createElement('script');" +
        "                    s.type = 'text/javascript';" +
        "                    var code = \"window.parent.postMessage('Evil Message', '*');\";" +
        "                    s.appendChild(document.createTextNode(code));" +
        "                    iframeNode.contentDocument.body.appendChild(s);" +
        "                }" +
                "    }\n" +
                "" +
                "    // Create an observer instance linked to the callback function\n" +
                "    const observer = new MutationObserver(callback);" +
                "" +
                "    // Start observing the target node for configured mutations\n" +
                "    observer.observe(targetNode, config);");
        
        return this;
    }
    
    public JavascriptTestExecutor login(JSObjectBuilder optionsBuilder, JavascriptStateValidator validator) {
        return login(optionsBuilder.build(), validator);
    }

    public JavascriptTestExecutor login(String options, JavascriptStateValidator validator) {
        if (options == null)
            jsExecutor.executeScript("keycloak.login()");
        else {
            jsExecutor.executeScript("keycloak.login(" + options + ")");
        }
        waitForPageToLoad();

        if (validator != null) {
            validator.validate(jsDriver, output, events);
        }

        configured = false; // Getting out of testApp page => loosing keycloak variable etc.

        return this;
    }

    public JavascriptTestExecutor loginForm(UserRepresentation user) {
        return loginForm(user, null);
    }

    public JavascriptTestExecutor loginForm(UserRepresentation user, JavascriptStateValidator validator) {
        loginPage.form().login(user);
        waitForPageToLoad();

        if (validator != null) {
            validator.validate(jsDriver, null, events);
        }

        configured = false; // Getting out of testApp page => loosing keycloak variable etc.
        // this is necessary in case we skipped login button for example in login-required mode

        return this;
    }

    public JavascriptTestExecutor logout() {
        return logout(null);
    }

    public JavascriptTestExecutor logout(JavascriptStateValidator validator) {
        return logout(validator, null);
    }

    public JavascriptTestExecutor logout(JavascriptStateValidator validator, LogoutConfirmPage logoutConfirmPage) {
        jsExecutor.executeScript("keycloak.logout()");

        try {
            // simple check if we are at the logout confirm page, if so just click 'Yes'
            if (logoutConfirmPage != null && logoutConfirmPage.isCurrent(jsDriver)) {
                logoutConfirmPage.confirmLogout(jsDriver);
                waitForPageToLoad();
            }
        } catch (Exception ex) {
            // ignore errors when checking logoutConfirm page, if an error tests will also fail
            logger.error("Exception during checking logout confirmation page", ex);
        }

        if (validator != null) {
            validator.validate(jsDriver, output, events);
        }

        configured = false; // Loosing keycloak variable so we need to create it when init next session

        return this;
    }

    public JavascriptTestExecutor configure() {
        return configure(null);
    }

    public JavascriptTestExecutor configure(JSObjectBuilder argumentsBuilder) {
        // a nasty hack: redirect console.warn to events
        // mainly for FF as it doesn't yet support reading console.warn directly through webdriver
        // see https://github.com/mozilla/geckodriver/issues/284
        jsExecutor.executeScript("console.warn = event;");

        if (argumentsBuilder == null) {
            jsExecutor.executeScript("window.keycloak = Keycloak();");
        } else {
            String configArguments = argumentsBuilder.build();
            jsExecutor.executeScript("window.keycloak = Keycloak(" + configArguments + ");");
        }

        jsExecutor.executeScript("window.keycloak.onAuthSuccess = function () {event('Auth Success')};"); // event function is declared in index.html
        jsExecutor.executeScript("window.keycloak.onAuthError = function () {event('Auth Error')}");
        jsExecutor.executeScript("window.keycloak.onAuthRefreshSuccess = function () {event('Auth Refresh Success')}");
        jsExecutor.executeScript("window.keycloak.onAuthRefreshError = function () {event('Auth Refresh Error')}");
        jsExecutor.executeScript("window.keycloak.onAuthLogout = function () {event('Auth Logout')}");
        jsExecutor.executeScript("window.keycloak.onTokenExpired = function () {event('Access token expired.')}");
        jsExecutor.executeScript("window.keycloak.onActionUpdate = function (status) {event('AIA status: ' + status)}");

        configured = true;

        return this;
    }

    public JavascriptTestExecutor init(JSObjectBuilder argumentsBuilder) {
        return init(argumentsBuilder, null);
    }

    public JavascriptTestExecutor init(JSObjectBuilder argumentsBuilder, JavascriptStateValidator validator) {
        return init(argumentsBuilder, validator, false);
    }

    public JavascriptTestExecutor init(JSObjectBuilder argumentsBuilder, JavascriptStateValidator validator, boolean expectPromptNoneRedirect) {
        if(!configured) {
            configure();
        }

        String arguments = argumentsBuilder.build();

        String script = "var callback = arguments[arguments.length - 1];" +
                "   window.keycloak.init(" + arguments + ").then(function (authenticated) {" +
                "       callback(\"Init Success (\" + (authenticated ? \"Authenticated\" : \"Not Authenticated\") + \")\");" +
                "   }).catch(function (error) {" +
                "       callback(error);" +
                "   });";

        Object output;

        if (expectPromptNoneRedirect) {
            try {
                output = jsExecutor.executeAsyncScript(script);
                fail("Redirect to Keycloak was expected");
            }
            catch (WebDriverException e) {
                waitForPageToLoad();
                configured = false;
                // the redirect should use prompt=none, that means KC should immediately redirect back to the app (regardless login state)
                return init(argumentsBuilder, validator, false);
            }
        }
        else {
            output = jsExecutor.executeAsyncScript(script);
        }

        if (validator != null) {
            validator.validate(jsDriver, output, events);
        }

        return this;
    }

    public JavascriptTestExecutor logInAndInit(JSObjectBuilder argumentsBuilder,
                                               UserRepresentation user, JavascriptStateValidator validator) {
        init(argumentsBuilder);
        login();
        loginForm(user);
        init(argumentsBuilder, validator);
        return this;
    }

    public JavascriptTestExecutor refreshToken(int value) {
        return refreshToken(value, null);
    }

    public JavascriptTestExecutor refreshToken(int value, JavascriptStateValidator validator) {
        String script = "var callback = arguments[arguments.length - 1];" +
                "   window.keycloak.updateToken(" + Integer.toString(value) + ").then(function (refreshed) {" +
                "       if (refreshed) {" +
                "            callback(window.keycloak.tokenParsed);" +
                "       } else {" +
                "            callback('Token not refreshed, valid for ' + Math.round(window.keycloak.tokenParsed.exp + window.keycloak.timeSkew - new Date().getTime() / 1000) + ' seconds');" +
                "       }" +
                "   }).catch(function () {" +
                "       callback('Failed to refresh token');" +
                "   });";

        Object output = jsExecutor.executeAsyncScript(script);

        if(validator != null) {
            validator.validate(jsDriver, output, events);
        }

        return this;
    }

    public JavascriptTestExecutor openAccountPage(JavascriptStateValidator validator) {
        jsExecutor.executeScript("window.keycloak.accountManagement()");
        waitForPageToLoad();

        // Leaving page -> loosing keycloak variable
        configured = false;

        if (validator != null) {
            validator.validate(jsDriver, null, null);
        }

        return this;
    }

    public JavascriptTestExecutor getProfile() {
        return getProfile(null);
    }

    public JavascriptTestExecutor getProfile(JavascriptStateValidator validator) {

        String script = "var callback = arguments[arguments.length - 1];" +
                    "   window.keycloak.loadUserProfile().then(function (profile) {" +
                    "       callback(profile);" +
                    "   }, function () {" +
                    "       callback('Failed to load profile');" +
                    "   });";

        Object output = jsExecutor.executeAsyncScript(script);

        if(validator != null) {
            validator.validate(jsDriver, output, events);
        }
        return this;
    }

    public JavascriptTestExecutor sendXMLHttpRequest(XMLHttpRequest request, ResponseValidator validator) {
        validator.validate(request.send(jsExecutor));

        return this;
    }

    public JavascriptTestExecutor refresh() {
        jsDriver.navigate().refresh();
        configured = false; // Refreshing webpage => Loosing window.keycloak variable

        return this;
    }

    public JavascriptTestExecutor addTimeSkew(int addition) {
        jsExecutor.executeScript("window.keycloak.timeSkew += " + Integer.toString(addition));

        return this;
    }

    public JavascriptTestExecutor checkTimeSkew(JavascriptStateValidator validator) {
        Object timeSkew = jsExecutor.executeScript("return window.keycloak.timeSkew");

        validator.validate(jsDriver, timeSkew, events);

        return this;
    }

    public JavascriptTestExecutor executeScript(String script) {
        return executeScript(script, null);
    }

    public JavascriptTestExecutor executeScript(String script, JavascriptStateValidator validator) {
        Object output = jsExecutor.executeScript(script);

        if(validator != null) {
            validator.validate(jsDriver, output, events);
        }

        return this;
    }

    public boolean isLoggedIn() {
        return (boolean) jsExecutor.executeScript("if (typeof keycloak !== 'undefined') {" +
                "return keycloak.authenticated" +
                "} else { return false}");
    }

    public JavascriptTestExecutor executeAsyncScript(String script) {
        return executeAsyncScript(script, null);
    }

    public JavascriptTestExecutor executeAsyncScript(String script, JavascriptStateValidator validator) {
        Object output = jsExecutor.executeAsyncScript(script);

        if(validator != null) {
            validator.validate(jsDriver, output, events);
        }

        return this;
    }

    public JavascriptTestExecutor errorResponse(JavascriptStateValidator validator) {
        Object output = jsExecutor.executeScript("return \"Error: \" + getParameterByName(\"error\") + \"\\n\" + \"Error description: \" + getParameterByName(\"error_description\")");

        validator.validate(jsDriver, output, events);
        return this;
    }

    public JavascriptTestExecutor wait(long millis, JavascriptStateValidator validator) {
        pause(millis);

        if (validator != null) {
            validator.validate(jsDriver, null, events);
        }

        return this;
    }

    public JavascriptTestExecutor validateOutputField(JavascriptStateValidator validator) {
        validator.validate(jsDriver, output, events);
        return this;
    }
}
