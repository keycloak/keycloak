export default class AttributesTab {
  private saveAttributeBtn = "save-attributes";
  private addAttributeBtn = "attribute-add-row";
  private attributesTab = "attributes";
  private attributeRow = "[data-testid=row]";
  private keyInput = (index: number) => `attributes[${index}].key`;
  private valueInput = (index: number) => `attributes[${index}].value`;

  goToAttributesTab() {
    cy.findByTestId(this.attributesTab).click();

    return this;
  }

  addRow() {
    cy.findByTestId(this.addAttributeBtn).click();
    return this;
  }

  fillLastRow(key: string, value: string) {
    cy.get(this.attributeRow)
      .its("length")
      .then((index) => {
        cy.findByTestId(this.keyInput(index - 1)).type(key);
        cy.findByTestId(this.valueInput(index - 1)).type(value);
      });
    return this;
  }

  saveAttribute() {
    cy.findByTestId(this.saveAttributeBtn).click();
    return this;
  }
}
