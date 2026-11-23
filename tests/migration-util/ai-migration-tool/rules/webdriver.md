# WebDriver & Page Object Migration Rules

Read this file when the legacy test uses `@Drone WebDriver`, `@Page`, `driver.*` calls, or page object patterns.

---

## WebDriver Injection

```java
// OLD
@Drone
protected WebDriver driver;
// import org.jboss.arquillian.drone.api.annotation.Drone;
// import org.openqa.selenium.WebDriver;

// NEW
@InjectWebDriver
ManagedWebDriver webDriver;
// import org.keycloak.testframework.ui.annotations.InjectWebDriver;
// import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
```

`ManagedWebDriver` wraps the raw Selenium `WebDriver` and provides convenience utilities. Access the raw driver via `webDriver.driver()` when needed.

---

## Multiple WebDriver Instances (`@SecondBrowser`)

Legacy tests used `@SecondBrowser` (a custom `@Drone` qualifier) to inject a second independent browser. In the new framework, use `ref` on `@InjectWebDriver` to create multiple instances:

```java
// OLD
@Drone
protected WebDriver driver;

@SecondBrowser
@Drone
protected WebDriver driver2;

// Use second browser with OAuthClient
OAuthClient oauth2 = oauth.newConfig().driver(driver2);

// NEW
@InjectWebDriver
ManagedWebDriver webDriver;

@InjectWebDriver(ref = "second")
ManagedWebDriver webDriver2;
```

**Binding pages and OAuthClient to a specific WebDriver:**

Use `webDriverRef` on `@InjectPage` and `@InjectOAuthClient` to bind them to a specific WebDriver instance:

```java
// Pages bound to the second browser
@InjectPage(webDriverRef = "second")
LoginPage loginPage2;

// OAuthClient using the second browser
@InjectOAuthClient(webDriverRef = "second")
OAuthClient oauthClient2;
```

**Complete example** — test with two independent browser sessions:

```java
// Primary browser
@InjectWebDriver
ManagedWebDriver webDriver;

@InjectOAuthClient
OAuthClient oauthClient;

@InjectPage
LoginPage loginPage;

// Second browser — independent session, cookies, state
@InjectWebDriver(ref = "second")
ManagedWebDriver webDriver2;

@InjectOAuthClient(webDriverRef = "second", ref = "second")
OAuthClient oauthClient2;

@InjectPage(webDriverRef = "second")
LoginPage loginPage2;
```

Each WebDriver instance has its own browser session with independent cookies and state. This replaces the `@SecondBrowser` pattern entirely.

**Imports:**
```
org.keycloak.testsuite.util.SecondBrowser  → remove (no replacement needed)
```

---

## ManagedWebDriver API

```java
webDriver.driver()                    // Get raw Selenium WebDriver
webDriver.getCurrentUrl()             // Current page URL
webDriver.findElement(By.id("foo"))   // Find element
webDriver.open(url)                   // Navigate to URL
webDriver.getBrowserType()            // CHROME, FIREFOX, or HTML_UNIT

// Utility facades
webDriver.waiting()                   // WaitUtils — chainable waiting
webDriver.navigate()                  // NavigateUtils — navigation helpers
webDriver.cookies()                   // CookieUtils — cookie management
webDriver.page()                      // PageUtils — page helpers
webDriver.tabs()                      // BrowserTabUtils — multi-tab support
webDriver.assertions()                // AssertionUtils — assertion helpers
```

---

## Common Driver Migrations

```java
// OLD: driver.navigate().to(url)
// NEW:
webDriver.open(url);
// or: webDriver.driver().navigate().to(url);

// OLD: driver.getCurrentUrl()
// NEW:
webDriver.getCurrentUrl();

// OLD: driver.findElement(By.id("foo"))
// NEW:
webDriver.findElement(By.id("foo"));
// or: webDriver.driver().findElement(By.id("foo"));

// OLD: driver.getPageSource()
// NEW (preferred — uses PageUtils wrapper):
webDriver.page().getPageSource();
// or: webDriver.driver().getPageSource();

// OLD: driver.manage().deleteAllCookies()
// NEW:
webDriver.cookies().deleteAll();
// or: webDriver.driver().manage().deleteAllCookies();

// OLD: driver.getTitle()
// NEW:
webDriver.driver().getTitle();
```

