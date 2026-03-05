# OAuth/OIDC Migration Rules

Read this file when the legacy test uses `@ArquillianResource OAuthClient`, `oauth.*` method calls, or any OAuth/OIDC flow patterns.

---

## OAuthClient Injection

```java
// OLD
@ArquillianResource
protected OAuthClient oauth;
// import org.keycloak.testsuite.util.oauth.OAuthClient;

// NEW
@InjectOAuthClient
OAuthClient oauthClient;
// import org.keycloak.testframework.oauth.OAuthClient;
// import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
```

**Attributes:**
- `@InjectOAuthClient(realmRef = "myRealm")` — bind to a specific realm
- `@InjectOAuthClient(kcAdmin = true)` — use admin credentials
- `@InjectOAuthClient(config = MyClientConfig.class)` — customize the client configuration
- `@InjectOAuthClient(webDriverRef = "second")` — bind to a specific WebDriver instance (see `webdriver.md`)

**IMPORTANT**: The new `OAuthClient` extends `AbstractOAuthClient` from `tests/utils-shared/`. Most method signatures are identical between old and new — the main change is injection, not API usage.

---

## OAuthClient Automatic Client Management

The `@InjectOAuthClient` supplier **automatically creates an OAuth client** in the realm and manages its full lifecycle. The architecture is:

```
TestAppSupplier → TestApp (mock HTTP server with callback endpoint)
       ↓
OAuthClientSupplier → creates client with redirectUri → TestApp callback
       ↓
OAuthClient (holds ManagedWebDriver + ClientResource references)
```

**What the supplier does:**
1. Gets the `TestApp` redirect URI (a mock HTTP callback server managed by the framework)
2. Creates a client in the realm with the configured clientId (default: `test-app`), pointing its redirect URI to the TestApp callback endpoint
3. If `ref` is set, appends `-<ref>` to the clientId, making it unique (e.g., `test-app-second`)
4. Holds a reference to the `ClientResource` for automatic cleanup
5. Holds a reference to the `ManagedWebDriver` for browser-based flows
6. Automatically removes the client when the test is done

**Critical rules:**

1. **Do NOT create a `test-app` client in `RealmConfig`** — the supplier creates it. Adding one manually will cause a 409 Conflict or interfere with the TestApp redirect endpoint wiring.

2. **Do NOT use custom `ClientConfig` just to change the clientId** — use `ref` instead. The `ref` appends to the default clientId making it unique while preserving the TestApp redirect URI connection:

```java
// WRONG — don't change clientId via config, it breaks the TestApp connection
@InjectOAuthClient(config = CustomConfig.class)  // avoid this pattern

// CORRECT — use ref to create a unique client
@InjectOAuthClient(ref = "second")  // creates client "test-app-second"
OAuthClient oauthClient2;
```

3. **If using `@InjectRealm(fromJson = ...)` and the JSON already contains a `test-app` client** — use `ref` on `@InjectOAuthClient` to avoid the 409 Conflict:

```java
@InjectRealm(fromJson = "/org/keycloak/tests/testrealm.json")
ManagedRealm realm;

// testrealm.json already has "test-app", so use ref to get "test-app-browser"
@InjectOAuthClient(ref = "browser")
OAuthClient oauthClient;
```

4. **Avoid `fromJson` with `testrealm.json` when possible** — prefer a minimal `RealmConfig` (see `core.md` Realm Configuration rule). A minimal realm won't have conflicting clients and `@InjectOAuthClient` works without any `ref`.

**Old patterns to remove:**
```java
// OLD — manually creating a client for OAuth in configureTestRealm
ClientBuilder.create().clientId("test-app").secret("password").redirectUris("*").build()
// Then: oauth.clientId("test-app")

// NEW — just inject, the supplier handles everything
@InjectOAuthClient
OAuthClient oauthClient;
// Client creation, redirect URI wiring, and cleanup are all automatic
```

---

## OAuthClient Method Reference

These methods exist on BOTH old and new `OAuthClient`. The API is shared via `AbstractOAuthClient` in `tests/utils-shared/`:

### Configuration methods (return `this` for chaining)

**Note:** The supplier auto-configures `realm`, `client`, and `redirectUri` from the injected `ManagedRealm` and `TestApp`. You typically do NOT need to call these manually. Only use them when a test needs to temporarily override settings (e.g., switching to a different scope or response type).

```java
oauthClient.realm("test")                    // Set target realm (auto-set by supplier)
oauthClient.client("clientId")               // Set client ID (auto-set by supplier)
oauthClient.client("clientId", "secret")     // Set client ID + secret (auto-set by supplier)
oauthClient.redirectUri("http://...")         // Set redirect URI (auto-set to TestApp callback)
oauthClient.scope("openid profile")          // Set scopes
oauthClient.openid(true)                     // Enable OpenID Connect
oauthClient.responseType("code")             // Set response type
oauthClient.responseMode("fragment")         // Set response mode
oauthClient.origin("http://...")             // Set origin header
```

