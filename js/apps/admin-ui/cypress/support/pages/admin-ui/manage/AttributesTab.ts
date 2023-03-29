export default class AttributesTab {
  private saveAttributeBtn = "save-attributes";
  private addAttributeBtn = "attributes-add-row";
  private attributesTab = "attributes";
  private keyInput = "attributes-key";
  private valueInput = "attributes-value";
  private removeBtn = "attributes-remove";
  private emptyState = "attributes-empty-state";

  public goToAttributesTab() {
    cy.findByTestId(this.attributesTab).click();

    return this;
  }

  public addAttribute(key: string, value: string) {
    cy.findByTestId(this.addAttributeBtn).click();

    cy.findAllByTestId(this.keyInput)
      .its("length")
      .then((length) => {
        this.keyInputAt(length - 1).type(key, { force: true });
        this.valueInputAt(length - 1).type(value, { force: true });
      });

    return this;
  }

  public save() {
    cy.findByTestId(this.saveAttributeBtn).click();
    return this;
  }

  public revert() {
    cy.get(".pf-c-button.pf-m-link").contains("Revert").click();
    return this;
  }

  public deleteAttributeButton(row: number) {
    this.removeButtonAt(row).click({ force: true });
    return this;
  }

  public addAnAttributeButton() {
    cy.findByTestId(this.addAttributeBtn).click();
    return this;
  }

  public deleteAttribute(rowIndex: number) {
    this.deleteAttributeButton(rowIndex);
    this.save();

    return this;
  }

  public assertEmpty() {
    cy.findByTestId(this.emptyState).should("exist");
  }

  public assertRowItemsEqualTo(amount: number) {
    cy.findAllByTestId(this.keyInput).its("length").should("be.eq", amount);
    return this;
  }

  private keyInputAt(index: number) {
    return cy.findAllByTestId(this.keyInput).eq(index);
  }

  private valueInputAt(index: number) {
    return cy.findAllByTestId(this.valueInput).eq(index);
  }

  private removeButtonAt(index: number) {
    return cy.findAllByTestId(this.removeBtn).eq(index);
  }
}
