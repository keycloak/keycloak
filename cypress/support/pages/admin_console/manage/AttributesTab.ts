export default class AttributesTab {
  private keyInput = '[name="attributes[0].key"]';
  private valueInput = '[name="attributes[0].value"]';
  private saveAttributeBtn = "save-attributes";
  private attributesTab = "attributes";

  goToAttributesTab() {
    cy.findByTestId(this.attributesTab).click();

    return this;
  }

  fillAttribute(key: string, value: string) {
    cy.get(this.keyInput).type(key).get(this.valueInput).type(value);
    return this;
  }

  saveAttribute() {
    cy.findByTestId(this.saveAttributeBtn).click();
    return this;
  }
}