**Migrating old `oauth.realm()` / `oauth.clientId()` calls:** Remove them — the supplier handles this. Only keep them if the test intentionally switches to a different realm or client mid-test.

### Login / Authorization
```java
oauthClient.openLoginForm()                          // Navigate browser to login page
oauthClient.loginForm()                              // Returns LoginUrlBuilder (add params like loginHint before .open())
oauthClient.doLogin("user", "password")              // Full login flow: openLoginForm() + fillLoginForm() + parseLoginResponse()
oauthClient.fillLoginForm("user", "password")        // Fill username+password in the login form and submit (uses LoginPage internally)
oauthClient.parseLoginResponse()                     // Wait for OAuth callback and parse redirect URL as AuthorizationEndpointResponse
oauthClient.openRegistrationForm()                   // Navigate browser to registration page
oauthClient.registrationForm()                       // Returns RegistrationUrlBuilder
```

### Token operations (`do*()` — return response directly)
```java
oauthClient.doAccessTokenRequest(code)               // Exchange code for tokens → AccessTokenResponse
oauthClient.doPasswordGrantRequest("user", "pass")   // Password grant → AccessTokenResponse
oauthClient.doRefreshTokenRequest(refreshToken)      // Refresh token → AccessTokenResponse
oauthClient.doClientCredentialsGrantAccessTokenRequest()  // Client credentials → AccessTokenResponse
oauthClient.doJWTAuthorizationGrantRequest(assertion)     // JWT grant → AccessTokenResponse
oauthClient.doTokenExchange(subjectToken)            // Token exchange → AccessTokenResponse
oauthClient.doFetchExternalIdpToken(alias, token)    // Fetch IdP token → AccessTokenResponse
oauthClient.doPushedAuthorizationRequest()           // PAR → ParResponse
```

### Token inspection
```java
oauthClient.doUserInfoRequest(accessToken)           // UserInfo endpoint → UserInfoResponse
oauthClient.doIntrospectionAccessTokenRequest(token)  // Introspect access token → IntrospectionResponse
oauthClient.doIntrospectionRefreshTokenRequest(token)  // Introspect refresh token → IntrospectionResponse
oauthClient.doIntrospectionRequest(token, typeHint)   // Introspect with type hint → IntrospectionResponse
oauthClient.doTokenRevoke(token)                      // Revoke token → TokenRevocationResponse
```

### Token parsing & verification
```java
oauthClient.verifyToken(tokenString)                 // Verify + parse → AccessToken
oauthClient.verifyIDToken(tokenString)               // Verify + parse → IDToken
oauthClient.parseRefreshToken(tokenString)           // Parse (no verify) → RefreshToken
oauthClient.parseToken(tokenString, MyToken.class)   // Parse (no verify) → custom token type
```

### Logout
```java
oauthClient.doLogout(refreshToken)                   // RP-initiated logout → LogoutResponse
oauthClient.openLogoutForm()                         // Navigate browser to logout page
oauthClient.logoutForm()                             // Returns LogoutUrlBuilder
oauthClient.doBackchannelLogout(logoutToken)         // Backchannel logout → BackchannelLogoutResponse
```

### Discovery & registration
```java
oauthClient.doWellKnownRequest()                     // OIDC discovery → OIDCConfigurationRepresentation
oauthClient.clientRegistration()                     // Create ClientRegistration instance (NEW — only on new OAuthClient)
```

### Sub-clients (specialized protocols)
```java
oauthClient.ciba()                                   // Returns CibaClient (CIBA flow)
oauthClient.device()                                 // Returns DeviceClient (device auth grant)
oauthClient.oid4vc()                                 // Returns OID4VCClient (OID4VC)
```

### Accessors
```java
oauthClient.config()                                 // Returns OAuthClientConfig (current state)
oauthClient.getRealm()                               // Current realm name
oauthClient.getClientId()                            // Current client ID
oauthClient.getRedirectUri()                         // Current redirect URI
oauthClient.getEndpoints()                           // Returns Endpoints (token, auth, userinfo URLs)
oauthClient.keys()                                   // Returns KeyManager (signing keys)
oauthClient.httpClient()                             // Returns HttpClientManager
```

