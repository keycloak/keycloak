import ListingPage from "../support/pages/admin-ui/ListingPage";
import { ClientRegistrationPage } from "../support/pages/admin-ui/manage/clients/ClientRegistrationPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";

describe("Client registration policies subtab", () => {
  const loginPage = new LoginPage();
  const listingPage = new ListingPage();
  const masthead = new Masthead();
  const sidebarPage = new SidebarPage();
  const clientRegistrationPage = new ClientRegistrationPage();

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToClients();
    clientRegistrationPage.goToClientRegistrationTab();
    sidebarPage.waitForPageLoad();
  });

  it("add anonymous client registration policy", () => {
    clientRegistrationPage
      .createPolicy()
      .selectRow("max-clients")
      .fillPolicyForm({
        name: "new policy",
      })
      .formUtils()
      .save();

    masthead.checkNotificationMessage("New client policy created successfully");
    clientRegistrationPage.formUtils().cancel();
    listingPage.itemExist("new policy");
  });

  it("edit anonymous client registration policy", () => {
    listingPage.goToItemDetails("new policy");
    clientRegistrationPage
      .fillPolicyForm({
        name: "policy 2",
      })
      .formUtils()
      .save();

    masthead.checkNotificationMessage("Client policy updated successfully");
    clientRegistrationPage.formUtils().cancel();
    listingPage.itemExist("policy 2");
  });

  it("delete anonymous client registration policy", () => {
    listingPage.clickRowDetails("policy 2").clickDetailMenu("Delete");
    clientRegistrationPage.modalUtils().confirmModal();

    masthead.checkNotificationMessage(
      "Client registration policy deleted successfully"
    );
    listingPage.itemExist("policy 2", false);
  });
});
