import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateClientScopePage from "../support/pages/admin_console/manage/client_scopes/CreateClientScopePage";
import { keycloakBefore } from "../support/util/keycloak_before";
import RoleMappingTab from "../support/pages/admin_console/manage/RoleMappingTab";
import ModalUtils from "../support/util/ModalUtils";
import AdminClient from "../support/util/AdminClient";

let itemId = "client_scope_crud";
const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const listingPage = new ListingPage();
const createClientScopePage = new CreateClientScopePage();
const modalUtils = new ModalUtils();

describe("Client Scopes test", () => {
  describe("Client Scope list items ", () => {
    const clientScopeName = "client-scope-test";
    const clientScope = {
      name: clientScopeName,
      description: "",
      protocol: "openid-connect",
      attributes: {
        "include.in.token.scope": "true",
        "display.on.consent.screen": "true",
        "gui.order": "1",
        "consent.screen.text": "",
      },
    };

    before(async () => {
      const client = new AdminClient();
      for (let i = 0; i < 5; i++) {
        clientScope.name = clientScopeName + i;
        await client.createClientScope(clientScope);
      }
    });

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClientScopes();
    });

    after(async () => {
      const client = new AdminClient();
      for (let i = 0; i < 5; i++) {
        await client.deleteClientScope(clientScopeName + i);
      }
    });

    it("should show items on next page are more than 11", () => {
      listingPage.showNextPageTableItems();

      cy.get(listingPage.tableRowItem).its("length").should("be.gt", 1);
    });
  });

  describe("Client Scope creation", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClientScopes();
    });

    it("should fail creating client scope", () => {
      listingPage.goToCreateItem();

      createClientScopePage.save().checkClientNameRequiredMessage();

      createClientScopePage
        .fillClientScopeData("address")
        .save()
        .checkClientNameRequiredMessage(false);

      // The error should inform about duplicated name/id
      masthead.checkNotificationMessage(
        "Could not create client scope: 'Client Scope address already exists'"
      );
    });

    it("Client scope CRUD test", () => {
      itemId += "_" + (Math.random() + 1).toString(36).substring(7);

      // Create
      listingPage.itemExist(itemId, false).goToCreateItem();

      createClientScopePage.fillClientScopeData(itemId).save();

      masthead.checkNotificationMessage("Client scope created");

      sidebarPage.goToClientScopes();
      sidebarPage.waitForPageLoad();

      // Delete
      listingPage
        .searchItem(itemId, false)
        .itemExist(itemId)
        .deleteItem(itemId);

      modalUtils
        .checkModalMessage("Are you sure you want to delete this client scope")
        .confirmModal();

      masthead.checkNotificationMessage("The client scope has been deleted");

      listingPage.itemExist(itemId, false);
    });
  });

  describe("Scope test", () => {
    const scopeTab = new RoleMappingTab();
    const scopeName = "address";

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClientScopes();
    });

    it("Assign role", () => {
      const role = "offline_access";
      listingPage.searchItem(scopeName, false).goToItemDetails(scopeName);
      scopeTab.goToScopeTab().clickAssignRole().selectRow(role).clickAssign();
      masthead.checkNotificationMessage("Role mapping updated");
      scopeTab.checkRoles([role]);
      scopeTab.hideInheritedRoles().selectRow(role).clickUnAssign();
      modalUtils.checkModalTitle("Remove mapping?").confirmModal();
      scopeTab.checkRoles([]);
    });
  });
});
