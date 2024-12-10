import InitialAccessTokenTab from "../support/pages/admin-ui/manage/clients/tabs/InitialAccessTokenTab";
import CommonPage from "../support/pages/CommonPage";
import LoginPage from "../support/pages/LoginPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const commonPage = new CommonPage();
const loginPage = new LoginPage();

describe("Client initial access tokens", () => {
  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    commonPage.sidebar().goToClients();
  });

  it("Initial access token can't be created with 0 days and count", () => {
    const initialAccessTokenTab = new InitialAccessTokenTab();
    initialAccessTokenTab
      .goToInitialAccessTokenTab()
      .shouldBeEmpty()
      .goToCreateFromEmptyList()
      .fillNewTokenData(0, 0)
      .checkExpirationGreaterThanZeroError()
      .checkCountValue(1)
      .checkSaveButtonIsDisabled();
  });

  it("Initial access token", () => {
    const initialAccessTokenTab = new InitialAccessTokenTab();
    initialAccessTokenTab
      .goToInitialAccessTokenTab()
      .shouldBeEmpty()
      .goToCreateFromEmptyList()
      .fillNewTokenData(1, 3)
      .save();

    commonPage
      .modalUtils()
      .checkModalTitle("Initial access token details")
      .closeModal();

    commonPage
      .masthead()
      .checkNotificationMessage("New initial access token has been created");

    initialAccessTokenTab.shouldNotBeEmpty();

    commonPage.tableToolbarUtils().searchItem("John Doe", false);
    commonPage.emptyState().checkIfExists(true);
    commonPage.tableToolbarUtils().searchItem("", false);

    initialAccessTokenTab.getFirstId((id) => {
      commonPage
        .tableUtils()
        .checkRowItemValueByItemName(id, 4, "4")
        .checkRowItemValueByItemName(id, 5, "4")
        .checkRowItemExists(id);
    });

    commonPage.tableToolbarUtils().clickPrimaryButton("Create");
    initialAccessTokenTab.fillNewTokenData(1, 3).save();

    commonPage.modalUtils().closeModal();

    initialAccessTokenTab.getFirstId((id) => {
      commonPage.tableUtils().selectRowItemAction(id, "Delete");
      commonPage.sidebar().waitForPageLoad();
      commonPage
        .modalUtils()
        .checkModalTitle("Delete initial access token?")
        .confirmModal();
    });

    commonPage
      .masthead()
      .checkNotificationMessage("Initial access token deleted successfully");
    initialAccessTokenTab.shouldNotBeEmpty();

    initialAccessTokenTab.getFirstId((id) => {
      commonPage.tableUtils().selectRowItemAction(id, "Delete");
      commonPage.sidebar().waitForPageLoad();
      commonPage.modalUtils().confirmModal();
    });
    initialAccessTokenTab.shouldBeEmpty();
  });
});
