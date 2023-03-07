import ProviderBaseGeneralSettingsPage from "../ProviderBaseGeneralSettingsPage";

const key_input_test_value = "key_input_test_value";

export default class ProviderStackoverflowGeneralSettings extends ProviderBaseGeneralSettingsPage {
  private keyInput = "#stackoverflowKey";

  constructor() {
    super();
    this.requiredFields.push(this.keyInput);
  }

  public typeKeyInput(value: string) {
    cy.get(this.keyInput).type(value).blur();
    return this;
  }

  public assertKeyInputEqual(value: string) {
    cy.get(this.keyInput).should("have.value", value);
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
