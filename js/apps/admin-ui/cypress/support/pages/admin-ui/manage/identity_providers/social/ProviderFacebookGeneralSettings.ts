import ProviderBaseGeneralSettingsPage from "../ProviderBaseGeneralSettingsPage";

const additionalUsersProfile_input_test_value =
  "additionalUsersProfile_input_test_value";

export default class ProviderFacebookGeneralSettings extends ProviderBaseGeneralSettingsPage {
  private additionalUsersProfileFieldsInput = "#facebookFetchedFields";

  public typeAdditionalUsersProfileFieldsInput(value: string) {
    cy.get(this.additionalUsersProfileFieldsInput).type(value).blur();
    return this;
  }

  public assertAdditionalUsersProfileFieldsInputEqual(value: string) {
    cy.get(this.additionalUsersProfileFieldsInput).should("have.value", value);
    return this;
  }

  public fillData(idpName: string) {
    this.fillCommonFields(idpName);
    this.typeAdditionalUsersProfileFieldsInput(
      idpName + additionalUsersProfile_input_test_value
    );
    return this;
  }

  public assertFilledDataEqual(idpName: string) {
    this.assertCommonFilledDataEqual(idpName);
    this.assertAdditionalUsersProfileFieldsInputEqual(
      idpName + additionalUsersProfile_input_test_value
    );
    return this;
  }
}