### Request builder methods (for advanced customization — call `.send()` to execute)
```java
oauthClient.passwordGrantRequest("user", "pass")     // → PasswordGrantRequest
oauthClient.accessTokenRequest(code)                 // → AccessTokenRequest
oauthClient.refreshRequest(refreshToken)             // → RefreshRequest
oauthClient.introspectionRequest(token)              // → IntrospectionRequest
oauthClient.logoutRequest()                          // → LogoutRequest
oauthClient.wellknownRequest()                       // → OpenIDProviderConfigurationRequest
oauthClient.userInfoRequest(token)                   // → UserInfoRequest
oauthClient.tokenRevocationRequest(token)            // → TokenRevocationRequest
oauthClient.tokenExchangeRequest(subjectToken)       // → TokenExchangeRequest
oauthClient.backchannelLogoutRequest(logoutToken)    // → BackchannelLogoutRequest
oauthClient.clientCredentialsGrantRequest()          // → ClientCredentialsGrantRequest
oauthClient.jwtAuthorizationGrantRequest(assertion)  // → JWTAuthorizationGrantRequest
oauthClient.pushedAuthorizationRequest()             // → ParRequest
oauthClient.permissionGrantRequest()                 // → PermissionGrantRequest
```

**IMPORTANT — `do*()` vs builder methods:**
- `do*()` methods (e.g., `doAccessTokenRequest(code)`) return the response **directly** — do NOT append `.send()`
- Builder methods (e.g., `accessTokenRequest(code)`) return a request object you can customize, then call `.send()` on
- `doLogin()` is special — it drives the browser through the full login flow (open → fill → parse callback). It's `openLoginForm()` + `fillLoginForm()` + `parseLoginResponse()` combined.

---

## Response Objects

These classes are in `tests/utils-shared/` and are the same in both old and new:

```java
// These imports stay the same (already in utils-shared)
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
```

Common response methods:
```java
AccessTokenResponse response = oauthClient.doAccessTokenRequest(code);
response.getAccessToken()        // JWT access token string
response.getRefreshToken()       // JWT refresh token string
response.getIdToken()            // JWT ID token string
response.getStatusCode()         // HTTP status code
response.getError()              // Error string if failed
response.getErrorDescription()   // Error description
response.getExpiresIn()          // Token expiry in seconds

AuthorizationEndpointResponse authResponse = oauthClient.parseLoginResponse();
authResponse.getCode()           // Authorization code
authResponse.isRedirected()      // Whether redirect happened
authResponse.getError()          // Error if failed
```

---

## Key Differences from Legacy

1. **Import path**: `org.keycloak.testsuite.util.oauth.OAuthClient` → `org.keycloak.testframework.oauth.OAuthClient`
2. **Injection**: `@ArquillianResource` → `@InjectOAuthClient`
3. **Field name**: Convention is `oauthClient` (not `oauth`)
4. **WebDriver**: The new OAuthClient holds a reference to `ManagedWebDriver` automatically. No manual `oauth.driver(driver)`, `oauth.setDriver()`, or `oauth.init()` calls needed. Use `webDriverRef` to bind to a specific WebDriver instance.
5. **Client management**: The supplier creates a unique client in the realm and holds a `ClientResource` reference for automatic cleanup. No need to create a client externally.
6. **`clientRegistration()`**: New convenience method on new OAuthClient — creates a `ClientRegistration` pre-configured with the server URL and realm.
7. **No `newConfig()`**: The old `oauth.newConfig().driver(driver2)` pattern for multi-browser is replaced by `@InjectOAuthClient(webDriverRef = "second")`. See `webdriver.md`.
8. **No static URL fields**: `OAuthClient.AUTH_SERVER_ROOT`, `OAuthClient.SERVER_ROOT`, `OAuthClient.APP_ROOT` don't exist in the new OAuthClient. Use `@InjectKeycloakUrls` instead (see `server-and-registration.md`).
9. **`parseLoginResponse()`**: The new OAuthClient waits for the OAuth callback before parsing (calls `webDriver.waiting().waitForOAuthCallback()`). The old one parsed immediately.

**Most `oauth.*` method calls require NO changes** other than the field name (`oauth` → `oauthClient`). The API is shared via `AbstractOAuthClient` in `tests/utils-shared/`.

**Old patterns to remove:**

| Old Pattern | Action |
|---|---|
| `oauth.init()` | Remove — supplier handles initialization |
| `oauth.setDriver(driver)` / `oauth.driver(driver)` | Remove — supplier wires the WebDriver |
| `oauth.clientId("x")` | Replace with `oauthClient.client("x")` (old method was deprecated) |
| `oauth.newConfig().driver(driver2)` | Replace with `@InjectOAuthClient(webDriverRef = "second")` |
| `OAuthClient.AUTH_SERVER_ROOT` | Replace with `keycloakUrls.getBaseUrl().toString()` |
| `OAuthClient.SERVER_ROOT` | Replace with `keycloakUrls.getBaseUrl().toString()` (without `/auth` suffix) |
| `OAuthClient.APP_ROOT` | Remove — TestApp handles the redirect endpoint |
