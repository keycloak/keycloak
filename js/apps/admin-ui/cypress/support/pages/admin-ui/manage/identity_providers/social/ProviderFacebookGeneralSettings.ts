import ProviderBaseGeneralSettingsPage from "../ProviderBaseGeneralSettingsPage";

const additionalUsersProfile_input_test_value =
  "additionalUsersProfile_input_test_value";

export default class ProviderFacebookGeneralSettings extends ProviderBaseGeneralSettingsPage {
  #additionalUsersProfileFieldsInput = "fetchedFields";

  public typeAdditionalUsersProfileFieldsInput(value: string) {
    cy.findByTestId(this.#additionalUsersProfileFieldsInput).type(value);
    cy.findByTestId(this.#additionalUsersProfileFieldsInput).blur();
    return this;
  }

  public assertAdditionalUsersProfileFieldsInputEqual(value: string) {
    cy.findByTestId(this.#additionalUsersProfileFieldsInput).should(
      "have.value",
      value,
    );
    return this;
  }

  public fillData(idpName: string) {
    this.fillCommonFields(idpName);
    this.typeAdditionalUsersProfileFieldsInput(
      idpName + additionalUsersProfile_input_test_value,
    );
    return this;
  }

  public assertFilledDataEqual(idpName: string) {
    this.assertCommonFilledDataEqual(idpName);
    this.assertAdditionalUsersProfileFieldsInputEqual(
      idpName + additionalUsersProfile_input_test_value,
    );
    return this;
  }
}
