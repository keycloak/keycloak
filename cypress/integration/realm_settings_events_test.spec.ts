import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import RealmSettingsPage from "../support/pages/admin_console/manage/realm_settings/RealmSettingsPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_before";
import ListingPage from "../support/pages/admin_console/ListingPage";
import AdminClient from "../support/util/AdminClient";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const masthead = new Masthead();
const modalUtils = new ModalUtils();
const realmSettingsPage = new RealmSettingsPage();

describe("Realm settings events tab tests", () => {
  const realmName = "Realm_" + (Math.random() + 1).toString(36).substring(7);
  const listingPage = new ListingPage();

  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToRealm(realmName);
  });

  before(async () => {
    await new AdminClient().createRealm(realmName);
  });

  after(async () => {
    await new AdminClient().deleteRealm(realmName);
  });

  const goToDetails = () => {
    const keysUrl = `/auth/admin/realms/${realmName}/keys`;
    cy.intercept(keysUrl).as("keysFetch");

    cy.findByTestId("rs-keys-tab").click();
    cy.findByTestId("rs-providers-tab").click();
    cy.findAllByTestId("provider-name-link")
      .contains("test_aes-generated")
      .click();

    sidebarPage.goToRealmSettings();

    cy.findByTestId("rs-keys-tab").click();
    cy.findByTestId("rs-providers-tab").click();
    cy.findAllByTestId("provider-name-link")
      .contains("test_hmac-generated")
      .click();

    sidebarPage.goToRealmSettings();

    cy.findByTestId("rs-keys-tab").click();
    cy.findByTestId("rs-providers-tab").click();
    cy.findAllByTestId("provider-name-link").contains("test_rsa").click();

    sidebarPage.goToRealmSettings();

    cy.findByTestId("rs-keys-tab").click();
    cy.findByTestId("rs-providers-tab").click();
    cy.findAllByTestId("provider-name-link")
      .contains("test_rsa-generated")
      .click();

    cy.wait(["@keysFetch"]);

    return this;
  };

  const goToKeys = () => {
    const keysUrl = `/auth/admin/realms/${realmName}/keys`;
    cy.intercept(keysUrl).as("keysFetch");
    cy.findByTestId("rs-keys-tab").click();
    cy.findByTestId("rs-keys-list-tab").click();
    cy.wait(["@keysFetch"]);

    return this;
  };

  const addBundle = () => {
    realmSettingsPage.addKeyValuePair(
      "key_" + (Math.random() + 1).toString(36).substring(7),
      "value_" + (Math.random() + 1).toString(36).substring(7)
    );

    return this;
  };

  it("Enable user events", () => {
    cy.intercept("GET", `/auth/admin/realms/${realmName}/events/config`).as(
      "load"
    );
    sidebarPage.goToRealmSettings();
    cy.findByTestId("rs-realm-events-tab").click();
    cy.findByTestId("rs-events-tab").click();
    cy.wait("@load");
    realmSettingsPage
      .toggleSwitch(realmSettingsPage.enableEvents)
      .save(realmSettingsPage.eventsUserSave);
    masthead.checkNotificationMessage("Successfully saved configuration");
    realmSettingsPage.clearEvents("user");
    modalUtils
      .checkModalMessage(
        "If you clear all events of this realm, all records will be permanently cleared in the database"
      )
      .confirmModal();
    masthead.checkNotificationMessage("The user events have been cleared");
    const events = ["Client info", "Client info error"];
    cy.intercept("GET", `/auth/admin/realms/${realmName}/events/config`).as(
      "fetchConfig"
    );
    realmSettingsPage.addUserEvents(events).clickAdd();
    masthead.checkNotificationMessage("Successfully saved configuration");
    cy.wait(["@fetchConfig"]);
    sidebarPage.waitForPageLoad();
    for (const event of events) {
      listingPage.searchItem(event, false).itemExist(event);
    }
  });

  it("Go to keys tab", () => {
    sidebarPage.goToRealmSettings();

    cy.findByTestId("rs-keys-tab").click();
  });

  it("add Providers", () => {
    sidebarPage.goToRealmSettings();

    cy.findByTestId("rs-keys-tab").click();

    cy.findByTestId("rs-providers-tab").click();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.findByTestId("option-aes-generated").click();
    realmSettingsPage.enterConsoleDisplayName("test_aes-generated");
    realmSettingsPage.addProvider();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.findByTestId("option-ecdsa-generated").click();
    realmSettingsPage.enterConsoleDisplayName("test_ecdsa-generated");
    realmSettingsPage.addProvider();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.findByTestId("option-hmac-generated").click();
    realmSettingsPage.enterConsoleDisplayName("test_hmac-generated");
    realmSettingsPage.addProvider();

    realmSettingsPage.toggleAddProviderDropdown();

    cy.findByTestId("option-rsa-generated").click();
    realmSettingsPage.enterConsoleDisplayName("test_rsa-generated");
    realmSettingsPage.addProvider();
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

  it("add locale", () => {
    sidebarPage.goToRealmSettings();

    cy.findByTestId("rs-localization-tab").click();

    cy.findByTestId("internationalization-disabled").click({ force: true });

    cy.get(realmSettingsPage.supportedLocalesTypeahead)
      .click()
      .get(".pf-c-select__menu-item")
      .contains("Dansk")
      .click();

    cy.get("#kc-l-supported-locales").click();

    cy.findByTestId("localization-tab-save").click();

    cy.findByTestId("add-bundle-button").click({ force: true });

    addBundle();

    masthead.checkNotificationMessage(
      "Success! The message bundle has been added."
    );
  });

  it("Realm header settings", () => {
    sidebarPage.goToRealmSettings();
    cy.get("#pf-tab-securityDefences-securityDefences").click();
    cy.findByTestId("headers-form-tab-save").should("be.disabled");
    cy.get("#xFrameOptions").clear().type("DENY");
    cy.findByTestId("headers-form-tab-save").should("be.enabled").click();

    masthead.checkNotificationMessage("Realm successfully updated");
  });

  it("Brute force detection", () => {
    sidebarPage.goToRealmSettings();
    cy.get("#pf-tab-securityDefences-securityDefences").click();
    cy.get("#pf-tab-20-bruteForce").click();

    cy.findByTestId("brute-force-tab-save").should("be.disabled");

    cy.get("#bruteForceProtected").click({ force: true });
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
      1
    );
    cy.findByTestId(realmSettingsPage.ssoSessionMaxInput).should(
      "have.value",
      2
    );
    cy.findByTestId(realmSettingsPage.ssoSessionIdleRememberMeInput).should(
      "have.value",
      3
    );
    cy.findByTestId(realmSettingsPage.ssoSessionMaxRememberMeInput).should(
      "have.value",
      4
    );

    cy.findByTestId(realmSettingsPage.clientSessionIdleInput).should(
      "have.value",
      5
    );
    cy.findByTestId(realmSettingsPage.clientSessionMaxInput).should(
      "have.value",
      6
    );

    cy.findByTestId(realmSettingsPage.offlineSessionIdleInput).should(
      "have.value",
      7
    );
    cy.findByTestId(realmSettingsPage.offlineSessionMaxSwitch).should(
      "have.value",
      "on"
    );

    cy.findByTestId(realmSettingsPage.loginTimeoutInput).should(
      "have.value",
      9
    );
    cy.findByTestId(realmSettingsPage.loginActionTimeoutInput).should(
      "have.value",
      10
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
      1
    );
    cy.findByTestId(realmSettingsPage.accessTokenLifespanImplicitInput).should(
      "have.value",
      2
    );
    cy.findByTestId(realmSettingsPage.clientLoginTimeoutInput).should(
      "have.value",
      3
    );
    cy.findByTestId(realmSettingsPage.userInitiatedActionLifespanInput).should(
      "have.value",
      4
    );

    cy.findByTestId(realmSettingsPage.defaultAdminInitatedInput).should(
      "have.value",
      5
    );
    cy.findByTestId(realmSettingsPage.emailVerificationInput).should(
      "have.value",
      6
    );

    cy.findByTestId(realmSettingsPage.idpEmailVerificationInput).should(
      "have.value",
      7
    );
    cy.findByTestId(realmSettingsPage.forgotPasswordInput).should(
      "have.value",
      8
    );

    cy.findByTestId(realmSettingsPage.executeActionsInput).should(
      "have.value",
      9
    );
  });
});

describe("Realm settings events tab tests", () => {
  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
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
    realmSettingsPage.shouldSaveEventListener();
    realmSettingsPage.shouldRemoveAllEventListeners();
    realmSettingsPage.shouldReSaveEventListener();
  });
});
