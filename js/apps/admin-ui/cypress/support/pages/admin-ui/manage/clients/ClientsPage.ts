import CommonPage from "../../../CommonPage";
import ClientsListTab from "./tabs/ClientsListTab";
import InitialAccessTokenTab from "./tabs/InitialAccessTokenTab";

enum ClientsTab {
  ClientsList = "Clients list",
  InitialAccessToken = "Initial access token",
}

export default class ClientsPage extends CommonPage {
  private clientsListTab = new ClientsListTab();
  private initialAccessTokenTab = new InitialAccessTokenTab();

  goToClientsListTab() {
    this.tabUtils().clickTab(ClientsTab.ClientsList);
    return this.clientsListTab;
  }

  goToInitialAccessTokenTab() {
    this.tabUtils().clickTab(ClientsTab.InitialAccessToken);
    return this.initialAccessTokenTab;
  }
}
