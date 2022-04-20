import type { KeyValueType } from "../../../../../src/components/attribute-form/attribute-convert";

export default class KeyValueInput {
  private name: string;

  constructor(name: string) {
    this.name = name;
  }

  private getRow(row: number) {
    return `table tr:nth-child(${row + 1})`;
  }

  fillKeyValue({ key, value }: KeyValueType, row: number | undefined = 0) {
    cy.get(`${this.getRow(row)} [data-testid=${this.name}-key-input]`)
      .clear()
      .type(key);
    cy.get(`${this.getRow(row)} [data-testid=${this.name}-value-input]`)
      .clear()
      .type(value);
    cy.findByTestId(`${this.name}-add-row`).click();
    return this;
  }

  deleteRow(row: number) {
    cy.get(`${this.getRow(row)} button`).click();
    return this;
  }

  validateRows(num: number) {
    cy.get(".kc-attributes__table tbody")
      .children()
      .should("have.length", num + 1);
    return this;
  }

  save() {
    cy.findByTestId("save-attributes").click();
    return this;
  }
}