---

## WaitUtils Migration

```java
// OLD (static utility)
import org.keycloak.testsuite.util.WaitUtils;
WaitUtils.waitForPageToLoad();
WaitUtils.pause(1000);
WaitUtils.waitUntilElement(element).is().present();

// NEW (instance via ManagedWebDriver)
webDriver.waiting().waitForPage(page);       // Wait for specific page
webDriver.waiting().waitForOAuthCallback();  // Wait for OAuth redirect
webDriver.waiting().waitForTitle("Title");   // Wait for page title
webDriver.waiting().until(d -> ...);         // Generic wait with function
```

The new `WaitUtils` uses 5-second timeout with 50ms polling. Access via `webDriver.waiting()`.

**NOTE**: `WaitUtils.pause()` (hard sleep) has no direct equivalent — and shouldn't. Use explicit wait conditions instead.

---

## Page Object Method Migration

**Do NOT add new methods to framework page objects.** Use the existing methods as-is. When the old test calls a method that doesn't exist on the new page, decompose or replace it.

### Login methods — use fill + submit

```java
// OLD — single login() call
loginPage.login("user", "password");

// NEW — use fillLogin() + submit() separately
loginPage.fillLogin("user", "password");
loginPage.submit();

// OLD — LoginUsernameOnlyPage.login()
loginUsernameOnlyPage.login("user");

// NEW — use fillLoginWithUsernameOnly() + submit()
loginUsernamePage.fillLoginWithUsernameOnly("user");
loginUsernamePage.submit();

// OLD — PasswordPage.login()
passwordPage.login("password");

// NEW — use fillPassword() + submit()
passwordPage.fillPassword("password");
passwordPage.submit();
```

### Error assertions — use existing getError() or ErrorPage

```java
// OLD — getInputError() / getPasswordError() / getUsernameError() on various pages
loginPage.getInputError()
passwordPage.getPasswordError()
loginUsernameOnlyPage.getUsernameError()

// NEW — use existing methods on the page, or ErrorPage for error pages, or webDriver.findElement() for input errors
loginPage.getUsernameInputError()       // exists on LoginPage (#input-error-username)
loginPage.getError()                    // exists on LoginPage (alert-level error)
passwordPage.getError()                 // exists on PasswordPage (alert-level error)
errorPage.getError()                    // exists on ErrorPage (dedicated error page)

// For input-level errors not covered by existing methods, use webDriver directly:
webDriver.findElement(By.id("input-error-password")).getText()
webDriver.findElement(By.id("input-error-username")).getText()
```

Do NOT add `getPasswordError()`, `getUsernameError()`, or other new methods to framework pages.

---

## Page Object Injection

**See `specs/page-objects.md`** for the full catalog of available vs missing pages, usage frequency, and name mappings.

```java
// OLD
@Page
protected LoginPage loginPage;
// import org.jboss.arquillian.graphene.page.Page;
// import org.keycloak.testsuite.pages.LoginPage;

// NEW
@InjectPage
LoginPage loginPage;
// import org.keycloak.testframework.ui.annotations.InjectPage;
// import org.keycloak.testframework.ui.page.LoginPage;
```

---

## Page Navigation (loginPage.open() etc.)

Old page objects had `open()` methods that navigated directly to pages. New page objects do NOT have `open()`. Use `OAuthClient` to navigate to login-related pages:

```java
// OLD — page objects had open() methods
loginPage.open();
registerPage.open();

// NEW — use OAuthClient convenience methods (preferred)
oauth.openLoginForm();              // opens the login page
oauth.openRegistrationForm();       // opens the registration page
oauth.openLogoutForm();             // opens the logout page

// NEW — builder pattern (use when you need to add params like login_hint, state, etc.)
oauth.loginForm().loginHint("user@example.com").open();
oauth.registrationForm().open();
```

This means tests using `@Page` page objects for navigation also need `@InjectOAuthClient OAuthClient oauth`.

