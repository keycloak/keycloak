import LoginPage from "../support/pages/LoginPage.js";
import Masthead from "../support/pages/admin_console/Masthead.js";
import ListingPage from "../support/pages/admin_console/ListingPage.js";
import SidebarPage from "../support/pages/admin_console/SidebarPage.js";
import CreateClientPage from "../support/pages/admin_console/manage/clients/CreateClientPage.js";

describe("Clients test", function () {
  let itemId = "client_crud";
  const loginPage = new LoginPage();
  const masthead = new Masthead();
  const sidebarPage = new SidebarPage();
  const listingPage = new ListingPage();
  const createClientPage = new CreateClientPage();

  describe("Client creation", function () {
    beforeEach(function () {
      cy.clearCookies();
      cy.visit("");
      loginPage.logIn();
      sidebarPage.goToClients();
    });

    it("should fail creating client", function () {
      listingPage.goToCreateItem();

      createClientPage
        .continue()
        .checkClientTypeRequiredMessage()
        .checkClientIdRequiredMessage();

      createClientPage
        .fillClientData(itemId)
        .continue()
        .checkClientTypeRequiredMessage()
        .checkClientIdRequiredMessage(false);

      createClientPage
        .fillClientData("")
        .selectClientType("openid-connect")
        .continue()
        .checkClientTypeRequiredMessage(false)
        .checkClientIdRequiredMessage();

      createClientPage.fillClientData("account").continue().continue();

      // The error should inform about duplicated name/id
      masthead.checkNotificationMessage(
        "Could not create client: 'Error: Request failed with status code 409'"
      );
    });

    it("Client CRUD test", function () {
      itemId += "_" + (Math.random() + 1).toString(36).substring(7);
      
      // Create
      listingPage.itemExist(itemId, false).goToCreateItem();

      createClientPage
        .selectClientType("openid-connect")
        .fillClientData(itemId)
        .continue()
        .continue();

      masthead.checkNotificationMessage("Client created successfully");

      sidebarPage.goToClients();

      listingPage
        .searchItem(itemId)
        .itemExist(itemId);

      // Delete
      listingPage
        .deleteItem(itemId); // There should be a confirmation pop-up

      masthead.checkNotificationMessage("The client has been deleted");

      listingPage.itemExist(itemId, false);
    });
  });
});
