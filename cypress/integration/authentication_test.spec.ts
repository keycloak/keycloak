import { keycloakBefore } from "../support/util/keycloak_before";
import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import DuplicateFlowModal from "../support/pages/admin_console/manage/authentication/DuplicateFlowModal";
import FlowDetails from "../support/pages/admin_console/manage/authentication/FlowDetail";

describe("Authentication test", () => {
  const loginPage = new LoginPage();
  const masthead = new Masthead();
  const sidebarPage = new SidebarPage();
  const listingPage = new ListingPage();

  const detailPage = new FlowDetails();

  beforeEach(function () {
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
});
