# Pattern → Solution Index

O(1) lookup: find a legacy pattern, get the exact rule location and solution.

## Annotations & Class Structure

| Legacy Pattern | Solution | Rule File |
|---|---|---|
| `@RunWith(KcArquillian.class)` | Remove → `@KeycloakIntegrationTest` | `core.md` §Imports Cleanup |
| `@RunAsClient` | Remove (default in new framework) | `core.md` §Imports Cleanup |
| `extends AbstractKeycloakTest` | Remove. Add `@InjectRealm`, `@InjectAdminClient` | `core.md` §Base Class Resolution |
| `extends AbstractTestRealmKeycloakTest` | Remove. Convert `configureTestRealm()` to `RealmConfig` | `core.md` §Base Class Resolution |
| `extends AbstractAdminTest` | Check for `AbstractRealmTest` in `tests/`. Extend or flatten | `core.md` §Base Class Resolution |
| `extends AbstractAuthenticationTest` | Check for equivalent in `tests/`. Extend or flatten | `core.md` §Base Class Resolution |
| `extends Abstract*Test` (any other) | Search `tests/base/` for equivalent, else flatten | `specs/base-class-flattening.md` |
| `@EnableFeature(Feature.X)` | `KeycloakServerConfig` → `config.features(Profile.Feature.X)` | `server-and-registration.md` §Feature Flags |
| `@DisableFeature(Feature.X)` | `KeycloakServerConfig` → `config.featuresDisabled(Profile.Feature.X)` | `server-and-registration.md` §Feature Flags |
| `@EnableVault` | `KeycloakServerConfig` → `config.option("vault", "file").option("vault-dir", path)` | `core.md` §Annotations to Remove |
| `@SetDefaultProvider(spi, providerId)` | `KeycloakServerConfig` → `config.option("spi-<spi>-provider", "providerId")` | `server-and-registration.md` §SetDefaultProvider |
| `@FixMethodOrder(NAME_ASCENDING)` | `@TestMethodOrder(OrderAnnotation.class)` + `@Order(n)` | `lifecycle.md` §Test Ordering |
| `@Ignore("reason")` | `@Disabled("reason")` | `core.md` §JUnit 4 → JUnit 6 |
| `@UncaughtServerErrorExpected` | Remove. Handle errors explicitly | `lifecycle.md` §UncaughtServerErrorExpected |
| `@IgnoreBrowserDriver(FirefoxDriver.class)` | `Assumptions.assumeTrue(webDriver.getBrowserType() != BrowserType.FIREFOX)` | `lifecycle.md` §IgnoreBrowserDriver |
| `@ModelTest` | `@TestOnServer` (preferred) or `RunOnServerClient` | `server-and-registration.md` §ModelTest |

## Injection & Fields

| Legacy Pattern | Solution | Rule File |
|---|---|---|
| `@ArquillianResource OAuthClient oauth` | `@InjectOAuthClient OAuthClient oauthClient` | `oauth.md` §OAuthClient Injection |
| `@Drone WebDriver driver` | `@InjectWebDriver ManagedWebDriver webDriver` | `webdriver.md` §WebDriver Injection |
| `@SecondBrowser @Drone WebDriver driver2` | `@InjectWebDriver(ref = "second") ManagedWebDriver webDriver2` | `webdriver.md` §Multiple WebDriver |
| `@Page LoginPage loginPage` | `@InjectPage LoginPage loginPage` | `webdriver.md` §Page Object Injection |
| `@Rule AssertEvents events = new AssertEvents(this)` | `@InjectEvents Events events` | `events.md` §Login Event Assertions |
| `@Rule AssertAdminEvents` | `@InjectAdminEvents AdminEvents adminEvents` | `events.md` §Admin Event Assertions |
| `@Rule GreenMailRule greenMail` | `@InjectMailServer MailServer mailServer` | `utilities.md` §Email Testing |
| `@Rule TokenUtil tokenUtil` | Remove. Use `oauthClient.client("direct-grant","password").doPasswordGrantRequest(u,p).getAccessToken()` | `utilities.md` §TokenUtil |
| `@ClassRule CryptoInitRule` | `@InjectCryptoHelper CryptoHelper cryptoHelper` | `utilities.md` §CryptoInitRule |
| `adminClient` (inherited field) | `@InjectAdminClient Keycloak adminClient` | `core.md` §Resource Injection |
| `testingClient` (inherited field) | Remove. See testingClient section below | `server-and-registration.md` §testingClient |
| `@ArquillianResource SuiteContext` | `@InjectKeycloakUrls KeycloakUrls keycloakUrls` | `server-and-registration.md` §Server URL |

