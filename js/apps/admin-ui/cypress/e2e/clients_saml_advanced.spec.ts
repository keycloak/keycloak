import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import { AdvancedSamlTab } from "../support/pages/admin-ui/manage/clients/client_details/tabs/AdvancedSamlTab";
import ClientDetailsPage from "../support/pages/admin-ui/manage/clients/client_details/ClientDetailsPage";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const listingPage = new ListingPage();
const advancedTab = new AdvancedSamlTab();

describe("Clients Saml advanced tab", () => {
  describe("Fine Grain SAML Endpoint Configuration", () => {
    const clientName = "advanced-tab";

    before(() => {
      adminClient.createClient({
        protocol: "saml",
        clientId: clientName,
        publicClient: false,
      });
    });

    after(() => {
      adminClient.deleteClient(clientName);
    });

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToClients();
      listingPage.searchItem(clientName).goToItemDetails(clientName);
      new ClientDetailsPage().goToAdvancedTab();
    });

    it("Should Terms of service URL", () => {
      const termsOfServiceUrl = "http://some.url/terms-of-service.html";
      advancedTab.termsOfServiceUrl(termsOfServiceUrl).saveFineGrain();
      masthead.checkNotificationMessage("Client successfully updated");

      advancedTab
        .termsOfServiceUrl("http://not.saveing.this/")
        .revertFineGrain();
      advancedTab.checkTermsOfServiceUrl(termsOfServiceUrl);
    });

    it("Invalid terms of service URL", () => {
      advancedTab.termsOfServiceUrl("not a url").saveFineGrain();

      masthead.checkNotificationMessage(
        "Client could not be updated: Terms of service URL is not a valid URL",
      );
    });
  });
});
