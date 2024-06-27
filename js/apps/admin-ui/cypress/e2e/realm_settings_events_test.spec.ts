import { v4 as uuid } from "uuid";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import RealmSettingsPage from "../support/pages/admin-ui/manage/realm_settings/RealmSettingsPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import adminClient from "../support/util/AdminClient";
import KeysTab from "../support/pages/admin-ui/manage/realm_settings/KeysTab";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const masthead = new Masthead();
const modalUtils = new ModalUtils();
const realmSettingsPage = new RealmSettingsPage();
const keysTab = new KeysTab();

describe("Realm settings events tab tests", () => {
  const realmName = "Realm_" + uuid();
  const listingPage = new ListingPage();

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToRealm(realmName);
  });

  before(async () => {
    await adminClient.createRealm(realmName);
  });

  after(async () => {
    await adminClient.deleteRealm(realmName);
  });

  const goToDetails = () => {
    const keysUrl = `/admin/realms/${realmName}/keys`;
    cy.intercept(keysUrl).as("keysFetch");

    keysTab.goToKeysTab();
    cy.findByTestId("rs-providers-tab").click();
    cy.findAllByTestId("provider-name-link")
      .contains("test_aes-generated")
      .click();

    sidebarPage.goToRealmSettings();

    keysTab.goToKeysTab();
    cy.findByTestId("rs-providers-tab").click();
    cy.findAllByTestId("provider-name-link")
      .contains("test_hmac-generated")
      .click();

    sidebarPage.goToRealmSettings();

    keysTab.goToKeysTab();
    cy.findByTestId("rs-providers-tab").click();
    cy.findAllByTestId("provider-name-link").contains("test_rsa").click();

    sidebarPage.goToRealmSettings();

    keysTab.goToKeysTab();
    cy.findByTestId("rs-providers-tab").click();
    cy.findAllByTestId("provider-name-link")
      .contains("test_rsa-generated")
      .click();

    sidebarPage.goToRealmSettings();

    keysTab.goToKeysTab();
    cy.findByTestId("rs-providers-tab").click();
    cy.findAllByTestId("provider-name-link")
      .contains("test_rsa-enc-generated")
      .click();

    cy.wait(["@keysFetch"]);

    return this;
  };

  const goToKeys = () => {
    const keysUrl = `/admin/realms/${realmName}/keys`;
    cy.intercept(keysUrl).as("keysFetch");
    keysTab.goToKeysTab();
    cy.findByTestId("rs-keys-list-tab").click();
    cy.wait(["@keysFetch"]);

    return this;
  };

  it("Enable user events", () => {
    cy.intercept("GET", `/admin/realms/${realmName}/events/config`).as("load");
    sidebarPage.goToRealmSettings();
    cy.findByTestId("rs-realm-events-tab").click();
    cy.findByTestId("rs-events-tab").click();
    cy.wait("@load");
    realmSettingsPage
      .toggleSwitch(realmSettingsPage.enableEvents, false)
      .save(realmSettingsPage.eventsUserSave);
    masthead.checkNotificationMessage("Successfully saved configuration");
    realmSettingsPage.clearEvents("user");
    modalUtils
      .checkModalMessage(
        "If you clear all events of this realm, all records will be permanently cleared in the database",
      )
      .confirmModal();
    masthead.checkNotificationMessage("The user events have been cleared");
    const events = ["Client info", "Client info error"];
    cy.intercept("GET", `/admin/realms/${realmName}/events/config`).as(
      "fetchConfig",
    );
    realmSettingsPage.addUserEvents(events).clickAdd();
    masthead.checkNotificationMessage("Successfully saved configuration");
    cy.wait(["@fetchConfig"]);
    sidebarPage.waitForPageLoad();
    cy.wait(1000);
    for (const event of events) {
      listingPage.searchItem(event, false).itemExist(event);
    }
  });

  it("Go to keys tab", () => {
    sidebarPage.goToRealmSettings();

    keysTab.goToKeysTab();
  });

  it("add Providers", () => {
    sidebarPage.goToRealmSettings();

    keysTab.goToKeysTab();

    cy.findByTestId("rs-providers-tab").click();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.findByTestId("option-aes-generated").click();
    realmSettingsPage.enterUIDisplayName("test_aes-generated");
    realmSettingsPage.addProvider();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.findByTestId("option-ecdsa-generated").click();
    realmSettingsPage.enterUIDisplayName("test_ecdsa-generated");
    realmSettingsPage.toggleSwitch("active", false);
    realmSettingsPage.addProvider();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.findByTestId("option-hmac-generated").click();
    realmSettingsPage.enterUIDisplayName("test_hmac-generated");
    realmSettingsPage.toggleSwitch("enabled", false);
    realmSettingsPage.addProvider();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.findByTestId("option-rsa-generated").click();
    realmSettingsPage.enterUIDisplayName("test_rsa-generated");
    realmSettingsPage.addProvider();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.findByTestId("option-rsa-enc-generated").click();
    realmSettingsPage.enterUIDisplayName("test_rsa-enc-generated");
    realmSettingsPage.addProvider();
  });

  it("search providers", () => {
    sidebarPage.goToRealmSettings();

    keysTab.goToKeysTab();

    cy.findByTestId("rs-providers-tab").click();

    // search providers
    cy.findByTestId("provider-search-input").type("rsa{enter}");
    listingPage.checkTableLength(4, "kc-draggable-table");
    cy.findByTestId("provider-search-input").clear().type("{enter}");
  });

  it("go to details", () => {
    sidebarPage.goToRealmSettings();
    goToDetails();
  });

  it("Test keys", () => {
    sidebarPage.goToRealmSettings();
    goToKeys();

    realmSettingsPage.testSelectFilter();
  });

  it.skip("Should search active keys", () => {
    sidebarPage.goToRealmSettings();
    goToKeys();

    realmSettingsPage.switchToActiveFilter();
    listingPage.searchItem("rs", false);
    listingPage.checkTableLength(3, "kc-keys-list");
  });

  it("Should search passive keys", () => {
    sidebarPage.goToRealmSettings();
    goToKeys();

    realmSettingsPage.switchToPassiveFilter();
    listingPage.searchItem("ec", false);
    listingPage.checkTableLength(1, "kc-keys-list");
  });

  it("Should search disabled keys", () => {
    sidebarPage.goToRealmSettings();
    goToKeys();

    realmSettingsPage.switchToDisabledFilter();
    listingPage.searchItem("hs", false);
    listingPage.checkTableLength(1, "kc-keys-list");
  });

  it("delete provider", () => {
    sidebarPage.goToRealmSettings();

    keysTab.goToKeysTab();

    cy.findByTestId("rs-providers-tab").click();

    realmSettingsPage.deleteProvider("test_aes-generated");
  });

  it("list keys", () => {
    sidebarPage.goToRealmSettings();
    keysTab.goToKeysTab();
    realmSettingsPage.checkKeyPublic();
  });

  it("Realm header settings", () => {
    sidebarPage.goToRealmSettings();
    cy.findByTestId("rs-security-defenses-tab").click();
    cy.findByTestId("browserSecurityHeaders.xFrameOptions").clear();
    cy.findByTestId("browserSecurityHeaders.xFrameOptions").type("DENY");
    cy.findByTestId("headers-form-tab-save").should("be.enabled").click();

    masthead.checkNotificationMessage("Realm successfully updated");
  });

  it("Brute force detection", () => {
    sidebarPage.goToRealmSettings();
    cy.findAllByTestId("rs-security-defenses-tab").click();
    cy.get("#pf-tab-20-bruteForce").click();

    cy.get("#kc-brute-force-mode").click();
    cy.findByTestId("select-brute-force-mode")
      .contains("Lockout temporarily")
      .click();
    cy.findByTestId("waitIncrementSeconds").type("1");
    cy.findByTestId("maxFailureWaitSeconds").type("1");
    cy.findByTestId("maxDeltaTimeSeconds").type("1");
    cy.findByTestId("minimumQuickLoginWaitSeconds").type("1");

    cy.findByTestId("brute-force-tab-save").should("be.enabled").click();
    masthead.checkNotificationMessage("Realm successfully updated");
  });

  it("add session data", () => {
    sidebarPage.goToRealmSettings();

    cy.findByTestId("rs-sessions-tab").click();

    realmSettingsPage.populateSessionsPage();
    realmSettingsPage.save("sessions-tab-save");

    masthead.checkNotificationMessage("Realm successfully updated");
  });

  it("check that sessions data was saved", () => {
    sidebarPage.goToAuthentication();
    sidebarPage.goToRealmSettings();

    cy.findByTestId("rs-sessions-tab").click();

    cy.findByTestId(realmSettingsPage.ssoSessionIdleInput).should(
      "have.value",
      1,
    );
    cy.findByTestId(realmSettingsPage.ssoSessionMaxInput).should(
      "have.value",
      2,
    );
    cy.findByTestId(realmSettingsPage.ssoSessionIdleRememberMeInput).should(
      "have.value",
      3,
    );
    cy.findByTestId(realmSettingsPage.ssoSessionMaxRememberMeInput).should(
      "have.value",
      4,
    );

    cy.findByTestId(realmSettingsPage.clientSessionIdleInput).should(
      "have.value",
      5,
    );
    cy.findByTestId(realmSettingsPage.clientSessionMaxInput).should(
      "have.value",
      6,
    );

    cy.findByTestId(realmSettingsPage.offlineSessionIdleInput).should(
      "have.value",
      7,
    );
    cy.findByTestId(realmSettingsPage.offlineSessionMaxSwitch).should(
      "have.value",
      "on",
    );

    cy.findByTestId(realmSettingsPage.loginTimeoutInput).should(
      "have.value",
      9,
    );
    cy.findByTestId(realmSettingsPage.loginActionTimeoutInput).should(
      "have.value",
      10,
    );
  });

  it("add token data", () => {
    sidebarPage.goToRealmSettings();

    cy.findByTestId("rs-tokens-tab").click();

    realmSettingsPage.populateTokensPage();
    realmSettingsPage.save("tokens-tab-save");

    masthead.checkNotificationMessage("Realm successfully updated");
  });

  it("check that token data was saved", () => {
    sidebarPage.goToRealmSettings();

    cy.findByTestId("rs-tokens-tab").click();

    cy.findByTestId(realmSettingsPage.accessTokenLifespanInput).should(
      "have.value",
      1,
    );
    cy.findByTestId(realmSettingsPage.parRequestUriLifespanInput).should(
      "have.value",
      2,
    );
    cy.findByTestId(realmSettingsPage.accessTokenLifespanImplicitInput).should(
      "have.value",
      2,
    );
    cy.findByTestId(realmSettingsPage.clientLoginTimeoutInput).should(
      "have.value",
      3,
    );
    cy.findByTestId(realmSettingsPage.userInitiatedActionLifespanInput).should(
      "have.value",
      4,
    );

    cy.findByTestId(realmSettingsPage.defaultAdminInitatedInput).should(
      "have.value",
      5,
    );
    cy.findByTestId(realmSettingsPage.emailVerificationInput).should(
      "have.value",
      6,
    );

    cy.findByTestId(realmSettingsPage.idpEmailVerificationInput).should(
      "have.value",
      7,
    );
    cy.findByTestId(realmSettingsPage.forgotPasswordInput).should(
      "have.value",
      8,
    );

    cy.findByTestId(realmSettingsPage.executeActionsInput).should(
      "have.value",
      9,
    );
  });
});

describe("Realm settings events tab tests", () => {
  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToRealmSettings();
    cy.findByTestId("rs-realm-events-tab").click();
    cy.findByTestId("rs-event-listeners-tab").click();
  });

  it("Should display event listeners form", () => {
    realmSettingsPage.shouldDisplayEventListenersForm();
  });

  it("Should revert saving event listener", () => {
    realmSettingsPage.shouldRevertSavingEventListener();
  });

  it("Should save event listener", () => {
    realmSettingsPage.shouldSaveEventListener();
  });

  it("Should remove event from event listener", () => {
    realmSettingsPage.shouldRemoveEventFromEventListener();
  });

  it("Should remove all events from event listener and re-save original", () => {
    realmSettingsPage.shouldRemoveAllEventListeners();
    realmSettingsPage.shouldReSaveEventListener();
  });
});
