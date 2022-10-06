import { keycloakBefore } from "../support/util/keycloak_hooks";
import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import DuplicateFlowModal from "../support/pages/admin_console/manage/authentication/DuplicateFlowModal";
import FlowDetails from "../support/pages/admin_console/manage/authentication/FlowDetail";
import RequiredActions from "../support/pages/admin_console/manage/authentication/RequiredActions";
import adminClient from "../support/util/AdminClient";
import PasswordPolicies from "../support/pages/admin_console/manage/authentication/PasswordPolicies";
import ModalUtils from "../support/util/ModalUtils";
import CommonPage from "../support/pages/CommonPage";
import BindFlowModal from "../support/pages/admin_console/manage/authentication/BindFlowModal";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const commonPage = new CommonPage();
const listingPage = new ListingPage();

describe("Authentication test", () => {
  const detailPage = new FlowDetails();
  const duplicateFlowModal = new DuplicateFlowModal();
  const modalUtil = new ModalUtils();

  before(() => {
    cy.wrap(adminClient.createRealm("test"));
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToRealm("test");
  });

  after(() => adminClient.deleteRealm("test"));

  beforeEach(() => {
    sidebarPage.goToAuthentication();
  });

  it("authentication empty search test", () => {
    commonPage.tableToolbarUtils().searchItem("", false);
    commonPage.tableUtils().checkIfExists(true);
  });

  it("authentication search flow", () => {
    const itemId = "Browser";
    commonPage.tableToolbarUtils().searchItem(itemId, false);
    commonPage.tableUtils().checkRowItemExists(itemId);
  });

  it("should create duplicate of existing flow", () => {
    listingPage.clickRowDetails("Browser").clickDetailMenu("Duplicate");
    duplicateFlowModal.fill("Copy of browser");

    masthead.checkNotificationMessage("Flow successfully duplicated");
    detailPage.flowExists("Copy of browser");
  });

  it("Should fail duplicate with empty flow name", () => {
    listingPage.clickRowDetails("Browser").clickDetailMenu("Duplicate");
    duplicateFlowModal.fill().shouldShowError("Required field");
    modalUtil.cancelModal();
  });

  it("Should fail duplicate with duplicated name", () => {
    listingPage.clickRowDetails("Browser").clickDetailMenu("Duplicate");
    duplicateFlowModal.fill("browser");
    masthead.checkNotificationMessage(
      "Could not duplicate flow: New flow alias name already exists"
    );
  });

  it("Should show the details of a flow as a table", () => {
    listingPage.goToItemDetails("Copy of browser");

    detailPage.executionExists("Cookie");
  });

  it("Should move kerberos down", () => {
    listingPage.goToItemDetails("Copy of browser");

    const fromRow = "Kerberos";
    detailPage.expectPriorityChange(fromRow, () => {
      detailPage.moveRowTo(
        fromRow,
        `[data-testid="Identity Provider Redirector"]`
      );
    });
  });

  it("Should edit flow details", () => {
    const name = "Copy of browser";
    listingPage.goToItemDetails(name);
    const commonPage = new CommonPage();

    commonPage
      .actionToolbarUtils()
      .clickActionToggleButton()
      .clickDropdownItem("Edit info");

    duplicateFlowModal.fill(name, "Other description");
    masthead.checkNotificationMessage("Flow successfully updated");
  });

  it("Should change requirement of cookie", () => {
    listingPage.goToItemDetails("Copy of browser");

    detailPage.changeRequirement("Cookie", "Required");

    masthead.checkNotificationMessage("Flow successfully updated");
  });

  it("Should switch to diagram mode", () => {
    listingPage.goToItemDetails("Copy of browser");

    detailPage.goToDiagram();

    cy.get(".react-flow").should("exist");
  });

  it("Should add a execution", () => {
    listingPage.goToItemDetails("Copy of browser");
    detailPage.addExecution(
      "Copy of browser forms",
      "reset-credentials-choose-user"
    );

    masthead.checkNotificationMessage("Flow successfully updated");
    detailPage.executionExists("Choose User");
  });

  it("should add a condition", () => {
    listingPage.goToItemDetails("Copy of browser");
    detailPage.addCondition(
      "Copy of browser Browser - Conditional OTP",
      "conditional-user-role"
    );

    masthead.checkNotificationMessage("Flow successfully updated");
  });

  it("Should add a sub-flow", () => {
    const flowName = "SubFlow";
    listingPage.goToItemDetails("Copy of browser");
    detailPage.addSubFlow(
      "Copy of browser Browser - Conditional OTP",
      flowName
    );

    masthead.checkNotificationMessage("Flow successfully updated");
    detailPage.flowExists(flowName);
  });

  it("Should remove an execution", () => {
    listingPage.goToItemDetails("Copy of browser");
    detailPage.executionExists("Cookie").clickRowDelete("Cookie");
    modalUtil.confirmModal();
    detailPage.executionExists("Cookie", false);
  });

  it("Should set as default in action menu", () => {
    const bindFlow = new BindFlowModal();
    listingPage.clickRowDetails("Copy of browser").clickDetailMenu("Bind flow");
    bindFlow.fill("Direct grant flow").save();
    masthead.checkNotificationMessage("Flow successfully updated");
  });

  const flowName = "Empty Flow";
  it("should create flow from scratch", () => {
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

  it("Should delete a flow from action menu", () => {
    listingPage.clickRowDetails(flowName).clickDetailMenu("Delete");
    modalUtil.confirmModal();
    masthead.checkNotificationMessage("Flow successfully deleted");
  });
});

describe("Required actions", () => {
  const requiredActionsPage = new RequiredActions();

  before(() => {
    cy.wrap(adminClient.createRealm("test"));
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToRealm("test");
  });

  beforeEach(() => {
    sidebarPage.goToAuthentication();
    requiredActionsPage.goToTab();
  });

  after(() => adminClient.deleteRealm("test"));

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
    masthead.checkNotificationMessage("Password policies successfully updated");
  });

  it("should remove password policies", () => {
    passwordPoliciesPage.removePolicy("remove-passwordHistory").save();
    masthead.checkNotificationMessage("Password policies successfully updated");
    passwordPoliciesPage.shouldShowEmptyState();
  });
});
