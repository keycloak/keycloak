export default class AttributesTab {
  #saveAttributeBtn = "save-attributes";
  #addAttributeBtn = "attributes-add-row";
  #attributesTab = "attributes";
  #keyInput = "attributes-key";
  #valueInput = "attributes-value";
  #removeBtn = "attributes-remove";
  #emptyState = "attributes-empty-state";

  public goToAttributesTab() {
    cy.findByTestId(this.#attributesTab).click();

    return this;
  }

  public addAttribute(key: string, value: string) {
    this.addAnAttributeButton();

    cy.findAllByTestId(this.#keyInput)
      .its("length")
      .then((length) => {
        this.#keyInputAt(length - 1).type(key, { force: true });
        this.#valueInputAt(length - 1).type(value, { force: true });
      });

    return this;
  }

  public save() {
    cy.findByTestId(this.#saveAttributeBtn).click();
    return this;
  }

  public revert() {
    cy.get(".pf-c-button.pf-m-link").contains("Revert").click();
    return this;
  }

  public deleteAttributeButton(row: number) {
    this.#removeButtonAt(row).click({ force: true });
    return this;
  }

  public addAnAttributeButton() {
    cy.wait(1000);
    cy.findByTestId(this.#addAttributeBtn).click();
    return this;
  }

  public deleteAttribute(rowIndex: number) {
    this.deleteAttributeButton(rowIndex);
    this.save();

    return this;
  }

  public assertEmpty() {
    cy.findByTestId(this.#emptyState).should("exist");
  }

  public assertRowItemsEqualTo(amount: number) {
    cy.findAllByTestId(this.#keyInput).its("length").should("be.eq", amount);
    return this;
  }

  #keyInputAt(index: number) {
    return cy.findAllByTestId(this.#keyInput).eq(index);
  }

  #valueInputAt(index: number) {
    return cy.findAllByTestId(this.#valueInput).eq(index);
  }

  #removeButtonAt(index: number) {
    return cy.findAllByTestId(this.#removeBtn).eq(index);
  }
}
