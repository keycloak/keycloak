import type { KeyValueType } from "../../../../../src/components/key-value-form/key-value-convert";

export default class LegacyKeyValueInput {
  #name: string;

  constructor(name: string) {
    this.#name = name;
  }

  fillKeyValue({ key, value }: KeyValueType, index = 0) {
    cy.findByTestId(`${this.#name}.${index}.key`).clear();
    cy.findByTestId(`${this.#name}.${index}.key`).type(key);
    cy.findByTestId(`${this.#name}.${index}.value`).clear();
    cy.findByTestId(`${this.#name}.${index}.value`).type(value);
    cy.findByTestId(`${this.#name}-add-row`).click();
    return this;
  }

  deleteRow(index: number) {
    cy.findByTestId(`${this.#name}.${index}.remove`).click();
    return this;
  }

  validateRows(numberOfRows: number) {
    cy.findAllByTestId("row").should("have.length", numberOfRows);
    return this;
  }

  save() {
    cy.findByTestId("save-attributes").click();
    return this;
  }
}
