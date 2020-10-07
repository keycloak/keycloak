package org.keycloak.testsuite.util.javascript;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.pages.ConsentPage;
import org.keycloak.testsuite.util.URLUtils;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 * @author mhajas
 */
public class JavascriptTestExecutorWithAuthorization extends JavascriptTestExecutor {

    public static JavascriptTestExecutorWithAuthorization create(WebDriver driver, OIDCLogin loginPage) {
        return new JavascriptTestExecutorWithAuthorization(driver, loginPage);
    }

    private JavascriptTestExecutorWithAuthorization(WebDriver driver, OIDCLogin loginPage) {
        super(driver, loginPage);
    }


    @Override
    public JavascriptTestExecutorWithAuthorization init(JSObjectBuilder argumentsBuilder, JavascriptStateValidator validator) {
        super.init(argumentsBuilder, validator);
        Object output = jsExecutor.executeScript(
                "var callback = arguments[arguments.length - 1];" +
                "window.authorization = new KeycloakAuthorization(window.keycloak);" +
                "while (typeof window.authorization === 'undefined') {}" + // Wait until authorization is initialized
                "return 'Authz initialized'");

        assertThat(output, is("Authz initialized"));
        return this;
    }

    @Override
    public JavascriptTestExecutorWithAuthorization login(JavascriptStateValidator validator) {
        super.login(validator);
        return this;
    }

    public JavascriptTestExecutorWithAuthorization loginFormWithScopesWithPossibleConsentPage(UserRepresentation user, JavascriptStateValidator validator, OAuthGrant oAuthGrantPage, String... scopes) {
        String currentUrl = jsDriver.getCurrentUrl();

        if (scopes.length > 0) {
            StringBuilder scopesValue = new StringBuilder();

            for (String scope : scopes) {
                if (scopesValue.length() != 0) {
                    scopesValue.append(" ");
                }
                scopesValue.append(scope);
            }

            scopesValue.append(" openid");


            StringBuilder urlWithScopeParam = new StringBuilder(currentUrl);

            int scopeIndex = currentUrl.indexOf("scope");

            if (scopeIndex != -1) {
                // Remove scope param from url
                urlWithScopeParam.delete(scopeIndex, currentUrl.indexOf('&', scopeIndex));
                // Add scope param to the end of query
                urlWithScopeParam.append("&").append("scope=");
            }

            if (!currentUrl.contains("?")) {
                urlWithScopeParam.append("?scope=");
            }

            urlWithScopeParam.append(scopesValue);

            URLUtils.navigateToUri(urlWithScopeParam.toString());
            waitForPageToLoad();
        }

        loginFormWithPossibleConsentPage(user, oAuthGrantPage, validator);
        return this;
    }

    public JavascriptTestExecutorWithAuthorization loginFormWithPossibleConsentPage(UserRepresentation user, OAuthGrant oAuthGrantPage, JavascriptStateValidator validator) {
        super.loginForm(user);

        try {
            // simple check if we are at the consent page, if so just click 'Yes'
            if (oAuthGrantPage.isCurrent(jsDriver)) {
                oAuthGrantPage.accept();
                waitForPageToLoad();
            }
        } catch (Exception ignore) {
            // ignore errors when checking consent page, if an error tests will also fail
        }

        if (validator != null) {
            validator.validate(jsDriver, null, events);
        }

        return this;
    }

    @Override
    public JavascriptTestExecutor sendXMLHttpRequest(XMLHttpRequest request, ResponseValidator validator) {
        // Intercept all requests and add rpt or token

        // check if rpt is already present
        Object o = jsExecutor.executeScript("if(window.authorization && window.authorization.rpt) return true; else return false;");



        if (o == null || o.equals(false)) {
            // RPT is not present yet, lets try to use bearer token
            request.includeBearerToken();
        } else {
            // RPT token is present so we will use it
            request.includeRpt();
        }

        // Try to send request
        Map<String, Object> result = request.send(jsExecutor);

        // If request was denied do UMA
        if ((Long.valueOf(403).equals(result.get("status")) || Long.valueOf(401).equals(result.get("status")))) {
            //extracting ticket from response
            String headersString = (String) result.get("responseHeaders");

            List<String> headersList = Arrays.asList(headersString.split("\r\n"));
            String wwwAuthenticate = headersList.stream().filter(s -> s.toLowerCase().startsWith("www-authenticate:")).findFirst().get();

            if (wwwAuthenticate.contains("UMA") && wwwAuthenticate.contains("ticket")) {
                String ticket = Arrays.asList(wwwAuthenticate.split(",")).stream().filter(s -> s.startsWith("ticket")).findFirst().get();

                ticket = ticket.substring(0, ticket.length() - 1).replaceFirst("ticket=\"", "");

                // AuthorizationRequest for RPT
                o = jsExecutor.executeAsyncScript(
                        "var callback = arguments[arguments.length - 1];" +
                        "window.authorization" +
                        ".authorize(" + JSObjectBuilder.create().add("ticket", ticket).build() + ")" +
                        ".then(function (rpt) {callback(rpt)}, function() {callback('failed1')}, function() {callback('failed2')});");

                o = jsExecutor.executeScript("if(window.authorization && window.authorization.rpt) return true; else return false;"); // return window.authorization && window.authorization.rpt doesn't work

                if (o != null && o.equals(true)) {
                    request.includeRpt();
                    result = request.send(jsExecutor);
                }
            }
        }

        if (validator != null) {
            validator.validate(result);
        }

        return this;
    }
}
