# Page Object Catalog

## Available in New Framework

Location: `test-framework/ui/src/main/java/org/keycloak/testframework/ui/page/`

| New Page | Old Equivalent | Key Methods |
|---|---|---|
| `LoginPage` | `LoginPage` | `fillLogin(u,p)`, `submit()`, `getError()`, `getUsernameInputError()`, `findSocialButton(alias)`, `resetPassword()` |
| `LoginUsernamePage` | `LoginUsernameOnlyPage` | `fillLoginWithUsernameOnly(u)`, `submit()` |
| `PasswordPage` | `PasswordPage` | `fillPassword(p)`, `submit()`, `getError()`, `clickTryAnotherWayLink()` |
| `ErrorPage` | `ErrorPage` | `assertCurrent()` |
| `LoginPasswordResetPage` | `LoginPasswordResetPage` | `assertCurrent()` |
| `LoginPasswordUpdatePage` | `LoginPasswordUpdatePage` | `assertCurrent()` |
| `LoginExpiredPage` | `LoginExpiredPage` | `assertCurrent()` |
| `RegisterPage` | `RegisterPage` | `assertCurrent()` |
| `ConsentPage` | `ConsentPage` | `assertCurrent()` |
| `OAuthGrantPage` | `OAuthGrantPage` | `assertCurrent()` |
| `SelectAuthenticatorPage` | `SelectAuthenticatorPage` | `assertCurrent()` |
| `TermsAndConditionsPage` | `TermsAndConditionsPage` | `assertCurrent()` |
| `InfoPage` | `InfoPage` | `assertCurrent()` |
| `LogoutConfirmPage` | `LogoutConfirmPage` | `assertCurrent()` |
| `ProceedPage` | `ProceedPage` | `assertCurrent()` |
| `WelcomePage` | `WelcomePage` | `assertCurrent()` |
| `AdminPage` | — | `assertCurrent()` |
| `OID4VCCredentialOfferPage` | — | OID4VC credential offer page |

**Base classes** (not injected directly):
- `AbstractPage` — base for all pages, provides `driver` field, `assertCurrent()`, `getExpectedPageId()`
- `AbstractLoginPage` — extends AbstractPage, adds language selection, attempted username

---

## NOT in New Framework (create during migration)

Sorted by usage frequency in legacy tests. Create as part of migration — see `rules/webdriver.md` "Creating New Page Objects".

### High Priority (used by 10+ legacy tests)

| Old Page | Usage | `data-page-id` | Notes |
|---|---|---|---|
| `AppPage` | 90 tests | N/A | Not a login page — it's the test app redirect page. Most usages can be replaced with `oauthClient.parseLoginResponse()` |
| `LoginTotpPage` | 23 tests | `login-login-otp` | OTP login form with credential selector |
| `LoginPasswordUpdatePage` | 22 tests | `login-login-update-password` | Already exists in framework — verify methods |
| `RegisterPage` | 22 tests | `login-register` | Already exists — verify methods |
| `OAuthGrantPage` | 19 tests | `login-login-oauth-grant` | Already exists — verify methods |
| `LoginConfigTotpPage` | 15 tests | `login-login-config-totp` | TOTP setup/configuration page |
| `LoginPasswordResetPage` | 13 tests | `login-login-reset-password` | Already exists — verify methods |
| `InfoPage` | 13 tests | `login-info` | Already exists — verify methods |
| `LogoutConfirmPage` | 12 tests | `login-logout-confirm` | Already exists — verify methods |
| `LoginUsernameOnlyPage` | 12 tests | `login-login-username` | Maps to `LoginUsernamePage` (already exists) |
| `PasswordPage` | 10 tests | `login-login-password` | Already exists |
| `LoginUpdateProfilePage` | 10 tests | `login-login-update-profile` | Update profile during login |

### Medium Priority (used by 3-9 legacy tests)

| Old Page | Usage | `data-page-id` | Notes |
|---|---|---|---|
| `OneTimeCode` | 7 tests | `login-login-otp` | Same page-id as LoginTotpPage, different methods (isOtpLabelPresent, sendCode, getInputError) |
| `VerifyEmailPage` | 7 tests | `login-login-verify-email` | |
| `SelectAuthenticatorPage` | 6 tests | `login-select-authenticator` | Already exists — verify methods |
| `UpdateAccountInformationPage` | 5 tests | `login-login-update-profile` | Similar to LoginUpdateProfilePage |
| `LoginExpiredPage` | 4 tests | `login-login-page-expired` | Already exists |
| `TermsAndConditionsPage` | 4 tests | `login-login-terms-and-conditions` | Already exists |
| `VerifyProfilePage` | 3 tests | `login-login-update-profile` | |
| `IdpConfirmLinkPage` | 3 tests | `login-login-idp-link-confirm` | IdP account linking |

### Low Priority (used by ≤2 legacy tests)

`ConsentPage` (2), `ProceedPage` (2), `DeleteCredentialPage`, `EmailUpdatePage`, `EnterRecoveryAuthnCodePage`, `IdpConfirmOverrideLinkPage`, `IdpLinkActionPage`, `IdpLinkEmailPage`, `InstalledAppRedirectPage`, `LogoutSessionsPage`, `OAuth2DeviceVerificationPage`, `PushTheButtonPage`, `ResetOtpPage`, `SelectOrganizationPage`, `SetupRecoveryAuthnCodesPage`

---

## Name Mapping Quirks

| Old Name | New Name | Notes |
|---|---|---|
| `LoginUsernameOnlyPage` | `LoginUsernamePage` | Already exists |
| `OneTimeCode` | `OneTimeCodePage` | Must add `Page` suffix |
| `AppPage` | — | Replace with `oauthClient.parseLoginResponse()` |
