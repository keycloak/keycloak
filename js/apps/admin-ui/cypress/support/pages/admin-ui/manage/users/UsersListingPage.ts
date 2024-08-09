import ListingPage from "../../ListingPage";
import UsersPage from "./UsersPage";

export enum DefaultUserAttribute {
  username = "Username",
  email = "Email",
  firstName = "First name",
  lastName = "Last name",
}

export enum UserFilterType {
  DefaultSearch = "Default search",
  AttributeSearch = "Attribute search",
}

export default class UsersListingPage extends ListingPage {
  #dropdownPanelBtn = "[data-testid='dropdown-panel-btn']";
  #userAttributeSearchForm = "[data-testid='user-attribute-search-form']";
  #userAttributeSearchAddFilterBtn =
    "[data-testid='user-attribute-search-add-filter-button']";
  #userAttributeSearchBtn = "[data-testid='search-user-attribute-btn']";
  #usersPage: UsersPage;

  constructor(usersPage: UsersPage) {
    super();
    this.#usersPage = usersPage;
    console.log("this.u1", usersPage);
    console.log("this.u2", this.#usersPage);
  }

  selectUserSearchFilter(filter: UserFilterType) {
    super.selectFilter("user-search-toggle", filter);

    return this;
  }

  openUserAttributesSearchForm() {
    cy.get(this.#dropdownPanelBtn).click();
    cy.get(this.#userAttributeSearchForm).should("be.visible");

    return this;
  }

  addUserAttributeSearchCriteria(
    defaultUserAttribute: DefaultUserAttribute,
    attributeValue: string,
  ) {
    return this.addUserAttributeSearchCriteriaCustom(
      defaultUserAttribute,
      attributeValue,
    );
  }

  addUserAttributeSearchCriteriaCustom(
    attributeLabel: string,
    attributeValue: string,
  ) {
    cy.get(this.#userAttributeSearchForm)
      .find(".pf-m-typeahead")
      .click()
      .get(".pf-v5-c-menu__list-item")
      .contains(attributeLabel)
      .click({ force: true });

    cy.get(this.#userAttributeSearchForm)
      .find("#value")
      .clear()
      .type(attributeValue);

    cy.get(this.#userAttributeSearchForm)
      .find(this.#userAttributeSearchAddFilterBtn)
      .click();

    this.#usersPage.assertAttributeSearchChipExists(
      attributeLabel,
      attributeValue,
      true,
    );

    return this;
  }

  triggerAttributesSearch() {
    cy.get(this.#userAttributeSearchBtn).click();

    return this;
  }
}
