export default class AttributesTab {
  private saveAttributeBtn = "save-attributes";
  private addAttributeBtn = "attributes-add-row";
  private attributesTab = "attributes";
  private attributeRow = "row";
  private keyInput = (index: number) => `attributes[${index}].key`;
  private valueInput = (index: number) => `attributes[${index}].value`;

  public goToAttributesTab() {
    cy.findByTestId(this.attributesTab).click();

    return this;
  }

  public addAttribute(key: string, value: string) {
    cy.findAllByTestId(this.attributeRow)
      .its("length")
      .then((index) => {
        cy.findByTestId(this.keyInput(index - 1)).type(key, { force: true });
        cy.findByTestId(this.valueInput(index - 1)).type(value, {
          force: true,
        });
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
    cy.findByTestId(`attributes[${row - 1}].remove`).click({ force: true });
    return this;
  }

  public addAnAttributeButton() {
    cy.findByTestId(this.addAttributeBtn).click();
    return this;
  }

  public deleteAttribute(rowIndex: number) {
    this.deleteAttributeButton(rowIndex);
    this.save();

    cy.findAllByTestId(`attributes[${rowIndex - 1}].key`).should(
      "have.value",
      ""
    );
    cy.findAllByTestId(`attributes[${rowIndex - 1}].value`).should(
      "have.value",
      ""
    );
    return this;
  }

  public asseertRowItemsEqualTo(amount: number) {
    cy.findAllByTestId("row").its("length").should("be.eq", amount);
    return this;
  }
}
