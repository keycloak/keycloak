import ProviderBaseGeneralSettingsPage from "../ProviderBaseGeneralSettingsPage";

const base_url_input_test_value = "base_url_input_test_value";
const api_url_input_test_value = "api_url_input_test_value";

export default class ProviderGithubGeneralSettings extends ProviderBaseGeneralSettingsPage {
  private baseUrlInput = "#baseUrl";
  private apiUrlInput = "#apiUrl";

  public typeBaseUrlInput(value: string) {
    cy.get(this.baseUrlInput).type(value).blur();
    return this;
  }

  public typeApiUrlInput(value: string) {
    cy.get(this.apiUrlInput).type(value).blur();
    return this;
  }

  public assertBaseUrlInputInputEqual(value: string) {
    cy.get(this.baseUrlInput).should("have.value", value);
    return this;
  }

  public assertApiUrlInputEqual(value: string) {
    cy.get(this.apiUrlInput).should("have.value", value);
    return this;
  }

  public fillData(idpName: string) {
    this.fillCommonFields(idpName);
    this.typeBaseUrlInput(idpName + base_url_input_test_value);
    this.typeApiUrlInput(idpName + api_url_input_test_value);
    return this;
  }

  public assertFilledDataEqual(idpName: string) {
    this.assertCommonFilledDataEqual(idpName);
    this.assertBaseUrlInputInputEqual(idpName + base_url_input_test_value);
    this.assertApiUrlInputEqual(idpName + api_url_input_test_value);
    return this;
  }
}
