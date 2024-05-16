import ProviderBaseGeneralSettingsPage from "../ProviderBaseGeneralSettingsPage";

const key_input_test_value = "key_input_test_value";

export default class ProviderStackoverflowGeneralSettings extends ProviderBaseGeneralSettingsPage {
  #keyInput = "key";

  constructor() {
    super();
  }

  public typeKeyInput(value: string) {
    cy.findByTestId(this.#keyInput).type(value);
    cy.findByTestId(this.#keyInput).blur();
    return this;
  }

  public assertKeyInputEqual(value: string) {
    cy.findByTestId(this.#keyInput).should("have.value", value);
    return this;
  }

  public assertRequiredFieldsErrorsExist() {
    return this.assertCommonRequiredFields(this.requiredFields);
  }

  public fillData(idpName: string) {
    this.fillCommonFields(idpName);
    this.typeKeyInput(idpName + key_input_test_value);
    return this;
  }

  public assertFilledDataEqual(idpName: string) {
    this.assertCommonFilledDataEqual(idpName);
    this.assertKeyInputEqual(idpName + key_input_test_value);
    return this;
  }
}
