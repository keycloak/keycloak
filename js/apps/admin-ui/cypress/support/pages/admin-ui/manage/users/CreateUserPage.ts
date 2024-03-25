import Masthead from "../../Masthead";
import FormValidation from "../../../../forms/FormValidation";

export default class CreateUserPage {
  readonly masthead = new Masthead();
  readonly emptyOptionValue = "Choose...";

  usersEmptyState: string;
  emptyStateCreateUserBtn: string;
  addUserBtn: string;
  joinGroupsBtn: string;
  joinBtn: string;
  createBtn: string;
  saveBtn: string;
  cancelBtn: string;

  constructor() {
    this.usersEmptyState = "empty-state";
    this.emptyStateCreateUserBtn = "no-users-found-empty-action";
    this.addUserBtn = "add-user";
    this.joinGroupsBtn = "join-groups-button";
    this.joinBtn = "join-button";
    this.createBtn = "create-user";
    this.saveBtn = "save-user";
    this.cancelBtn = "cancel-create-user";
  }

  //#region General Settings
  createUser(username: string) {
    return this.setUsername(username);
  }

  goToCreateUser() {
    cy.get("body").then((body) => {
      if (body.find(`[data-testid=${this.addUserBtn}]`).length > 0) {
        cy.findByTestId(this.addUserBtn).click({ force: true });
      } else {
        cy.findByTestId(this.emptyStateCreateUserBtn).click({ force: true });
      }
    });

    return this;
  }

  toggleAddGroupModal() {
    cy.findByTestId(this.joinGroupsBtn).click();

    return this;
  }

  joinGroups() {
    cy.findByTestId(this.joinBtn).click();

    return this;
  }

  create() {
    cy.findByTestId(this.createBtn).click();

    return this;
  }

  update() {
    cy.findByTestId(this.saveBtn).click();

    return this;
  }

  assertAttributeValue(attrName: string, expectedValue: string) {
    cy.findByTestId(attrName).should("have.value", expectedValue);

    return this;
  }

  assertAttributeFieldExists(attrName: string, shouldExist: boolean) {
    const chainer = shouldExist ? "exist" : "not.exist";
    cy.findByTestId(attrName).should(chainer);

    return this;
  }

  assertAttributeSelect(
    attrName: string,
    expectedOptionsWithoutEmptyOption: string[],
    expectedValue: string,
  ) {
    this.#getSelectFieldButton(attrName).should(
      "have.class",
      "pf-v5-c-select__toggle",
    );

    const valueToCheck = expectedValue ? expectedValue : this.emptyOptionValue;
    this.#getSelectFieldButton(attrName)
      .find(".pf-v5-c-select__toggle-text")
      .invoke("text")
      .should("eq", valueToCheck);

    const expectedOptions = [this.emptyOptionValue].concat(
      expectedOptionsWithoutEmptyOption,
    );
    this.#withSelectExpanded(attrName, () => {
      this.#getSelectOptions(attrName)
        .should("have.length", expectedOptions.length)
        .each(($option, index) =>
          cy.wrap($option).should("have.text", expectedOptions[index]),
        );
    });

    return this;
  }

  #getSelectFieldButton(attrName: string) {
    const attrSelector = `#${attrName}`;
    return cy.get(attrSelector);
  }

  #getSelectOptions(attrName: string) {
    return this.#getSelectFieldButton(attrName)
      .parent()
      .find(".pf-v5-c-select__menu-item");
  }

  #toggleSelectField(
    attrName: string,
    toggleCondition?: (currentlyExpanded: boolean) => boolean,
  ) {
    return this.#getSelectFieldButton(attrName).then(($selectField) => {
      const currentlyExpanded = $selectField.attr("aria-expanded") === "true";
      if (!toggleCondition || toggleCondition(currentlyExpanded)) {
        cy.wrap($selectField).click();
      }
    });
  }

  /**
   * Runs the given function in the context of an expanded select field.
   *
   * This method makes sure that the initial expanded/collapsed state is preserved:
   * The select field will be expanded before running the function, if it is not already expanded.
   * It will be collapsed after running the function, when it was not expanded before running the function.
   *
   * @param attrName the attribute name of the select field
   * @param func the function to be applied
   * @private
   */
  #withSelectExpanded(attrName: string, func: () => any) {
    let wasInitiallyExpanded = false;
    this.#toggleSelectField(attrName, (currentlyExpanded) => {
      wasInitiallyExpanded = currentlyExpanded;
      return !currentlyExpanded;
    })
      .then(() => func())
      .then(() =>
        // click again on the dropdown to hide the values list, when necessary
        this.#toggleSelectField(
          attrName,
          (currentlyExpanded) => currentlyExpanded && !wasInitiallyExpanded,
        ),
      );
  }

  assertAttributeLabel(attrName: string, expectedText: string) {
    cy.get(`.pf-v5-c-form__label[for='${attrName}'] .pf-v5-c-form__label-text`)
      .contains(expectedText)
      .should("exist");

    return this;
  }

  assertValidationErrorRequired(attrName: string) {
    FormValidation.assertMessage(
      cy.findByTestId(attrName),
      `Please specify '${attrName}'.`,
    );

    return this;
  }

  assertGroupDisplayName(group: string, expectedDisplayName: string) {
    cy.get(`h1#${group.toLowerCase()}`).should(
      "have.text",
      expectedDisplayName,
    );

    return this;
  }

  setAttributeValue(attrName: string, value: string) {
    cy.findByTestId(attrName).as("attr").clear();
    cy.get("@attr").type(value);

    return this;
  }

  setAttributeValueOnSelect(attrName: string, value: string) {
    this.#withSelectExpanded(attrName, () => {
      this.#getSelectOptions(attrName).contains(value).click();
    });

    return this;
  }

  setUsername(value: string) {
    this.#getUsernameField().clear();

    if (value) {
      this.#getUsernameField().type(value);
    }

    return this;
  }

  #getUsernameField() {
    return cy.findByTestId("username");
  }

  assertNotificationCreated() {
    this.masthead.checkNotificationMessage("The user has been created");

    return this;
  }

  assertNotificationUpdated() {
    this.masthead.checkNotificationMessage("The user has been saved");

    return this;
  }

  cancel() {
    cy.findByTestId(this.cancelBtn).click();

    return this;
  }
}