## Realm & Config

| Legacy Pattern | Solution | Rule File |
|---|---|---|
| `addTestRealms(List<RealmRepresentation>)` | `@InjectRealm(config = MyConfig.class)` with `RealmConfig` | `core.md` §Realm Configuration |
| `configureTestRealm(RealmRepresentation)` | Extract config into `RealmConfig.configure()` | `core.md` §Realm Configuration |
| `loadJson(resource)` (testrealm.json) | Prefer minimal `RealmConfig`. Only `fromJson` if many users/OTP | `specs/testrealm-json.md` |
| `adminClient.realm("test")` | `realm.admin()` | `core.md` §Admin Client Access |
| `RealmBuilder.create()` | `RealmConfigBuilder` inside `RealmConfig.configure()` | `core.md` §Builder → Config |
| `ClientBuilder.create()` | `ClientConfigBuilder` or `realm.addClient()` | `core.md` §Builder → Config |
| `UserBuilder.create()` | `UserConfigBuilder` or `realm.addUser()` | `core.md` §Builder → Config |
| `RoleBuilder.create()` | `RoleConfigBuilder` or `realm.addRole()` / `realm.roles()` | `core.md` §Builder → Config |
| `GroupBuilder.create()` | `GroupConfigBuilder` or `realm.addGroup()` | `core.md` §Builder → Config |
| `RolesBuilder.create()` | `realm.roles("a", "b")` or `realm.clientRoles("client", "r1")` | `core.md` §Builder → Config |
| `realm.setXxx(value)` (RealmRepresentation) | Check `core.md` §RealmConfigBuilder Method Reference table | `core.md` §RealmConfigBuilder |

## OAuthClient

| Legacy Pattern | Solution | Rule File |
|---|---|---|
| `oauth.doAccessTokenRequest(code)` | `oauthClient.doAccessTokenRequest(code)` — same API, rename field | `oauth.md` §Method Reference |
| `oauth.doLogin("user", "pass")` | `oauthClient.doLogin("user", "pass")` | `oauth.md` §Method Reference |
| `oauth.clientId("x")` | `oauthClient.client("x")` (`clientId()` is deprecated) | `oauth.md` §Key Differences |
| `oauth.init()` | Remove — supplier handles initialization | `oauth.md` §Key Differences |
| `oauth.setDriver(driver)` / `oauth.driver(driver)` | Remove — supplier wires the WebDriver | `oauth.md` §Key Differences |
| `oauth.newConfig().driver(driver2)` | `@InjectOAuthClient(webDriverRef = "second", ref = "second")` | `oauth.md` §Key Differences |
| `OAuthClient.AUTH_SERVER_ROOT` | `keycloakUrls.getBaseUrl().toString()` | `oauth.md` §Key Differences |
| `OAuthClient.SERVER_ROOT` / `APP_ROOT` | `keycloakUrls.getBaseUrl().toString()` / Remove | `oauth.md` §Key Differences |

## Events

| Legacy Pattern | Solution | Rule File |
|---|---|---|
| `events.expectLogin()...assertEvent()` | `EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN)` | `events.md` §Common migration mappings |
| `events.expectLogin().error(err)` | `EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(err)` | `events.md` §Common migration mappings |
| `events.expectCodeToToken(codeId, sessionId)` | `EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN)` | `events.md` §Common migration mappings |
| `events.expectLogout(sessionId)` | `EventAssertion.assertSuccess(events.poll()).type(EventType.LOGOUT)` | `events.md` §Common migration mappings |
| `.user(userId).assertEvent()` | `.userId(userId)` | `events.md` §Common migration mappings |
| `.user(userRep).assertEvent()` | `.userId(userRep.getId())` — String only | `events.md` §Common migration mappings |
| `.detail(key, value)` | `.details(key, value)` | `events.md` §Common migration mappings |
| `.removeDetail(key)` | `.withoutDetails(key)` | `events.md` §Common migration mappings |
| `assertAdminEvents.expectCreate(ResourceType.X)` | `AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.CREATE).resourceType(ResourceType.X)` | `events.md` §Admin Event |
| `events.clear()` / `events.assertEmpty()` | `events.clear()` / `events.skipAll()` | `events.md` §Events API |

## WebDriver & Pages