**When the test navigates to a non-login page** (e.g., account page, admin console), use `webDriver.open(url)` with the appropriate URL constructed from `@InjectKeycloakUrls`.

---

## Page Object Class Mapping

These page classes exist in BOTH old and new frameworks (different packages):

| Old (`org.keycloak.testsuite.pages.*`) | New (`org.keycloak.testframework.ui.page.*`) |
|---|---|
| `LoginPage` | `LoginPage` |
| `LoginUsernamePage` | `LoginUsernamePage` |
| `LoginUsernameOnlyPage` | `LoginUsernamePage` *(same page, different old name)* |
| `LoginPasswordResetPage` | `LoginPasswordResetPage` |
| `LoginPasswordUpdatePage` | `LoginPasswordUpdatePage` |
| `LoginExpiredPage` | `LoginExpiredPage` |
| `RegisterPage` | `RegisterPage` |
| `ErrorPage` | `ErrorPage` |
| `ConsentPage` | `ConsentPage` |
| `OAuthGrantPage` | `OAuthGrantPage` |
| `SelectAuthenticatorPage` | `SelectAuthenticatorPage` |
| `TermsAndConditionsPage` | `TermsAndConditionsPage` |
| `InfoPage` | `InfoPage` |
| `WelcomePage` | `WelcomePage` |
| `LogoutConfirmPage` | `LogoutConfirmPage` |
| `PasswordPage` | `PasswordPage` |
| `AdminPage` | `AdminPage` |
| `ProceedPage` | `ProceedPage` |

**Name mapping quirks** — some old names don't match the new names exactly:

| Old Name | New Name | Notes |
|---|---|---|
| `LoginUsernameOnlyPage` | `LoginUsernamePage` | Already exists in new framework |

**NOT in new framework** (40+ old pages have no equivalent):
- `AppPage` — no direct replacement
- Social login pages (`FacebookLoginPage`, `GoogleLoginPage`, etc.)
- IDP pages (`IdpConfirmLinkPage`, `IdpLinkEmailPage`, etc.)
- `LoginConfigTotpPage`, `LoginTotpPage`, `LoginUpdateProfilePage`, `UpdateAccountInformationPage`
- `VerifyEmailPage`, `VerifyProfilePage`
- `OneTimeCode` (from `org.keycloak.testsuite.auth.page.login`)

**Missing pages are NOT migration blockers** — create them during migration. See the next section.

---

## Creating New Page Objects

If the old test uses a page object that does NOT exist in `test-framework/ui/src/main/java/org/keycloak/testframework/ui/page/`, **create it as part of the migration**. This is not optional — missing pages must be created, not left as TODOs.

### Step-by-step

1. **Read the old page class** from `testsuite/` to understand its fields and methods
2. **Determine the new class name**: must end with `Page`. If the old name doesn't end with `Page`, add it (e.g., `OneTimeCode` → `OneTimeCodePage`)
3. **Location**: `test-framework/ui/src/main/java/org/keycloak/testframework/ui/page/`
4. **Extend** `AbstractLoginPage` (for login/auth pages) or `AbstractPage` (for other pages)
5. **Constructor**: `public XxxPage(ManagedWebDriver driver) { super(driver); }`
6. **Implement `getExpectedPageId()`**: return the `data-page-id` value from the HTML `<body data-page-id="...">`. To find this value, search the Keycloak theme templates (`themes/`) for the page's FreeMarker template, or check the convention pattern: `login-<page-name>` (e.g., `login-login-otp`, `login-login-username`)
7. **Do NOT port `isCurrent()`, `open()`, or any URL-based assertion** — they are replaced by `getExpectedPageId()` + the framework's `assertCurrent()` method
8. **Port only methods** the migrated test actually uses — don't copy the entire old page
9. **Replace `UIUtils.getTextFromElement(el)`** with `el.getText()`
10. **Replace `UIUtils.clickLink(el)` / `UIUtils.click(el)`** with `el.click()`
11. **Replace `driver.findElement()`** calls with `this.driver.findElement()` (using the inherited `ManagedWebDriver driver` field)

### Example

