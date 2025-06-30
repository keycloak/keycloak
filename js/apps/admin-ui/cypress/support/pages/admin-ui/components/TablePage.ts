import CommonElements from "../../CommonElements";

export default class TablePage extends CommonElements {
  #tableRowItem: string;
  #tableRowItemChckBx: string;
  #tableHeaderRowItem: string;
  #tableKebabMenu: string;
  #tableInModal: boolean;
  static tableSelector = ".pf-v5-c-table";

  constructor(parentElement?: string) {
    super(parentElement ?? TablePage.tableSelector + ":visible");
    this.#tableRowItem = this.parentSelector + "tbody tr";
    this.#tableHeaderRowItem = this.parentSelector + "thead tr";
    this.#tableRowItemChckBx = ".pf-v5-c-table__check";
    this.#tableKebabMenu = ".pf-v5-c-menu";
    this.#tableInModal = false;
  }

  setTableInModal(value: boolean) {
    this.#tableInModal = value;
  }

  selectRowItemCheckbox(itemName: string) {
    cy.get(
      (this.#tableInModal ? ".pf-v5-c-modal-box.pf-m-md " : "") +
        this.#tableRowItem,
    )
      .contains(itemName)
      .parentsUntil("tbody")
      .find(this.#tableRowItemChckBx)
      .click();
    return this;
  }

  clickRowItemLink(itemName: string) {
    cy.get(
      (this.#tableInModal ? ".pf-v5-c-modal-box.pf-m-md " : "") +
        this.#tableRowItem,
    )
      .contains(itemName)
      .click({ force: true });
    return this;
  }

  selectRowItemAction(itemName: string, actionItemName: string) {
    this.#getRowItemAction(itemName, actionItemName).click();
    return this;
  }

  assertRowItemActionExist(itemName: string, actionItemName: string) {
    this.#getRowItemAction(itemName, actionItemName).should("exist");
    return this;
  }

  #getRowItemAction(itemName: string, actionItemName: string) {
    return cy
      .get(
        (this.#tableInModal ? ".pf-v5-c-modal-box.pf-m-md " : "") +
          this.#tableRowItem,
      )
      .contains(itemName)
      .parentsUntil("tbody")
      .find(".pf-v5-c-table__action .pf-v5-c-menu-toggle")
      .click()
      .get(this.#tableKebabMenu)
      .contains(actionItemName);
  }

  typeValueToRowItem(row: number, column: number, value: string) {
    cy.get(
      (this.#tableInModal ? ".pf-v5-c-modal-box.pf-m-md " : "") +
        this.#tableRowItem +
        ":nth-child(" +
        row +
        ")",
    )
      .find("td:nth-child(" + column + ")")
      .type(value);
    return this;
  }

  clickRowItemByIndex(row: number, column: number, appendChildren?: string) {
    cy.get(
      (this.#tableInModal ? ".pf-v5-c-modal-box.pf-m-md " : "") +
        this.#tableRowItem +
        ":nth-child(" +
        row +
        ")",
    )
      .find("td:nth-child(" + column + ") " + appendChildren)
      .click();
    return this;
  }

  clickRowItemByItemName(
    itemName: string,
    column: number,
    appendChildren?: string,
  ) {
    cy.get(
      (this.#tableInModal ? ".pf-v5-c-modal-box.pf-m-md " : "") +
        this.#tableRowItem,
    )
      .find("td:nth-child(" + column + ") " + appendChildren)
      .contains(itemName)
      .click();
    return this;
  }

  clickHeaderItem(column: number, appendChildren?: string) {
    cy.get(
      (this.#tableInModal ? ".pf-v5-c-modal-box.pf-m-md " : "") +
        this.#tableHeaderRowItem,
    )
      .find("th:nth-child(" + column + ") " + appendChildren)
      .click();
    return this;
  }

  checkRowItemsEqualTo(amount: number) {
    cy.get(
      (this.#tableInModal ? ".pf-v5-c-modal-box.pf-m-md " : "") +
        this.#tableRowItem,
    )
      .its("length")
      .should("be.eq", amount);
    return this;
  }

  checkRowItemsGreaterThan(amount: number) {
    cy.get(
      (this.#tableInModal ? ".pf-v5-c-modal-box.pf-m-md " : "") +
        this.#tableRowItem,
    )
      .its("length")
      .should("be.gt", amount);
    return this;
  }

  checkRowItemExists(itemName: string, exist = true) {
    cy.get(
      (this.#tableInModal ? ".pf-v5-c-modal-box.pf-m-md " : "") +
        this.#tableRowItem,
    )
      .contains(itemName)
      .should((!exist ? "not." : "") + "exist");
    return this;
  }

  checkRowItemValueByItemName(itemName: string, column: number, value: string) {
    cy.get(
      (this.#tableInModal ? ".pf-v5-c-modal-box.pf-m-md " : "") +
        this.#tableRowItem,
    )
      .contains(itemName)
      .parentsUntil("tbody")
      .find("td:nth-child(" + column + ")")
      .should("have.text", value);
    return this;
  }

  checkRowItemValueByIndex(
    row: number,
    column: number,
    value: string,
    appendChildren?: string,
  ) {
    cy.get(
      (this.#tableInModal ? ".pf-v5-c-modal-box.pf-m-md " : "") +
        this.#tableRowItem +
        ":nth-child(" +
        row +
        ")",
    )
      .find("td:nth-child(" + column + ") " + appendChildren)
      .should("have.text", value)
      .should("have.value", value);
    return this;
  }
}
