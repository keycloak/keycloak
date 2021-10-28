export default class AttributesTab {
  private saveAttributeBtn = "save-attributes";
  private addAttributeBtn = "attribute-add-row";
  private attributesTab = "attributes";
  private attributeRow = "attribute-row";
  private keyInput = "attribute-key-input";
  private valueInput = "attribute-value-input";

  goToAttributesTab() {
    cy.findByTestId(this.attributesTab).click();

    return this;
  }

  addRow() {
    cy.findByTestId(this.addAttributeBtn).click();
    return this;
  }

  fillLastRow(key: string, value: string) {
    cy.findAllByTestId(this.attributeRow)
      .last()
      .findByTestId(this.keyInput)
      .type(key);
    cy.findAllByTestId(this.attributeRow)
      .last()
      .findByTestId(this.valueInput)
      .type(value);
    return this;
  }

  saveAttribute() {
    cy.findByTestId(this.saveAttributeBtn).click();
    return this;
  }
}
