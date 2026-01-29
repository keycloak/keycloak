package org.keycloak.testsuite.util.oauth;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.protocol.oid4vc.model.AuthorizationRequest;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AuthorizationRequestRequest extends AbstractHttpGetRequest<AuthorizationRequestRequest, AuthorizationRequestResponse> {

    private final AuthorizationRequest authRequest;
    private String username;
    private String password;

    public AuthorizationRequestRequest(AbstractOAuthClient<?> client, AuthorizationRequest authRequest) {
        super(client);
        this.authRequest = authRequest;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getAuthorization();
    }

    @Override
    protected void initRequest() {
    }

    public AuthorizationRequestRequest credentials(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    public AuthorizationRequestResponse send() {
        initRequest();
        String endpointUrl = authRequest.toRequestUrl(getEndpoint());
        client.driver.navigate().to(endpointUrl);
        AuthPageState authPageState = waitForLoginOrError();
        switch (authPageState) {
            case ERROR_REDIRECT -> {
                return new AuthorizationRequestResponse(client);
            }
            case ERROR_PAGE -> {
                Map<String, String> params = new HashMap<>();
                params.put("error", "invalid_request");
                client.driver.findElements(By.id("kc-error-message")).stream()
                        .map(d -> d.getText().trim().split("\\n")[0])
                        .forEach(s -> params.put("error_description", s));
                return new AuthorizationRequestResponse(params);
            }
            // AuthPageState.LOGIN
            default -> {
                client.fillLoginForm(username, password);
                return new AuthorizationRequestResponse(client);
            }
        }
    }

    @Override
    protected AuthorizationRequestResponse toResponse(CloseableHttpResponse response) {
        return null;
    }

    enum AuthPageState { LOGIN, ERROR_REDIRECT, ERROR_PAGE }

    private AuthPageState waitForLoginOrError() {
        WebDriverWait wait = new WebDriverWait(client.driver, Duration.ofSeconds(10));

        return wait.until(d -> {
            String url = d.getCurrentUrl();

            // Error via redirect (some flows)
            if (url != null && (url.contains("error=") || url.contains("error_description="))) {
                return AuthPageState.ERROR_REDIRECT;
            }

            // Error page rendered by Keycloak
            if (!d.findElements(By.id("kc-error-message")).isEmpty()) {
                return AuthPageState.ERROR_PAGE;
            }

            // Login form present
            if (!d.findElements(By.id("username")).isEmpty()
                    || !d.findElements(By.name("username")).isEmpty()) {
                return AuthPageState.LOGIN;
            }

            return null; // keep waiting
        });
    }
}