| Legacy Pattern | Solution | Rule File |
|---|---|---|
| `loginPage.login("user", "pass")` | `loginPage.fillLogin("user", "pass"); loginPage.submit();` | `webdriver.md` §Login methods |
| `loginPage.open()` | `oauthClient.openLoginForm()` | `webdriver.md` §Page Navigation |
| `Assert.assertTrue(page.isCurrent())` | `page.assertCurrent()` | `webdriver.md` §Page Assertions |
| `driver.navigate().to(url)` | `webDriver.open(url)` | `webdriver.md` §Common Driver Migrations |
| `driver.getCurrentUrl()` | `webDriver.getCurrentUrl()` | `webdriver.md` §Common Driver Migrations |
| `driver.manage().deleteAllCookies()` | `webDriver.cookies().deleteAll()` | `webdriver.md` §Cookie Management |
| `driver.getPageSource()` | `webDriver.page().getPageSource()` | `webdriver.md` §Common Driver Migrations |
| `WaitUtils.waitForPageToLoad()` | `webDriver.waiting().waitForPage(page)` | `webdriver.md` §WaitUtils |

## Lifecycle & Cleanup

| Legacy Pattern | Solution | Rule File |
|---|---|---|
| `getCleanup().addCleanup(() -> ...)` | `realm.cleanup().add(r -> r.xxx())` | `lifecycle.md` §Cleanup Patterns |
| `@After` deleting users/clients/realms | Remove — managed objects auto-cleanup | `lifecycle.md` §@Before/@After |
| Modify realm + reset in `@After` | `realm.updateWithCleanup(r -> r.xxx())` | `lifecycle.md` §Modify-and-reset |
| `setTimeOffset(n)` / `resetTimeOffset()` | `@InjectTimeOffSet TimeOffSet` → `timeOffSet.set(n)`. Remove reset | `lifecycle.md` §Time Manipulation |
| `Assume.assumeTrue(cond)` | `Assumptions.assumeTrue(cond)` | `lifecycle.md` §Conditional Execution |
| `ProfileAssume.assumeFeatureEnabled(X)` | `KeycloakServerConfig` to enable feature, or `Assumptions` | `lifecycle.md` §Conditional Execution |

## testingClient

| Legacy Pattern | Solution | Rule File |
|---|---|---|
| `testingClient.testing().setTimeOffset(...)` | `@InjectTimeOffSet TimeOffSet` → `timeOffSet.set(n)` | `utilities.md` §testingClient |
| `testingClient.testing().pollEvent()` | `@InjectEvents Events` → `events.poll()` | `utilities.md` §testingClient |
| `testingClient.testing().pollAdminEvent()` | `@InjectAdminEvents AdminEvents` → `adminEvents.poll()` | `utilities.md` §testingClient |
| `testingClient.testing().clearEventQueue()` | `events.clear()` | `utilities.md` §testingClient |
| `testingClient.testing().removeUserSessions()` | `RunOnServerClient` or admin API | `utilities.md` §testingClient |
| `testingClient.server().run(session -> ...)` | `@InjectRunOnServer RunOnServerClient` → `runOnServer.run(session -> ...)` | `server-and-registration.md` §testingClient |
| `testingClient.server().fetch(session -> ..., T.class)` | `runOnServer.fetch(session -> ..., T.class)` | `server-and-registration.md` §testingClient |
| `testingClient.testApp()` | `@InjectTestApp TestApp testApp` (rarely needed explicitly) | `server-and-registration.md` §testingClient |

## Utility Classes

| Legacy Pattern | Solution | Rule File |
|---|---|---|
| `o.k.testsuite.Assert` | `o.k.tests.utils.Assert` | `utilities.md` §Assert |
| `o.k.testsuite.admin.ApiUtil` | `o.k.tests.utils.admin.AdminApiUtil` | `utilities.md` §ApiUtil |
| `ApiUtil.getCreatedId(response)` | `o.k.testframework.util.ApiUtil.getCreatedId(response)` | `utilities.md` §ApiUtil |
| `SimpleHttpDefault.doGet(url, httpClient)` | `@InjectSimpleHttp SimpleHttp` → `simpleHttp.doGet(url)` | `utilities.md` §SimpleHttpDefault |
| `TokenUtil.getToken()` | `oauthClient.client("direct-grant","password").doPasswordGrantRequest(u,p).getAccessToken()` | `utilities.md` §TokenUtil |
| `AdminClientUtil.createAdminClient()` | `@InjectAdminClient Keycloak adminClient` | `utilities.md` §AdminClientUtil |
| `ClientManager.realm().clientId().addRedirectUris()` | Direct API: `realm.admin().clients().get(id).update(rep)` | `utilities.md` §ClientManager |
| `AccountHelper.*` | Same import — in `utils-shared` | `utilities.md` §AccountHelper |
| `FlowUtil.*` | Same import — in `utils-shared` | `utilities.md` §FlowUtil |
