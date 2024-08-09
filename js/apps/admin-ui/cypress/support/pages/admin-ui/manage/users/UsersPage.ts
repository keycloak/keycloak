import PageObject from "../../components/PageObject";
import UsersListingPage from "./UsersListingPage";

export default class UsersPage extends PageObject {
  #userListTabLink = "listTab";
  #permissionsTabLink = "permissionsTab";
  #userAttributeSearchChipsGroup =
    "[data-testid='user-attribute-search-chips-group']";
  #usersListingPage = new UsersListingPage(this);

  public goToUserListTab() {
    cy.findByTestId(this.#userListTabLink).click();

    return this;
  }

  public listing() {
    return this.#usersListingPage;
  }

  public goToPermissionsTab() {
    cy.findByTestId(this.#permissionsTabLink).click();

    return this;
  }

  public goToUserDetailsPage(username: string) {
    this.#usersListingPage.searchItem(username);
    this.#usersListingPage.goToItemDetails(username);

    return this;
  }

  public assertAttributeSearchChipExists(
    attributeLabel: string,
    attributeValue: string,
    exists: boolean,
  ) {
    super.assertLabeledChipGroupItemExist(
      this.#userAttributeSearchChipsGroup,
      attributeLabel,
      attributeValue,
      exists,
    );

    return this;
  }
}
