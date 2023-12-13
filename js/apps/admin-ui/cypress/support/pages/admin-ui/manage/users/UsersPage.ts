import PageObject from "../../components/PageObject";
import ListingPage from "../../ListingPage";

const listingPage = new ListingPage();

export default class UsersPage extends PageObject {
  #userListTabLink = "listTab";
  #permissionsTabLink = "permissionsTab";

  public goToUserListTab() {
    cy.findByTestId(this.#userListTabLink).click();

    return this;
  }

  public goToPermissionsTab() {
    cy.findByTestId(this.#permissionsTabLink).click();

    return this;
  }

  public goToUserDetailsPage(username: string) {
    listingPage.searchItem(username);
    listingPage.goToItemDetails(username);

    return this;
  }
}
