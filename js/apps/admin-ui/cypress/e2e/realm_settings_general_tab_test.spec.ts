import { v4 as uuid } from "uuid";
import { SERVER_URL } from "../support/constants";
import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import RealmSettingsPage from "../support/pages/admin-ui/manage/realm_settings/RealmSettingsPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const masthead = new Masthead();
const realmSettingsPage = new RealmSettingsPage();

describe("Realm settings general tab tests", () => {
  const realmName = "Realm_" + uuid();

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

  it("Test all general tab switches", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.toggleSwitch(
      realmSettingsPage.managedAccessSwitch,
      false,
    );
    realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
    masthead.checkNotificationMessage("Realm successfully updated", true);
    realmSettingsPage.toggleSwitch(
      realmSettingsPage.managedAccessSwitch,
      false,
    );
    realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
    masthead.checkNotificationMessage("Realm successfully updated", true);

    // Enable realm
    realmSettingsPage.toggleSwitch(`${realmName}-switch`);
    masthead.checkNotificationMessage("Realm successfully updated");
    sidebarPage.waitForPageLoad();

    // Disable realm
    realmSettingsPage.toggleSwitch(`${realmName}-switch`, false);
    realmSettingsPage.disableRealm();
    masthead.checkNotificationMessage("Realm successfully updated", true);
    sidebarPage.waitForPageLoad();

    // Re-enable realm
    realmSettingsPage.toggleSwitch(`${realmName}-switch`);
    masthead.checkNotificationMessage("Realm successfully updated");
  });

  it("Fail to set Realm ID to empty", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.clearRealmId();
    realmSettingsPage.saveGeneral();
    cy.findByTestId("realm-id-error").should("have.text", "Required field");
  });

  it("Modify Display name", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.fillDisplayName("display_name");
    realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
    masthead.checkNotificationMessage("Realm successfully updated", true);
  });

  it("Check Display name value", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.getDisplayName("display_name");
  });

  it("Modify front end URL", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.fillFrontendURL("www.example.com");

    // TODO: Fix internal server error 500 when front-end URL is saved
    // realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
    // masthead.checkNotificationMessage("Realm successfully updated", true);

    realmSettingsPage.getFrontendURL("www.example.com");
    realmSettingsPage.clearFrontendURL();
  });

  it("Select SSL all requests", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.fillRequireSSL("All requests");
    realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
    masthead.checkNotificationMessage("Realm successfully updated", true);
  });

  it("Verify SSL all requests displays", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.getRequireSSL("All requests");
  });

  it("Select SSL external requests", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.fillRequireSSL("External requests");
    realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
    masthead.checkNotificationMessage("Realm successfully updated", true);
  });

  it("Verify SSL external requests displays", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.getRequireSSL("External requests");
  });

  it("Select SSL None", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.fillRequireSSL("None");
    realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
    masthead.checkNotificationMessage("Realm successfully updated", true);
  });

  it("Verify SSL None displays", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage.getRequireSSL("None");
  });

  it("Check Access Endpoints OpenID Endpoint Configuration link", () => {
    sidebarPage.goToRealmSettings();

    // Check link exists
    cy.get("a")
      .contains("OpenID Endpoint Configuration")
      .should(
        "have.attr",
        "href",
        `${SERVER_URL}/realms/${realmName}/.well-known/openid-configuration`,
      )
      .should("have.attr", "target", "_blank")
      .should("have.attr", "rel", "noreferrer noopener");
  });

  it("Access Endpoints OpenID Endpoint Configuration link", () => {
    sidebarPage.goToRealmSettings();
    // Check the link is live
    cy.get("a")
      .contains("OpenID Endpoint Configuration")
      .then((link) => {
        cy.request(link.prop("href")).its("status").should("eq", 200);
      });
  });

  it("Check if Access Endpoints SAML 2.0 Identity Provider Metadata link exists", () => {
    sidebarPage.goToRealmSettings();
    cy.get("a")
      .contains("SAML 2.0 Identity Provider Metadata")
      .should(
        "have.attr",
        "href",
        `${SERVER_URL}/realms/${realmName}/protocol/saml/descriptor`,
      )
      .should("have.attr", "target", "_blank")
      .should("have.attr", "rel", "noreferrer noopener");
  });

  it("Access Endpoints SAML 2.0 Identity Provider Metadata link", () => {
    sidebarPage.goToRealmSettings();

    // Check the link is live
    cy.get("a")
      .contains("SAML 2.0 Identity Provider Metadata ")
      .then((link) => {
        cy.request(link.prop("href")).its("status").should("eq", 200);
      });
  });

  it("Verify 'Revert' button works", () => {
    sidebarPage.goToRealmSettings();

    realmSettingsPage.fillDisplayName("should_be_reverted");
    realmSettingsPage.revert(realmSettingsPage.generalRevertBtn);
    realmSettingsPage.getDisplayName("display_name");
  });
});
