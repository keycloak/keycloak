import type { KeyValueType } from "../../../../../src/components/key-value-form/key-value-convert";

export default class KeyValueInput {
  #name: string;

  constructor(name: string) {
    this.#name = name;
  }

  fillKeyValue({ key, value }: KeyValueType) {
    cy.findByTestId(`${this.#name}-add-row`).click();

    cy.findAllByTestId(`${this.#name}-key`)
      .its("length")
      .then((length) => {
        this.keyInputAt(length - 1).type(key);
        this.valueInputAt(length - 1).type(value);
      });

    return this;
  }

  deleteRow(index: number) {
    cy.findAllByTestId(`${this.#name}-remove`).eq(index).click();
    return this;
  }

  validateRows(numberOfRows: number) {
    cy.findAllByTestId(`${this.#name}-key`).should("have.length", numberOfRows);
    return this;
  }

  save() {
    cy.findByTestId("save-attributes").click();
    return this;
  }

  keyInputAt(index: number) {
    return cy.findAllByTestId(`${this.#name}-key`).eq(index);
  }

  valueInputAt(index: number) {
    return cy.findAllByTestId(`${this.#name}-value`).eq(index);
  }
}
