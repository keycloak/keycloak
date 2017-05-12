package org.keycloak.testsuite.adapter.javascript;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.concurrent.TimeUnit;


/**
 * @author mhajas
 */
public class JavascriptTestExecutor {
    private WebDriver jsDriver;
    private JavascriptExecutor jsExecutor;
    private WebElement output;
    private WebElement events;
    private OIDCLogin loginPage;
    private boolean configured;

    public static JavascriptTestExecutor create(WebDriver driver, OIDCLogin loginPage) {
        return new JavascriptTestExecutor(driver, loginPage);
    }

    private JavascriptTestExecutor(WebDriver driver, OIDCLogin loginPage) {
        this.jsDriver = driver;
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
        jsExecutor = (JavascriptExecutor) driver;
        events = driver.findElement(By.id("events"));
        output = driver.findElement(By.id("output"));
        this.loginPage = loginPage;
        configured = false;
    }

    public JavascriptTestExecutor login() {
        return login(null);
    }

    public JavascriptTestExecutor login(JavascriptStateValidator validator) {
        jsExecutor.executeScript("keycloak.login()");

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
        jsExecutor.executeScript("keycloak.logout()");
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
        if (argumentsBuilder == null) {
            jsExecutor.executeScript("keycloak = Keycloak()");
        } else {
            String configArguments = argumentsBuilder.build();
            jsExecutor.executeScript("keycloak = Keycloak(" + configArguments + ")");
        }

        jsExecutor.executeScript("keycloak.onAuthSuccess = function () {event('Auth Success')}"); // event function is declared in index.html
        jsExecutor.executeScript("keycloak.onAuthError = function () {event('Auth Error')}");
        jsExecutor.executeScript("keycloak.onAuthRefreshSuccess = function () {event('Auth Refresh Success')}");
        jsExecutor.executeScript("keycloak.onAuthRefreshError = function () {event('Auth Refresh Error')}");
        jsExecutor.executeScript("keycloak.onAuthLogout = function () {event('Auth Logout')}");
        jsExecutor.executeScript("keycloak.onTokenExpired = function () {event('Access token expired.')}");

        configured = true;

        return this;
    }

    public JavascriptTestExecutor init(JSObjectBuilder argumentsBuilder) {
        return init(argumentsBuilder, null);
    }

    public JavascriptTestExecutor init(JSObjectBuilder argumentsBuilder, JavascriptStateValidator validator) {
        if(!configured) {
            configure();
        }

        String arguments = argumentsBuilder.build();

        Object output = jsExecutor.executeAsyncScript(
                "var callback = arguments[arguments.length - 1];" +
                "   keycloak.init(" + arguments + ").success(function (authenticated) {" +
                "       callback(\"Init Success (\" + (authenticated ? \"Authenticated\" : \"Not Authenticated\") + \")\");" +
                "   }).error(function () {" +
                "       callback(\"Init Error\");" +
                "   });");

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
        Object output = jsExecutor.executeAsyncScript(
                    "var callback = arguments[arguments.length - 1];" +
                    "   keycloak.updateToken(" + Integer.toString(value) + ").success(function (refreshed) {" +
                    "       if (refreshed) {" +
                    "            callback(keycloak.tokenParsed);" +
                    "       } else {" +
                    "            callback('Token not refreshed, valid for ' + Math.round(keycloak.tokenParsed.exp + keycloak.timeSkew - new Date().getTime() / 1000) + ' seconds');" +
                    "       }" +
                    "   }).error(function () {" +
                    "       callback('Failed to refresh token');" +
                    "   });");

        if(validator != null) {
            validator.validate(jsDriver, output, events);
        }

        return this;
    }

    public JavascriptTestExecutor getProfile() {
        return getProfile(null);
    }

    public JavascriptTestExecutor getProfile(JavascriptStateValidator validator) {

        Object output = jsExecutor.executeAsyncScript(
                "var callback = arguments[arguments.length - 1];" +
                "   keycloak.loadUserProfile().success(function (profile) {" +
                "       callback(profile);" +
                "   }).error(function () {" +
                "       callback('Failed to load profile');" +
                "   });");

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
        configured = false; // Refreshing webpage => Loosing keycloak variable

        return this;
    }

    public JavascriptTestExecutor addTimeSkew(int addition) {
        jsExecutor.executeScript("keycloak.timeSkew += " + Integer.toString(addition));

        return this;
    }

    public JavascriptTestExecutor checkTimeSkew(JavascriptStateValidator validator) {
        Object timeSkew = jsExecutor.executeScript("return keycloak.timeSkew");

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

}