```java
// OLD (org.keycloak.testsuite.pages.LoginTotpPage)
public class LoginTotpPage extends LanguageComboboxAwarePage {
    @FindBy(id = "otp") private WebElement otpInput;
    @FindBy(css = "[type=\"submit\"]") private WebElement submitButton;
    @FindBy(id = "input-error-otp") private WebElement totpInputCodeError;

    public void login(String totp) {
        otpInput.clear();
        if (totp != null) otpInput.sendKeys(totp);
        UIUtils.clickLink(submitButton);
    }
    public String getInputError() { return UIUtils.getTextFromElement(totpInputCodeError); }
    public boolean isCurrent() { ... }  // DO NOT PORT
}

// NEW (org.keycloak.testframework.ui.page.LoginTotpPage)
public class LoginTotpPage extends AbstractLoginPage {
    @FindBy(id = "otp") private WebElement otpInput;
    @FindBy(css = "[type=submit]") private WebElement submitButton;
    @FindBy(id = "input-error-otp") private WebElement totpInputCodeError;

    public LoginTotpPage(ManagedWebDriver driver) { super(driver); }

    public void login(String totp) {
        otpInput.clear();
        if (totp != null) otpInput.sendKeys(totp);
        submitButton.click();
    }

    public String getInputError() {
        try { return totpInputCodeError.getText(); }
        catch (NoSuchElementException e) { return null; }
    }

    @Override
    public String getExpectedPageId() { return "login-login-otp"; }
}
```

### Key structural differences

| Aspect | Old (Arquillian) | New (Test Framework) |
|---|---|---|
| Driver field | `@ArquillianResource WebDriver` (inherited) | `protected final ManagedWebDriver driver` (inherited from `AbstractPage`) |
| Constructor | No-arg (Arquillian-managed) | `public XxxPage(ManagedWebDriver driver)` |
| Page identity | `boolean isCurrent()` | `String getExpectedPageId()` — matched against `<body data-page-id="...">` |
| Current assertion | `Assert.assertTrue(page.isCurrent())` | `page.assertCurrent()` (waits + asserts) |
| Text extraction | `UIUtils.getTextFromElement(el)` | `el.getText()` |
| Click | `UIUtils.clickLink(el)` / `UIUtils.click(el)` | `el.click()` |

---

## Page Assertions

```java
// OLD — boolean check
page.isCurrent();
Assert.assertTrue(page.isCurrent());
Assert.assertTrue(loginPage.isCurrent());

// NEW — assertCurrent() waits for the page and asserts it is current
page.assertCurrent();
loginPage.assertCurrent();

// OLD — negative check
Assert.assertFalse(page.isCurrent());
Assert.assertFalse(loginPage.isCurrent());

// NEW — no direct equivalent for negative assertion
// Option A: verify you're on a DIFFERENT page instead
otherPage.assertCurrent();
// Option B: check the page ID directly if you need a negative assertion
assertNotEquals("login-login", driver.page().getCurrentPageId());
```

**Important**: `isCurrent()` does not exist in the new framework. All `isCurrent()` calls must be replaced:
- Positive (`assertTrue(page.isCurrent())`) → `page.assertCurrent()`
- Standalone (`page.isCurrent()` with no assertion) → `page.assertCurrent()`
- Negative (`assertFalse(page.isCurrent())`) → assert you're on the expected page instead, or use `driver.page().getCurrentPageId()` for direct comparison

---

## Cookie Management

```java
// OLD
DroneUtils.resetQueue();
deleteAllCookiesForRealm("realmName");
driver.manage().deleteAllCookies();

// NEW
webDriver.cookies().deleteAll();
```

---

## Key Differences

| Aspect | Old Framework | New Framework |
|---|---|---|
| Base class | `AbstractPage` with `@ArquillianResource WebDriver` | `AbstractPage` with constructor `ManagedWebDriver` |
| Initialization | Arquillian Graphene auto-init | Framework `PageSupplier` auto-init |
| Waiting | Static `WaitUtils` methods | Instance `webDriver.waiting()` |
| Current check | `page.isCurrent()` (URL/title check) | `page.getExpectedPageId()` (element ID check) |
