import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateClientPage from "../support/pages/admin_console/manage/clients/CreateClientPage";
import ModalUtils from "../support/util/ModalUtils";
import AdvancedTab from "../support/pages/admin_console/manage/clients/AdvancedTab";
import AdminClient from "../support/util/AdminClient";
import InitialAccessTokenTab from "../support/pages/admin_console/manage/clients/InitialAccessTokenTab";
import { keycloakBefore } from "../support/util/keycloak_before";

let itemId = "client_crud";
const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const listingPage = new ListingPage();
const createClientPage = new CreateClientPage();
const modalUtils = new ModalUtils();

describe("Clients test", function () {
  describe("Client creation", function () {
    beforeEach(function () {
      keycloakBefore();
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

      listingPage.searchItem(itemId).itemExist(itemId);

      // Delete
      listingPage.deleteItem(itemId);
      modalUtils.checkModalTitle(`Delete ${itemId} ?`).confirmModal();

      masthead.checkNotificationMessage("The client has been deleted");

      listingPage.itemExist(itemId, false);
    });

    it("Initial access token", () => {
      const initialAccessTokenTab = new InitialAccessTokenTab();
      listingPage.goToInitialAccessTokenTab();
      initialAccessTokenTab.shouldBeEmpty();
      initialAccessTokenTab.createNewToken(1, 1).save();

      modalUtils.checkModalTitle("Initial access token details").closeModal();

      initialAccessTokenTab.shouldNotBeEmpty();

      initialAccessTokenTab.getFistId((id) => {
        listingPage.deleteItem(id);
        modalUtils
          .checkModalTitle("Delete initial access token?")
          .confirmModal();
        masthead.checkNotificationMessage(
          "initial access token created successfully"
        );
      });
    });
  });

  describe("Advanced tab test", () => {
    const advancedTab = new AdvancedTab();
    let client: string;

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToClients();

      client = "client_" + (Math.random() + 1).toString(36).substring(7);

      listingPage.goToCreateItem();

      createClientPage
        .selectClientType("openid-connect")
        .fillClientData(client)
        .continue()
        .continue();

      advancedTab.goToTab();
    });

    afterEach(() => {
      new AdminClient().deleteClient(client);
    });

    it("Revocation", () => {
      advancedTab.checkNone();

      advancedTab.clickSetToNow().checkSetToNow();
      advancedTab.clickClear().checkNone();

      advancedTab.clickPush();
      masthead.checkNotificationMessage(
        "No push sent. No admin URI configured or no registered cluster nodes available"
      );
    });

    it("Clustering", () => {
      advancedTab.expandClusterNode().checkTestClusterAvailability(false);

      advancedTab
        .clickRegisterNodeManually()
        .fillHost("localhost")
        .clickSaveHost();
      advancedTab.checkTestClusterAvailability(true);
    });

    it("Fine grain OpenID connect configuration", () => {
      const algorithm = "ES384";
      advancedTab
        .selectAccessTokenSignatureAlgorithm(algorithm)
        .clickSaveFineGrain();

      advancedTab
        .selectAccessTokenSignatureAlgorithm("HS384")
        .clickRevertFineGrain();
      advancedTab.checkAccessTokenSignatureAlgorithm(algorithm);
    });
  });
});
