import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import RealmSettingsPage from "../support/pages/admin_console/manage/realm_settings/RealmSettingsPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_before";
import AdminClient from "../support/util/AdminClient";
import ListingPage from "../support/pages/admin_console/ListingPage";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const masthead = new Masthead();
const modalUtils = new ModalUtils();
const realmSettingsPage = new RealmSettingsPage();

describe("Realm settings tests", () => {
  describe("Realm settings tabs tests", () => {
    const realmName = "Realm_" + (Math.random() + 1).toString(36).substring(7);

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

    const goToKeys = () => {
      const keysUrl = `/auth/admin/realms/${realmName}/keys`;
      cy.intercept(keysUrl).as("keysFetch");
      cy.findByTestId("rs-keys-tab").click();
      cy.findByTestId("rs-keys-list-tab").click();
      cy.wait(["@keysFetch"]);

      return this;
    };

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

    /*const deleteProvider = (providerName: string) => {
    const url = `/auth/admin/realms/${realmName}/users/*`;
    cy.intercept(url).as("reload");
    cy.findByTestId("provider-name")
      .contains(providerName)
      .parentsUntil(".pf-c-data-list__item-row")
      .find(".pf-c-dropdown__toggle")
      .click()
      .findByTestId(realmSettingsPage.deleteAction)
      .click();
    cy.findByTestId(realmSettingsPage.modalConfirm).click();

    cy.wait(["@reload"]);
    return this;
  };*/

    const addBundle = () => {
      const localizationUrl = `/auth/admin/realms/${realmName}/localization/en`;
      cy.intercept(localizationUrl).as("localizationFetch");

      realmSettingsPage.addKeyValuePair(
        "key_" + (Math.random() + 1).toString(36).substring(7),
        "value_" + (Math.random() + 1).toString(36).substring(7)
      );

      cy.wait(["@localizationFetch"]);

      return this;
    };

    it("Go to general tab", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage.toggleSwitch(realmSettingsPage.managedAccessSwitch);
      realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
      masthead.checkNotificationMessage("Realm successfully updated");
      realmSettingsPage.toggleSwitch(realmSettingsPage.managedAccessSwitch);
      realmSettingsPage.save(realmSettingsPage.generalSaveBtn);
      masthead.checkNotificationMessage("Realm successfully updated");
    });

    /* 
    it("Go to login tab", () => {
      sidebarPage.goToRealmSettings();
      cy.findByTestId("rs-login-tab").click();
      realmSettingsPage.toggleSwitch(realmSettingsPage.userRegSwitch);
      realmSettingsPage.toggleSwitch(realmSettingsPage.forgotPwdSwitch);
      realmSettingsPage.toggleSwitch(realmSettingsPage.rememberMeSwitch);
    });

    it("Check login tab values", () => {
      sidebarPage.goToRealmSettings();
      cy.findByTestId("rs-login-tab").click();

      cy.get("#kc-user-reg-switch-off").should("be.visible");
      cy.get("#kc-forgot-pw-switch-off").should("be.visible");
      cy.get("#kc-remember-me-switch-off").should("not.be.visible");
    });
    */

    it("Go to email tab", () => {
      sidebarPage.goToRealmSettings();
      cy.findByTestId("rs-email-tab").click();

      realmSettingsPage.addSenderEmail("example@example.com");

      realmSettingsPage.toggleCheck(realmSettingsPage.enableSslCheck);
      realmSettingsPage.toggleCheck(realmSettingsPage.enableStartTlsCheck);

      realmSettingsPage.save(realmSettingsPage.emailSaveBtn);

      realmSettingsPage.fillHostField("localhost");
      cy.findByTestId(realmSettingsPage.testConnectionButton).click();

      realmSettingsPage.fillEmailField(
        "example" +
          (Math.random() + 1).toString(36).substring(7) +
          "@example.com"
      );
      cy.findByTestId(realmSettingsPage.modalTestConnectionButton).click();

      masthead.checkNotificationMessage("Error! Failed to send email.");
    });

    it("Go to themes tab", () => {
      sidebarPage.goToRealmSettings();
      cy.intercept(`/auth/admin/realms/${realmName}/keys`).as("load");

      cy.findByTestId("rs-themes-tab").click();
      cy.wait(["@load"]);

      realmSettingsPage.selectLoginThemeType("keycloak");
      realmSettingsPage.selectAccountThemeType("keycloak");
      realmSettingsPage.selectAdminThemeType("base");
      realmSettingsPage.selectEmailThemeType("base");

      realmSettingsPage.saveThemes();
    });

    describe("Events tab", () => {
      const listingPage = new ListingPage();

      it("Enable user events", () => {
        cy.intercept("GET", `/auth/admin/realms/${realmName}/keys`).as("load");
        sidebarPage.goToRealmSettings();
        cy.findByTestId("rs-realm-events-tab").click();
        cy.wait(["@load"]);

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

    /*it("delete providers", () => {
        sidebarPage.goToRealmSettings();
        const url = `/auth/admin/realms/${realmName}/keys`;
        cy.intercept(url).as("load");

        cy.findByTestId("rs-keys-tab").click();
        cy.findByTestId("rs-providers-tab").click();

        cy.wait("@load");

        deleteProvider("test_aes-generated");
        deleteProvider("test_ecdsa-generated");
        deleteProvider("test_hmac-generated");
        deleteProvider("test_rsa-generated");
      });*/

    it("Test keys", () => {
      sidebarPage.goToRealmSettings();
      goToKeys();

      realmSettingsPage.testSelectFilter();
    });

    it("add locale", () => {
      sidebarPage.goToRealmSettings();

      cy.findByTestId("rs-localization-tab").click();

      addBundle();

      masthead.checkNotificationMessage(
        "Success! The localization text has been created."
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
      cy.findByTestId(
        realmSettingsPage.accessTokenLifespanImplicitInput
      ).should("have.value", 2);
      cy.findByTestId(realmSettingsPage.clientLoginTimeoutInput).should(
        "have.value",
        3
      );
      cy.findByTestId(
        realmSettingsPage.userInitiatedActionLifespanInput
      ).should("have.value", 4);

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

    describe("Realm settings client profiles tab tests", () => {
      beforeEach(() => {
        keycloakBefore();
        loginPage.logIn();
        sidebarPage.goToRealmSettings();
        cy.findByTestId("rs-clientPolicies-tab").click();
        cy.findByTestId("rs-policies-clientProfiles-tab").click();
      });

      it("Go to client policies profiles tab", () => {
        realmSettingsPage.shouldDisplayProfilesTab();
      });

      it("Check new client form is displaying", () => {
        realmSettingsPage.shouldDisplayNewClientProfileForm();
      });

      it("Complete new client form and cancel", () => {
        realmSettingsPage.shouldCompleteAndCancelCreateNewClientProfile();
      });

      it("Complete new client form and submit", () => {
        realmSettingsPage.shouldCompleteAndCreateNewClientProfile();
      });

      it("Should perform client profile search by profile name", () => {
        realmSettingsPage.shouldSearchClientProfile();
      });

      it("Check cancelling the client profile deletion", () => {
        realmSettingsPage.shouldDisplayDeleteClientProfileDialog();
      });

      it("Check deleting the client profile", () => {
        realmSettingsPage.shouldDeleteClientProfileDialog();
      });

      it("Check navigating between Form View and JSON editor", () => {
        realmSettingsPage.shouldNavigateBetweenFormAndJSONView();
      });

      it("Check saving changed JSON profiles", () => {
        realmSettingsPage.shouldSaveChangedJSONProfiles();
        realmSettingsPage.shouldDeleteClientProfileDialog();
      });

      it("Should not create duplicate client profile", () => {
        sidebarPage.goToRealmSettings();
        cy.findByTestId("rs-clientPolicies-tab").click();
        cy.findByTestId("rs-policies-clientProfiles-tab").click();
        realmSettingsPage.shouldCompleteAndCreateNewClientProfile();
        realmSettingsPage.shouldNotCreateDuplicateClientProfile();
      });

      describe("Realm settings client policies tab tests", () => {
        beforeEach(() => {
          keycloakBefore();
          loginPage.logIn();
          sidebarPage.goToRealmSettings();
          cy.findByTestId("rs-clientPolicies-tab").click();
          cy.findByTestId("rs-policies-clientPolicies-tab").click();
        });

        it("Go to client policies tab", () => {
          realmSettingsPage.shouldDisplayPoliciesTab();
        });

        it("Check new client form is displaying", () => {
          realmSettingsPage.shouldDisplayNewClientPolicyForm();
        });

        it("Complete new client form and cancel", () => {
          realmSettingsPage.shouldCompleteAndCancelCreateNewClientPolicy();
        });

        it("Complete new client form and submit", () => {
          realmSettingsPage.shouldCompleteAndCreateNewClientPolicyFromEmptyState();
        });

        it("Should perform client profile search by profile name", () => {
          realmSettingsPage.shouldSearchClientPolicy();
        });

        it("Check cancelling the client policy deletion", () => {
          realmSettingsPage.shouldDisplayDeleteClientPolicyDialog();
        });

        it("Check deleting the client policy", () => {
          realmSettingsPage.shouldDeleteClientPolicyDialog();
        });

        it("Check navigating between Form View and JSON editor", () => {
          realmSettingsPage.shouldNavigateBetweenFormAndJSONViewPolicies();
        });

        /*       it("Check saving changed JSON policies", () => {
        realmSettingsPage.shouldSaveChangedJSONPolicies();
        realmSettingsPage.shouldDeleteClientPolicyDialog();
      }); */

        it("Should not create duplicate client profile", () => {
          realmSettingsPage.shouldCompleteAndCreateNewClientPolicyFromEmptyState();

          sidebarPage.goToRealmSettings();
          cy.findByTestId("rs-clientPolicies-tab").click();
          cy.findByTestId("rs-policies-clientPolicies-tab").click();
          realmSettingsPage.shouldCompleteAndCreateNewClientPolicy();
          realmSettingsPage.shouldNotCreateDuplicateClientPolicy();

          sidebarPage.goToRealmSettings();
          cy.findByTestId("rs-clientPolicies-tab").click();
          cy.findByTestId("rs-policies-clientPolicies-tab").click();
          realmSettingsPage.shouldDeleteClientProfileDialog();
        });

        it("Check deleting newly created client policy from create view via dropdown", () => {
          realmSettingsPage.shouldRemoveClientPolicyFromCreateView();
        });

        it("Check reloading JSON policies", () => {
          realmSettingsPage.shouldReloadJSONPolicies();
        });
      });
    });
  });
});
