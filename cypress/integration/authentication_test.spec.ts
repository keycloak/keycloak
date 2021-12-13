import { keycloakBefore } from "../support/util/keycloak_before";
import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import DuplicateFlowModal from "../support/pages/admin_console/manage/authentication/DuplicateFlowModal";
import FlowDetails from "../support/pages/admin_console/manage/authentication/FlowDetail";
import RequiredActions from "../support/pages/admin_console/manage/authentication/RequiredActions";
import AdminClient from "../support/util/AdminClient";
import PasswordPolicies from "../support/pages/admin_console/manage/authentication/PasswordPolicies";
import OTPPolicies from "../support/pages/admin_console/manage/authentication/OTPPolicies";

describe("Authentication test", () => {
  const loginPage = new LoginPage();
  const masthead = new Masthead();
  const sidebarPage = new SidebarPage();
  const listingPage = new ListingPage();

  const detailPage = new FlowDetails();

  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToAuthentication();
  });

  it("should create duplicate of existing flow", () => {
    const modalDialog = new DuplicateFlowModal();
    listingPage.clickRowDetails("Browser").clickDetailMenu("Duplicate");
    modalDialog.fill("Copy of browser");

    masthead.checkNotificationMessage("Flow successfully duplicated");
    listingPage.itemExist("Copy of browser");
  });

  it("should show the details of a flow as a table", () => {
    listingPage.goToItemDetails("Copy of browser");

    detailPage.executionExists("Cookie");
  });

  it("should move kerberos down", () => {
    listingPage.goToItemDetails("Copy of browser");

    detailPage.moveRowTo("Kerberos", "Identity Provider Redirector");
  });

  it("should change requirement of cookie", () => {
    listingPage.goToItemDetails("Copy of browser");

    detailPage.changeRequirement("Cookie", "Required");

    masthead.checkNotificationMessage("Flow successfully updated");
  });

  it("should switch to diagram mode", () => {
    listingPage.goToItemDetails("Copy of browser");

    detailPage.goToDiagram();

    cy.get(".react-flow").should("exist");
  });

  it("should add a execution", () => {
    listingPage.goToItemDetails("Copy of browser");
    detailPage.addExecution(
      "Copy of browser forms",
      "console-username-password"
    );

    masthead.checkNotificationMessage("Flow successfully updated");
    detailPage.executionExists("Username Password Challenge");
  });

  it("should add a condition", () => {
    listingPage.goToItemDetails("Copy of browser");
    detailPage.addCondition(
      "Copy of browser Browser - Conditional OTP",
      "conditional-user-role"
    );

    masthead.checkNotificationMessage("Flow successfully updated");
    detailPage.executionExists("Username Password Challenge");
  });

  it("should add a sub-flow", () => {
    const flowName = "SubFlow";
    listingPage.goToItemDetails("Copy of browser");
    detailPage.addSubFlow(
      "Copy of browser Browser - Conditional OTP",
      flowName
    );

    masthead.checkNotificationMessage("Flow successfully updated");
    detailPage.flowExists(flowName);
  });

  it("should create flow from scratch", () => {
    const flowName = "Flow";
    listingPage.goToCreateItem();
    detailPage.fillCreateForm(
      flowName,
      "Some nice description about what this flow does so that we can use it later",
      "Client flow"
    );
    masthead.checkNotificationMessage("Flow created");
    detailPage.addSubFlowToEmpty(flowName, "EmptySubFlow");

    masthead.checkNotificationMessage("Flow successfully updated");

    detailPage.flowExists(flowName);
  });

  describe("Required actions", () => {
    const requiredActionsPage = new RequiredActions();
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToRealm("Test");
      sidebarPage.goToAuthentication();
      requiredActionsPage.goToTab();
    });

    before(() => {
      new AdminClient().createRealm("Test");
    });

    after(() => {
      new AdminClient().deleteRealm("Test");
    });

    it("should enable delete account", () => {
      const action = "Delete Account";
      requiredActionsPage.enableAction(action);
      masthead.checkNotificationMessage("Updated required action successfully");
      requiredActionsPage.isChecked(action);
    });

    it("should register an unregistered action", () => {
      const action = "Verify Profile";
      requiredActionsPage.enableAction(action);
      masthead.checkNotificationMessage("Updated required action successfully");
      requiredActionsPage.isChecked(action).isDefaultEnabled(action);
    });

    it("should set action as default", () => {
      const action = "Configure OTP";
      requiredActionsPage.setAsDefault(action);
      masthead.checkNotificationMessage("Updated required action successfully");
      requiredActionsPage.isDefaultChecked(action);
    });

    it("should reorder required actions", () => {
      const action = "Terms and Conditions";
      requiredActionsPage.moveRowTo(action, "Update Profile");
      masthead.checkNotificationMessage("Updated required action successfully");
    });
  });

  describe("Password policies tab", () => {
    const passwordPoliciesPage = new PasswordPolicies();
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToAuthentication();
      passwordPoliciesPage.goToTab();
    });

    it("should add password policies", () => {
      passwordPoliciesPage
        .shouldShowEmptyState()
        .addPolicy("Not Recently Used")
        .save();
      masthead.checkNotificationMessage(
        "Password policies successfully updated"
      );
    });

    it("should remove password policies", () => {
      passwordPoliciesPage.removePolicy("remove-passwordHistory").save();
      masthead.checkNotificationMessage(
        "Password policies successfully updated"
      );
      passwordPoliciesPage.shouldShowEmptyState();
    });
  });

  describe("OTP policies tab", () => {
    const otpPoliciesPage = new OTPPolicies();
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToAuthentication();
      otpPoliciesPage.goToTab();
    });

    it("should change to hotp", () => {
      otpPoliciesPage.checkSupportedActions("FreeOTP, Google Authenticator");
      otpPoliciesPage.setPolicyType("hotp").increaseInitialCounter().save();
      masthead.checkNotificationMessage("OTP policy successfully updated");
      otpPoliciesPage.checkSupportedActions("FreeOTP");
    });
  });
});
